package com.stentio.emailservice.service;

import com.stentio.emailservice.dto.EntregaFinalEmailMensagem;
import com.stentio.emailservice.dto.MensagemEmail;
import com.stentio.emailservice.exception.AnexoIndisponivelException;
import com.stentio.emailservice.model.ConfiguracaoEmpresaDocument;
import com.stentio.emailservice.repository.ConfiguracaoEmpresaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * US-015 CA.5: monta e envia ao cliente o e-mail com o documento final.
 *
 * <p>Tudo aqui é obrigatório. No e-mail do orçamento um anexo que falta vira um aviso no
 * log e a mensagem sai assim mesmo; aqui o anexo é o próprio motivo do e-mail, então a
 * falha sobe e a mensagem volta para a fila.
 */
@Service
public class EntregaFinalEmailService {

    private static final Logger log = LoggerFactory.getLogger(EntregaFinalEmailService.class);

    private final ConfiguracaoEmpresaRepository empresas;
    private final AnexoSolicitacaoStorage anexos;
    private final EntregaFinalEmailRenderer renderer;
    private final EmailService emailService;

    public EntregaFinalEmailService(ConfiguracaoEmpresaRepository empresas,
                                    AnexoSolicitacaoStorage anexos,
                                    EntregaFinalEmailRenderer renderer,
                                    EmailService emailService) {
        this.empresas = empresas;
        this.anexos = anexos;
        this.renderer = renderer;
        this.emailService = emailService;
    }

    public void enviar(EntregaFinalEmailMensagem mensagem) {
        validar(mensagem);

        ConfiguracaoEmpresaDocument empresa =
                empresas.findById(ConfiguracaoEmpresaDocument.ID_UNICO).orElse(null);
        if (empresa == null) {
            log.warn("Empresa não cadastrada: a entrega final vai sair sem o nome na assinatura");
        }

        var anexo = anexos.carregarObrigatorio(
                mensagem.anexo().nomeArquivo(),
                mensagem.anexo().caminho(),
                mensagem.anexo().tipoConteudo());

        EntregaFinalEmailRenderer.CorpoEmail corpo = renderer.renderizar(mensagem, empresa);

        emailService.enviar(new MensagemEmail(
                mensagem.to(),
                mensagem.subject(),
                corpo.texto(),
                corpo.html(),
                List.of(anexo),
                null));

        log.info("Entrega final da OS {} enviada ({})",
                mensagem.ordemServico().codigo(), mensagem.idempotencyKey());
    }

    /**
     * Recusa o que não dá para enviar antes de tocar no disco.
     *
     * <p>A versão do schema é conferida de propósito: uma mensagem de um formato futuro
     * precisa falhar dizendo isso, e não desserializar pela metade e mandar um e-mail
     * incompleto ao cliente.
     */
    private static void validar(EntregaFinalEmailMensagem mensagem) {
        if (mensagem == null || mensagem.schemaVersao() == null
                || mensagem.schemaVersao() != EntregaFinalEmailMensagem.SCHEMA_SUPORTADO) {
            throw new AnexoIndisponivelException(
                    "Entrega final em versão de schema não suportada por este serviço");
        }
        boolean completa = mensagem.to() != null && !mensagem.to().isBlank()
                && mensagem.subject() != null && !mensagem.subject().isBlank()
                && mensagem.ordemServico() != null
                && mensagem.ordemServico().codigo() != null
                && mensagem.anexo() != null
                && mensagem.anexo().caminho() != null && !mensagem.anexo().caminho().isBlank()
                && mensagem.anexo().nomeArquivo() != null && !mensagem.anexo().nomeArquivo().isBlank();
        if (!completa) {
            throw new AnexoIndisponivelException("Entrega final sem destinatário, código da OS ou anexo");
        }
    }
}
