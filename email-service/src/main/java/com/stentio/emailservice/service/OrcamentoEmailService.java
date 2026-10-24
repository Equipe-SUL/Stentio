package com.stentio.emailservice.service;

import com.stentio.emailservice.dto.MensagemEmail;
import com.stentio.emailservice.dto.OrcamentoEmailMensagem;
import com.stentio.emailservice.model.ConfiguracaoEmpresaDocument;
import com.stentio.emailservice.repository.ConfiguracaoEmpresaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * US-13: junta as três fontes do e-mail de aprovação — os dados do orçamento que vieram do
 * Core pela fila, os dados da empresa que estão no Mongo daqui e os arquivos da solicitação
 * que estão no volume compartilhado — e entrega a mensagem pronta ao EmailService.
 */
@Service
public class OrcamentoEmailService {

    private static final Logger log = LoggerFactory.getLogger(OrcamentoEmailService.class);

    private final ConfiguracaoEmpresaRepository empresas;
    private final AnexoSolicitacaoStorage anexos;
    private final OrcamentoEmailRenderer renderer;
    private final EmailService emailService;

    public OrcamentoEmailService(ConfiguracaoEmpresaRepository empresas,
                                 AnexoSolicitacaoStorage anexos,
                                 OrcamentoEmailRenderer renderer,
                                 EmailService emailService) {
        this.empresas = empresas;
        this.anexos = anexos;
        this.renderer = renderer;
        this.emailService = emailService;
    }

    public void enviar(OrcamentoEmailMensagem mensagem) {
        validar(mensagem);

        ConfiguracaoEmpresaDocument empresa = empresaCadastrada();
        boolean logoInline = empresa != null && empresa.possuiLogo();
        AnexoSolicitacaoStorage.Resultado arquivos = anexos.carregar(mensagem.anexos());
        OrcamentoEmailRenderer.CorpoEmail corpo = renderer.renderizar(mensagem, empresa, arquivos, logoInline);

        emailService.enviar(new MensagemEmail(
                mensagem.to(),
                mensagem.subject(),
                corpo.texto(),
                corpo.html(),
                arquivos.carregados(),
                logoInline ? new MensagemEmail.ImagemInline(OrcamentoEmailRenderer.CID_LOGO,
                        empresa.getLogo(), empresa.getLogoContentType()) : null));
    }

    /**
     * O cadastro da empresa é opcional no sistema, então a ausência dele não pode impedir o
     * orçamento de chegar ao cliente: o e-mail sai sem o rodapé de contato, com aviso no log.
     */
    private ConfiguracaoEmpresaDocument empresaCadastrada() {
        ConfiguracaoEmpresaDocument empresa =
                empresas.findById(ConfiguracaoEmpresaDocument.ID_UNICO).orElse(null);
        if (empresa == null) {
            log.warn("Empresa não cadastrada: o e-mail do orçamento vai sair sem os dados de contato");
        }
        return empresa;
    }

    private static void validar(OrcamentoEmailMensagem mensagem) {
        boolean completa = mensagem != null
                && mensagem.to() != null && !mensagem.to().isBlank()
                && mensagem.subject() != null && !mensagem.subject().isBlank()
                && mensagem.orcamento() != null
                && mensagem.orcamento().codigo() != null && !mensagem.orcamento().codigo().isBlank()
                && mensagem.orcamento().linkAprovacao() != null
                && !mensagem.orcamento().linkAprovacao().isBlank();
        if (!completa) {
            throw new IllegalArgumentException(
                    "Mensagem de orçamento incompleta: to, subject, código e link são obrigatórios");
        }
    }
}
