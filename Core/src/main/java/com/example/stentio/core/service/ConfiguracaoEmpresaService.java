package com.example.stentio.core.service;

import com.example.stentio.core.dto.EmpresaRequest;
import com.example.stentio.core.dto.EmpresaResponse;
import com.example.stentio.core.model.ConfiguracaoEmpresa;
import com.example.stentio.core.repository.ConfiguracaoEmpresaRepository;
import com.example.stentio.core.validation.CnpjValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ConfiguracaoEmpresaService {

    private final ConfiguracaoEmpresaRepository repository;

    public ConfiguracaoEmpresaService(
            ConfiguracaoEmpresaRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public EmpresaResponse buscar() {

        ConfiguracaoEmpresa empresa = repository
                .findById(ConfiguracaoEmpresa.ID_UNICO)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Configuração da empresa ainda não cadastrada"
                        )
                );

        return new EmpresaResponse(empresa);
    }

    @Transactional
    public EmpresaResponse salvar(EmpresaRequest request) {

        if (!CnpjValidator.valido(request.cnpj())) {
            throw new IllegalArgumentException("CNPJ inválido");
        }

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

        ConfiguracaoEmpresa salva = repository.save(empresa);

        return new EmpresaResponse(salva);
    }

    @Transactional
    public EmpresaResponse salvarLogo(MultipartFile arquivo) {

        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException(
                    "O arquivo da logo é obrigatório"
            );
        }

        if (arquivo.getSize() > 2 * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "A logo deve possuir no máximo 2 MB"
            );
        }

        String contentType = arquivo.getContentType();

        if (contentType == null ||
                (!contentType.equals("image/png")
                        && !contentType.equals("image/jpeg")
                        && !contentType.equals("image/webp"))) {

            throw new IllegalArgumentException(
                    "Formato de imagem inválido. Utilize PNG, JPEG ou WebP"
            );
        }

        ConfiguracaoEmpresa empresa = repository
                .findById(ConfiguracaoEmpresa.ID_UNICO)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Cadastre os dados da empresa antes de enviar a logo"
                        )
                );

        try {
            empresa.atualizarLogo(
                    arquivo.getBytes(),
                    contentType,
                    arquivo.getOriginalFilename()
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Não foi possível processar o arquivo da logo",
                    exception
            );
        }

        return new EmpresaResponse(repository.save(empresa));
    }
}