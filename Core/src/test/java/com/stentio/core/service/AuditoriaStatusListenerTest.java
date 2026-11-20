package com.stentio.core.service;

import com.stentio.core.config.DadosUsuarioToken;
import com.stentio.core.event.StatusAlteradoEvento;
import com.stentio.core.model.EntidadeAuditada;
import com.stentio.core.model.LogAuditoria;
import com.stentio.core.model.OrigemAcao;
import com.stentio.core.repository.LogAuditoriaRepository;
import com.stentio.core.support.AtorAuditoria;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// US-019 T6: o listener transforma o evento em log com status, ator e origem corretos.
class AuditoriaStatusListenerTest {

    private static final Instant AGORA = Instant.parse("2026-11-14T14:32:10Z");
    private static final UUID ETAPA_ID = UUID.randomUUID();
    private static final UUID GESTOR_ID = UUID.randomUUID();

    private final LogAuditoriaRepository logs = mock(LogAuditoriaRepository.class);
    private final AuditoriaStatusListener listener =
            new AuditoriaStatusListener(logs, Clock.fixed(AGORA, ZoneOffset.UTC));

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private static StatusAlteradoEvento eventoDeEtapa(String anterior, String novo, AtorAuditoria ator) {
        return new StatusAlteradoEvento(EntidadeAuditada.ORDEM_SERVICO_ETAPA, ETAPA_ID,
                "OS OS-2026-ABC123 - Etapa 1 (Tradução)", anterior, novo,
                "Recurso Ana Silva alocado à etapa.", ator);
    }

    private static void autenticar(String email, String role, DadosUsuarioToken dados) {
        var autenticacao = new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        autenticacao.setDetails(dados);
        SecurityContextHolder.getContext().setAuthentication(autenticacao);
    }

    private LogAuditoria logGravado() {
        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(logs).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Grava status anterior/novo e o usuário do JWT como ator, com origem PAINEL_WEB")
    void gravaComAtorDoToken() {
        autenticar("carlos@stentio.com", "GESTOR_PROJETO", new DadosUsuarioToken(GESTOR_ID, "Carlos Mendes"));

        listener.registrar(eventoDeEtapa("PENDENTE", "ALOCADO", null));

        LogAuditoria log = logGravado();
        assertEquals(EntidadeAuditada.ORDEM_SERVICO_ETAPA, log.getEntidade());
        assertEquals(ETAPA_ID, log.getEntidadeId());
        assertEquals("OS OS-2026-ABC123 - Etapa 1 (Tradução)", log.getReferenciaLegivel());
        assertEquals("PENDENTE", log.getStatusAnterior());
        assertEquals("ALOCADO", log.getStatusNovo());
        assertEquals(GESTOR_ID, log.getUsuarioId());
        assertEquals("Carlos Mendes", log.getUsuarioNome());
        assertEquals("GESTOR_PROJETO", log.getUsuarioPerfil());
        assertEquals(OrigemAcao.PAINEL_WEB, log.getOrigemAcao());
        assertEquals("Recurso Ana Silva alocado à etapa.", log.getObservacao());
        assertEquals(AGORA, log.getDataHora());
    }

    @Test
    @DisplayName("CA.4: ator informado no evento prevalece — cliente pelo link vira TOKEN_PUBLICO")
    void atorExplicitoDoLinkPublico() {
        // Mesmo com alguém autenticado na thread, quem decidiu foi o dono do link.
        autenticar("admin@stentio.com", "ADMIN", new DadosUsuarioToken(UUID.randomUUID(), "Admin"));

        listener.registrar(new StatusAlteradoEvento(EntidadeAuditada.ORCAMENTO, UUID.randomUUID(),
                "Orçamento ORC-2026-AAAAAA", "AGUARDANDO_APROVACAO", "APROVADO",
                "Aprovado via token de e-mail.", AtorAuditoria.clienteLinkPublico()));

        LogAuditoria log = logGravado();
        assertEquals(OrigemAcao.TOKEN_PUBLICO, log.getOrigemAcao());
        assertEquals("Cliente (Link Público)", log.getUsuarioNome());
        assertEquals("CLIENTE_EXTERNO", log.getUsuarioPerfil());
        assertNull(log.getUsuarioId());
    }

    @Test
    @DisplayName("Sem autenticação e sem ator no evento, quem agiu foi o SISTEMA")
    void semAutenticacaoViraSistema() {
        listener.registrar(new StatusAlteradoEvento(EntidadeAuditada.ORDEM_SERVICO, UUID.randomUUID(),
                "OS OS-2026-BBBBBB", null, "PENDENTE_ALOCACAO", null, null));

        LogAuditoria log = logGravado();
        assertEquals(OrigemAcao.SISTEMA, log.getOrigemAcao());
        assertEquals("Sistema", log.getUsuarioNome());
        assertEquals("SISTEMA", log.getUsuarioPerfil());
        assertNull(log.getStatusAnterior());
    }

    @Test
    @DisplayName("Token antigo, sem as claims userId e nome, cai para o e-mail")
    void tokenSemClaimsUsaEmail() {
        autenticar("gestor@stentio.com", "GESTOR_PROJETO", new DadosUsuarioToken(null, null));

        listener.registrar(eventoDeEtapa("PENDENTE", "ALOCADO", null));

        LogAuditoria log = logGravado();
        assertNull(log.getUsuarioId());
        assertEquals("gestor@stentio.com", log.getUsuarioNome());
    }

    @Test
    @DisplayName("Mesmo status antes e depois não é transição e não gera log")
    void semTransicaoNaoGrava() {
        listener.registrar(eventoDeEtapa("ALOCADO", "ALOCADO", null));

        verify(logs, never()).save(any());
    }

    @Test
    @DisplayName("Observação acima do limite é cortada em vez de derrubar a transação")
    void observacaoLongaECortada() {
        String motivo = "x".repeat(3000);
        listener.registrar(new StatusAlteradoEvento(EntidadeAuditada.ORCAMENTO, UUID.randomUUID(),
                "Orçamento ORC-2026-CCCCCC", "AGUARDANDO_APROVACAO", "REJEITADO",
                motivo, AtorAuditoria.clienteLinkPublico()));

        assertEquals(2500, logGravado().getObservacao().length());
    }
}
