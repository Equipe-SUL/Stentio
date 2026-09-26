package com.stentio.emailservice.service;

import com.stentio.emailservice.dto.EmpresaPatchRequest;
import com.stentio.emailservice.dto.EmpresaRequest;
import com.stentio.emailservice.dto.EmpresaResponse;
import com.stentio.emailservice.exception.EmpresaNaoCadastradaException;
import com.stentio.emailservice.exception.RequisicaoInvalidaException;
import com.stentio.emailservice.model.ConfiguracaoEmpresaDocument;
import com.stentio.emailservice.repository.ConfiguracaoEmpresaRepository;
import com.stentio.emailservice.validation.CnpjValidator;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
public class ConfiguracaoEmpresaService {

    private static final long TAMANHO_MAXIMO_LOGO = 2 * 1024 * 1024L;
    private static final Set<String> TIPOS_ACEITOS = Set.of("image/png", "image/jpeg", "image/webp");

    private final ConfiguracaoEmpresaRepository repository;

    public ConfiguracaoEmpresaService(ConfiguracaoEmpresaRepository repository) {
        this.repository = repository;
    }

    public EmpresaResponse buscar() {
        return new EmpresaResponse(obterOuFalhar());
    }

    public EmpresaResponse salvar(EmpresaRequest request) {

        if (!CnpjValidator.valido(request.cnpj())) {
            throw new RequisicaoInvalidaException("CNPJ inválido");
        }

        ConfiguracaoEmpresaDocument empresa = repository
                .findById(ConfiguracaoEmpresaDocument.ID_UNICO)
                .orElseGet(() -> new ConfiguracaoEmpresaDocument(
                        ConfiguracaoEmpresaDocument.ID_UNICO,
                        request.nome(),
                        request.cnpj(),
                        request.endereco(),
                        request.telefone()
                ));

        empresa.atualizarDados(
                request.nome(),
                request.cnpj(),
                request.endereco(),
                request.telefone()
        );

        return new EmpresaResponse(repository.save(empresa));
    }

    public EmpresaResponse salvarLogo(MultipartFile arquivo) {

        if (arquivo == null || arquivo.isEmpty()) {
            throw new RequisicaoInvalidaException("O arquivo da logo é obrigatório");
        }

        if (arquivo.getSize() > TAMANHO_MAXIMO_LOGO) {
            throw new RequisicaoInvalidaException("A logo deve possuir no máximo 2 MB");
        }

        String contentType = arquivo.getContentType();

        if (contentType == null || !TIPOS_ACEITOS.contains(contentType)) {
            throw new RequisicaoInvalidaException(
                    "Formato de imagem inválido. Utilize PNG, JPEG ou WebP"
            );
        }

        ConfiguracaoEmpresaDocument empresa = obterOuFalhar();

        byte[] conteudo;

        try {
            conteudo = arquivo.getBytes();
        } catch (IOException exception) {
            throw new RequisicaoInvalidaException("Não foi possível processar o arquivo da logo");
        }

        // O Content-Type e a extensão vêm do cliente, então não bastam: um .txt
        // renomeado para logo.png passaria e seria devolvido inline como image/png.
        // Confere os magic numbers para garantir que o conteúdo é mesmo uma imagem.
        if (!formatoConfereComConteudo(contentType, conteudo)) {
            throw new RequisicaoInvalidaException(
                    "O conteúdo do arquivo não corresponde a uma imagem válida"
            );
        }

        empresa.atualizarLogo(conteudo, contentType, arquivo.getOriginalFilename());

        return new EmpresaResponse(repository.save(empresa));
    }

    private boolean formatoConfereComConteudo(String contentType, byte[] conteudo) {

        byte[] assinatura;

        switch (contentType) {
            case MediaType.IMAGE_PNG_VALUE -> assinatura = new byte[]{
                    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
            };
            case MediaType.IMAGE_JPEG_VALUE -> assinatura = new byte[]{
                    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
            };
            // WebP: "RIFF" .... "WEBP" — os bytes 8..11 carregam o subtipo real.
            case "image/webp" -> {
                if (conteudo.length < 12) {
                    return false;
                }
                boolean riff = conteudo[0] == 'R' && conteudo[1] == 'I'
                        && conteudo[2] == 'F' && conteudo[3] == 'F';
                boolean webp = conteudo[8] == 'W' && conteudo[9] == 'E'
                        && conteudo[10] == 'B' && conteudo[11] == 'P';
                return riff && webp;
            }
            default -> {
                return false;
            }
        }

        if (conteudo.length < assinatura.length) {
            return false;
        }

        for (int i = 0; i < assinatura.length; i++) {
            if (conteudo[i] != assinatura[i]) {
                return false;
            }
        }

        return true;
    }

    public byte[] obterLogo() {
        return obterOuFalhar().getLogo();
    }

    public String obterContentTypeLogo() {
        return obterOuFalhar().getLogoContentType();
    }

    public EmpresaResponse atualizarParcialmente(EmpresaPatchRequest request) {

        ConfiguracaoEmpresaDocument empresa = obterOuFalhar();

        boolean possuiAlteracao = false;

        if (request.nome() != null) {

            if (request.nome().isBlank()) {
                throw new RequisicaoInvalidaException("Nome não pode ser vazio");
            }

            empresa.atualizarNome(request.nome());
            possuiAlteracao = true;
        }

        if (request.cnpj() != null) {

            if (!CnpjValidator.valido(request.cnpj())) {
                throw new RequisicaoInvalidaException("CNPJ inválido");
            }

            empresa.atualizarCnpj(request.cnpj());
            possuiAlteracao = true;
        }

        if (request.endereco() != null) {

            if (request.endereco().isBlank()) {
                throw new RequisicaoInvalidaException("Endereço não pode ser vazio");
            }

            empresa.atualizarEndereco(request.endereco());
            possuiAlteracao = true;
        }

        if (request.telefone() != null) {
            empresa.atualizarTelefone(request.telefone());
            possuiAlteracao = true;
        }

        if (!possuiAlteracao) {
            throw new RequisicaoInvalidaException("Nenhum campo foi informado para atualização");
        }

        return new EmpresaResponse(repository.save(empresa));
    }

    private ConfiguracaoEmpresaDocument obterOuFalhar() {
        return repository
                .findById(ConfiguracaoEmpresaDocument.ID_UNICO)
                .orElseThrow(EmpresaNaoCadastradaException::new);
    }
}
