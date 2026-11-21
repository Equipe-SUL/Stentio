package com.stentio.core.model;

import com.stentio.core.exception.TransicaoOrdemServicoInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** US-015: a máquina de estados da OS no fim do fluxo, sem banco e sem Spring. */
class OrdemServicoEntregaFinalTest {

    private static final Instant AGORA = Instant.parse("2026-11-21T12:00:00Z");
    private static final String GESTOR = "gestor@aliancatraducoes.com";

    private OrdemServico ordem;

    @BeforeEach
    void preparar() {
        Orcamento orcamento = mock(Orcamento.class);
        when(orcamento.getSolicitacao()).thenReturn(mock(Solicitacao.class));
        ordem = new OrdemServico(orcamento);
        ordem.planejar();
        ordem.iniciarExecucao();
        ordem.aguardarVerificacao();
    }

    @Test
    @DisplayName("CA.8: encerrar grava quem aprovou e quando")
    void encerrarRegistraAEntrega() {
        ordem.encerrar(AGORA, GESTOR);

        assertEquals(StatusOrdemServico.ENCERRADA, ordem.getStatus());
        assertEquals(AGORA, ordem.getEntregueEm());
        assertEquals(GESTOR, ordem.getEntreguePor());
    }

    @Test
    @DisplayName("CA.7: OS encerrada não é reprovada")
    void naoReprovaOrdemEncerrada() {
        ordem.encerrar(AGORA, GESTOR);

        assertThrows(TransicaoOrdemServicoInvalidaException.class,
                () -> ordem.reprovarEntrega("Faltou a assinatura"));
        assertEquals(StatusOrdemServico.ENCERRADA, ordem.getStatus());
    }

    @Test
    @DisplayName("CA.11: reprovar devolve a OS para execução com o motivo registrado")
    void reprovarVoltaParaExecucao() {
        ordem.reprovarEntrega("Falta a assinatura no último documento");

        assertEquals(StatusOrdemServico.EM_EXECUCAO, ordem.getStatus());
        assertEquals("Falta a assinatura no último documento", ordem.getMotivoRevisao());
        assertNull(ordem.getEntregueEm());
        assertNull(ordem.getEntreguePor());
    }

    @Test
    @DisplayName("CA.13: a nova verificação limpa o motivo da revisão anterior")
    void novaVerificacaoLimpaOMotivo() {
        ordem.reprovarEntrega("Falta a assinatura");

        ordem.aguardarVerificacao();

        assertEquals(StatusOrdemServico.AGUARDANDO_VERIFICACAO, ordem.getStatus());
        assertNull(ordem.getMotivoRevisao());
    }

    @Test
    @DisplayName("só se encerra o que está aguardando verificação")
    void naoEncerraOrdemEmExecucao() {
        ordem.reprovarEntrega("Falta a assinatura");

        assertThrows(TransicaoOrdemServicoInvalidaException.class, () -> ordem.encerrar(AGORA, GESTOR));
        assertEquals(StatusOrdemServico.EM_EXECUCAO, ordem.getStatus());
    }
}
