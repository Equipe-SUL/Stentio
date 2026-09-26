package com.example.stentio.core.service;

import com.example.stentio.core.dto.EmpresaPatchRequest;
import com.example.stentio.core.dto.EmpresaRequest;
import com.example.stentio.core.dto.EmpresaResponse;
import com.example.stentio.core.exception.RecursoInvalidoException;
import com.example.stentio.core.exception.RecursoNaoEncontradoException;
import com.example.stentio.core.model.ConfiguracaoEmpresa;
import com.example.stentio.core.repository.ConfiguracaoEmpresaRepository;
import com.example.stentio.core.validation.CnpjValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
public class ConfiguracaoEmpresaService {

    private static final long TAMANHO_MAXIMO_LOGO = 2 * 1024 * 1024;

    private static final Set<String> TIPOS_LOGO_PERMITIDOS = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp"
    );

    private final ConfiguracaoEmpresaRepository repository;

    public ConfiguracaoEmpresaService(
            ConfiguracaoEmpresaRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public EmpresaResponse buscar() {

        ConfiguracaoEmpresa empresa = buscarEmpresa();

        return new EmpresaResponse(empresa);
    }

    @Transactional
    public EmpresaResponse salvar(EmpresaRequest request) {

        validarCnpj(request.cnpj());

        ConfiguracaoEmpresa empresa = repository
                .findById(ConfiguracaoEmpresa.ID_UNICO)
                .orElseGet(() ->
                        new ConfiguracaoEmpresa(
                                request.nome(),
                                request.cnpj(),
                                request.endereco(),
                                request.telefone()
                        )
                );

        empresa.atualizarDados(
                request.nome(),
                request.cnpj(),
                request.endereco(),
                request.telefone()
        );

        ConfiguracaoEmpresa salva =
                repository.save(empresa);

        return new EmpresaResponse(salva);
    }

    @Transactional
    public EmpresaResponse atualizarParcialmente(
            EmpresaPatchRequest request
    ) {

        ConfiguracaoEmpresa empresa = buscarEmpresa();

        boolean possuiAlteracao = false;

        if (request.nome() != null) {

            if (request.nome().isBlank()) {
                throw new RecursoInvalidoException(
                        "Nome não pode ser vazio"
                );
            }

            empresa.atualizarNome(request.nome());
            possuiAlteracao = true;
        }

        if (request.cnpj() != null) {

            validarCnpj(request.cnpj());

            empresa.atualizarCnpj(request.cnpj());
            possuiAlteracao = true;
        }

        if (request.endereco() != null) {

            if (request.endereco().isBlank()) {
                throw new RecursoInvalidoException(
                        "Endereço não pode ser vazio"
                );
            }

            empresa.atualizarEndereco(request.endereco());
            possuiAlteracao = true;
        }

        if (request.telefone() != null) {
            empresa.atualizarTelefone(request.telefone());
            possuiAlteracao = true;
        }

        if (!possuiAlteracao) {
            throw new RecursoInvalidoException(
                    "Nenhum campo foi informado para atualização"
            );
        }

        ConfiguracaoEmpresa salva =
                repository.save(empresa);

        return new EmpresaResponse(salva);
    }

    @Transactional
    public EmpresaResponse salvarLogo(
            MultipartFile arquivo
    ) {

        validarLogo(arquivo);

        ConfiguracaoEmpresa empresa = buscarEmpresa();

        try {

            empresa.atualizarLogo(
                    arquivo.getBytes(),
                    arquivo.getContentType(),
                    arquivo.getOriginalFilename()
            );

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Não foi possível processar o arquivo da logo",
                    exception
            );
        }

        return new EmpresaResponse(
                repository.save(empresa)
        );
    }

    private ConfiguracaoEmpresa buscarEmpresa() {

        return repository
                .findById(ConfiguracaoEmpresa.ID_UNICO)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Configuração da empresa ainda não cadastrada"
                        )
                );
    }

    private void validarCnpj(String cnpj) {

        if (!CnpjValidator.valido(cnpj)) {
            throw new RecursoInvalidoException(
                    "CNPJ inválido"
            );
        }
    }

    private void validarLogo(
            MultipartFile arquivo
    ) {

        if (arquivo == null || arquivo.isEmpty()) {
            throw new RecursoInvalidoException(
                    "O arquivo da logo é obrigatório"
            );
        }

        if (arquivo.getSize() > TAMANHO_MAXIMO_LOGO) {
            throw new RecursoInvalidoException(
                    "A logo deve possuir no máximo 2 MB"
            );
        }

        String contentType = arquivo.getContentType();

        if (contentType == null ||
                !TIPOS_LOGO_PERMITIDOS.contains(contentType)) {

            throw new RecursoInvalidoException(
                    "Formato de imagem inválido. Utilize PNG, JPEG ou WebP"
            );
        }
    }
}