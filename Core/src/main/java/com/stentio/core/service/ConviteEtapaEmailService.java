package com.stentio.core.service;

import com.stentio.core.dto.NotificacaoEtapaMensagem;
import com.stentio.core.model.NotificacaoEtapaOutbox;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.repository.NotificacaoEtapaOutboxRepository;
import com.stentio.core.support.CodigoVisual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * US-013 CA.2: monta o convite e o grava na outbox, dentro da transação do disparo.
 *
 * <p>Texto puro de propósito. O convite diz o que é o trabalho, até quando, e dá o
 * endereço do portal — nada aqui pede HTML, e texto puro chega legível em qualquer cliente
 * de e-mail e passa melhor por filtro de spam.
 */
@Service
public class ConviteEtapaEmailService {

    private static final Logger log = LoggerFactory.getLogger(ConviteEtapaEmailService.class);

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", new Locale("pt", "BR"));

    private final NotificacaoEtapaOutboxRepository outbox;
    private final ObjectMapper objectMapper;
    private final String urlPortal;

    public ConviteEtapaEmailService(NotificacaoEtapaOutboxRepository outbox,
                                    ObjectMapper objectMapper,
                                    @Value("${app.portal-recurso.url:http://localhost/recurso/demandas}") String urlPortal) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.urlPortal = urlPortal;

        if (urlPortal.contains("localhost") || urlPortal.contains("127.0.0.1")) {
            // O profissional recebe este link fora da rede da Aliança. Em produção, um
            // localhost silencioso vira um convite que ninguém consegue abrir.
            log.warn("app.portal-recurso.url aponta para {} — convites enviados assim não abrem "
                    + "fora desta máquina", urlPortal);
        }
    }

    /** Grava um convite por elegível. Nada é publicado aqui: quem publica é o dispatcher. */
    public void convidar(EtapaOrdemServico etapa, List<Recurso> elegiveis, Instant agora) {
        String assunto = "Nova etapa disponível: " + etapa.getNome();

        List<NotificacaoEtapaOutbox> convites = elegiveis.stream()
                .map(recurso -> new NotificacaoEtapaOutbox(
                        etapa,
                        recurso.getEmail(),
                        objectMapper.writeValueAsString(
                                new NotificacaoEtapaMensagem(recurso.getEmail(), assunto, corpo(etapa, recurso))),
                        agora))
                .toList();

        outbox.saveAll(convites);
    }

    private String corpo(EtapaOrdemServico etapa, Recurso recurso) {
        Solicitacao solicitacao = etapa.getOrdemServico().getSolicitacao();
        String codigo = CodigoVisual.gerar("OS", etapa.getOrdemServico().getCriadoEm(),
                etapa.getOrdemServico().getId());

        StringBuilder corpo = new StringBuilder()
                .append("Olá, ").append(recurso.getNome()).append(".\n\n")
                .append("Uma nova etapa está disponível e o seu perfil atende aos requisitos.\n\n")
                .append("Ordem de serviço: ").append(codigo).append('\n')
                .append("Etapa: ").append(etapa.getNome()).append('\n')
                .append("Serviço: ").append(etapa.getTipoServico().getNome()).append('\n')
                .append("Idiomas: ").append(solicitacao.getIdiomaOrigem().getNome())
                .append(" para ").append(solicitacao.getIdiomaDestino().getNome()).append('\n')
                .append("Esforço estimado: ").append(etapa.getPrazoEstimadoHoras()).append(" horas\n");

        if (etapa.getDataLimiteEntrega() != null) {
            corpo.append("Entrega até: ")
                    .append(DATA_HORA.format(etapa.getDataLimiteEntrega().atZone(FUSO)))
                    .append('\n');
        }

        return corpo
                .append("\nQuem aceitar primeiro assume a etapa. Para aceitar, acesse o portal:\n")
                .append(urlPortal)
                .append("\n\nEquipe Aliança Traduções\n")
                .toString();
    }
}
