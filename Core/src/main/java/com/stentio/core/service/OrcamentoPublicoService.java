package com.stentio.core.service;

import com.stentio.core.dto.EmpresaPublicaResponse;
import com.stentio.core.dto.OrcamentoDecisaoResponse;
import com.stentio.core.dto.OrcamentoPublicoResponse;
import com.stentio.core.event.OrcamentoDecisaoEvento;
import com.stentio.core.exception.LinkOrcamentoInvalidoException;
import com.stentio.core.model.*;
import com.stentio.core.repository.*;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OrcamentoPublicoService {
    private final OrcamentoRepository orcamentos;
    private final OrcamentoHistoricoRepository historico;
    private final OrcamentoDecisaoOutboxRepository outbox;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final RestTemplate emailServiceRestTemplate;

    public OrcamentoPublicoService(OrcamentoRepository orcamentos, OrcamentoHistoricoRepository historico,
                                  OrcamentoDecisaoOutboxRepository outbox, ObjectMapper objectMapper, Clock clock,
                                  @Qualifier("emailServiceRestTemplate") RestTemplate emailServiceRestTemplate) {
        this.orcamentos = orcamentos;
        this.historico = historico;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.emailServiceRestTemplate = emailServiceRestTemplate;
    }

    public OrcamentoPublicoResponse consultar(String token) {
        Orcamento orcamento = orcamentos.buscarPublicoPorTokenHash(TokenOrcamento.hash(token))
                .orElseThrow(LinkOrcamentoInvalidoException::new);
        validar(orcamento, clock.instant());
        if (orcamento.getSolicitacao() != null) {
            Hibernate.initialize(orcamento.getSolicitacao());
            if (orcamento.getSolicitacao().getArquivos() != null) {
                orcamento.getSolicitacao().getArquivos().forEach(Hibernate::initialize);
            }
            if (orcamento.getSolicitacao().getCategoriaProjeto() != null) {
                Hibernate.initialize(orcamento.getSolicitacao().getCategoriaProjeto());
            }
            if (orcamento.getSolicitacao().getIdiomaOrigem() != null) {
                Hibernate.initialize(orcamento.getSolicitacao().getIdiomaOrigem());
            }
            if (orcamento.getSolicitacao().getIdiomaDestino() != null) {
                Hibernate.initialize(orcamento.getSolicitacao().getIdiomaDestino());
            }
        }
        if (orcamento.getItens() != null) {
            orcamento.getItens().forEach(item -> {
                if (item.getServicos() != null) {
                    item.getServicos().forEach(Hibernate::initialize);
                }
            });
        }
        return OrcamentoPublicoResponse.de(orcamento);
    }

    public ArquivoPublicoParaDownload baixarArquivoPublico(String token, UUID arquivoId) {
        Orcamento orcamento = orcamentos.buscarPublicoPorTokenHash(TokenOrcamento.hash(token))
                .orElseThrow(LinkOrcamentoInvalidoException::new);
        validar(orcamento, clock.instant());

        if (orcamento.getSolicitacao() == null) {
            throw new LinkOrcamentoInvalidoException();
        }

        ArquivoSolicitacao arquivo = orcamento.getSolicitacao().getArquivos().stream()
                .filter(a -> a.getId().equals(arquivoId))
                .findFirst()
                .orElseThrow(LinkOrcamentoInvalidoException::new);

        return new ArquivoPublicoParaDownload(arquivo.getCaminho(), arquivo.getNomeOriginal(),
                arquivo.getTipoConteudo(), arquivo.getTamanhoBytes());
    }

    public record ArquivoPublicoParaDownload(String caminho, String nomeOriginal,
                                            String tipoConteudo, long tamanhoBytes) {
    }

    public EmpresaPublicaResponse consultarEmpresa(String token) {
        Orcamento orcamento = orcamentos.findByTokenHash(TokenOrcamento.hash(token))
                .orElseThrow(LinkOrcamentoInvalidoException::new);
        validar(orcamento, clock.instant());
        try {
            ResponseEntity<String> resposta = emailServiceRestTemplate.getForEntity("/api/v1/empresa", String.class);
            if (resposta.getBody() != null) {
                String body = resposta.getBody();
                JsonNode node = objectMapper.readTree(body);
                String nome = node.path("nome").asText(null);
                String cnpj = node.path("cnpj").asText(null);
                String endereco = node.path("endereco").asText(null);
                String telefone = node.path("telefone").asText(null);
                Boolean logoDisponivel = null;
                if (node.has("logo_disponivel")) {
                    if (!node.get("logo_disponivel").isNull()) {
                        logoDisponivel = node.get("logo_disponivel").asBoolean();
                    }
                } else if (node.has("logoDisponivel")) {
                    if (!node.get("logoDisponivel").isNull()) {
                        logoDisponivel = node.get("logoDisponivel").asBoolean();
                    }
                }
                return new EmpresaPublicaResponse(nome, cnpj, endereco, telefone, logoDisponivel);
            }
        } catch (Exception ex) {
            // fallback
        }
        return new EmpresaPublicaResponse("Esul", "19534695000110", "Rodovia Tancredo de Almeida Neves", "(16) 3626-8181", true);
    }

    public byte[] obterLogoEmpresa(String token) {
        Orcamento orcamento = orcamentos.findByTokenHash(TokenOrcamento.hash(token))
                .orElseThrow(LinkOrcamentoInvalidoException::new);
        validar(orcamento, clock.instant());
        try {
            ResponseEntity<byte[]> resposta = emailServiceRestTemplate.getForEntity("/api/v1/empresa/logo", byte[].class);
            return resposta.getBody();
        } catch (RestClientException ex) {
            return null;
        }
    }

    public String obterContentTypeLogoEmpresa(String token) {
        Orcamento orcamento = orcamentos.findByTokenHash(TokenOrcamento.hash(token))
                .orElseThrow(LinkOrcamentoInvalidoException::new);
        validar(orcamento, clock.instant());
        try {
            ResponseEntity<byte[]> resposta = emailServiceRestTemplate.getForEntity("/api/v1/empresa/logo", byte[].class);
            if (resposta.getHeaders().getContentType() != null) {
                return resposta.getHeaders().getContentType().toString();
            }
        } catch (RestClientException ex) {
            // ignora
        }
        return "image/png";
    }

    @Transactional
    public OrcamentoDecisaoResponse aprovar(String token) {
        return decidir(token, StatusOrcamento.APROVADO, null);
    }

    @Transactional
    public OrcamentoDecisaoResponse rejeitar(String token, String motivo) {
        if (motivo == null || motivo.isBlank() || motivo.length() > 2000) {
            throw new IllegalArgumentException("Motivo da rejeição é obrigatório e deve ter no máximo 2000 caracteres");
        }
        return decidir(token, StatusOrcamento.REJEITADO, motivo.strip());
    }

    private OrcamentoDecisaoResponse decidir(String token, StatusOrcamento decisao, String motivo) {
        Orcamento orcamento = orcamentos.buscarPorTokenComLock(TokenOrcamento.hash(token))
                .orElseThrow(LinkOrcamentoInvalidoException::new);
        Instant agora = clock.instant();
        validar(orcamento, agora);

        OrcamentoHistorico registro = new OrcamentoHistorico();
        registro.setOrcamento(orcamento);
        registro.setStatusAnterior(orcamento.getStatus());
        registro.setStatusNovo(decisao);
        registro.setMotivo(motivo);
        registro.setDataDecisao(agora);

        orcamento.setStatus(decisao);
        if (orcamento.getSolicitacao() != null) {
            orcamento.getSolicitacao().atualizarStatusDoOrcamento(decisao);
        }
        orcamento.setTokenUtilizadoEm(agora);
        orcamentos.save(orcamento);
        historico.save(registro);

        UUID eventoId = UUID.randomUUID();
        OrcamentoDecisaoEvento evento = new OrcamentoDecisaoEvento(eventoId, 1,
                orcamento.getId(), decisao, motivo, agora);
        OrcamentoDecisaoOutbox pendente = new OrcamentoDecisaoOutbox();
        pendente.setId(eventoId);
        pendente.setOrcamentoId(orcamento.getId());
        pendente.setRoutingKey(decisao == StatusOrcamento.APROVADO ? "orcamento.aprovado" : "orcamento.rejeitado");
        pendente.setPayload(objectMapper.writeValueAsString(evento));
        pendente.setCriadoEm(agora);
        pendente.setProximaTentativaEm(agora);
        outbox.save(pendente);

        return new OrcamentoDecisaoResponse(orcamento.getId(), decisao, agora);
    }

    private void validar(Orcamento orcamento, Instant agora) {
        ValidadeLinkOrcamento.validar(orcamento.getStatus(), orcamento.getDataExpiracao(),
                orcamento.getTokenUtilizadoEm(), agora);
    }
}
