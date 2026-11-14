package com.stentio.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// US-012: a situação é a leitura conjunta do status de fluxo com a alocação.
class SituacaoEtapaTest {

    private final TipoServico traducao = tipo();

    private static TipoServico tipo() {
        TipoServico t = new TipoServico();
        t.setId(UUID.randomUUID());
        t.setNome("Tradução");
        t.setAtivo(true);
        return t;
    }

    private static Idioma idioma() {
        Idioma i = mock(Idioma.class);
        when(i.getId()).thenReturn(UUID.randomUUID());
        return i;
    }

    private EtapaOrdemServico etapa(int ordem) {
        return new EtapaOrdemServico(mock(OrdemServico.class), ordem, "Etapa", traducao, 8);
    }

    private static Recurso recursoApto(Idioma origem, Idioma destino, TipoServico tipo) {
        Recurso r = new Recurso("Ana Silva", "ana@traducoes.com", null);
        r.definirTiposServico(List.of(tipo));
        r.definirPrecos(List.of(new Recurso.NovoPreco(
                origem, destino, UnidadeCobranca.PALAVRA, new BigDecimal("0.35"))));
        return r;
    }

    @Test
    @DisplayName("Sem recurso, a situação é o próprio status do fluxo")
    void semRecursoMostraOFluxo() {
        assertEquals(SituacaoEtapa.PENDENTE, etapa(1).situacao());
        assertEquals(SituacaoEtapa.AGUARDANDO_ETAPA_ANTERIOR, etapa(2).situacao());
    }

    @Test
    @DisplayName("Com recurso, a situação é ALOCADO, como o contrato do card pede")
    void comRecursoMostraAlocado() {
        EtapaOrdemServico etapa = etapa(1);
        etapa.alocar(recursoApto(idioma(), idioma(), traducao),
                Instant.parse("2026-12-01T12:00:00Z"), Instant.parse("2026-11-09T12:00:00Z"));

        assertEquals(SituacaoEtapa.ALOCADO, etapa.situacao());
    }

    @Test
    @DisplayName("Alocar uma etapa que espera a anterior não apaga o fluxo gravado")
    void alocarNaoPerdeAInformacaoDeEspera() {
        EtapaOrdemServico segunda = etapa(2);
        segunda.alocar(recursoApto(idioma(), idioma(), traducao),
                Instant.parse("2026-12-01T12:00:00Z"), Instant.parse("2026-11-09T12:00:00Z"));

        // É isto que a US-014 vai consultar para saber quem pode começar.
        assertEquals(StatusEtapaOrdemServico.AGUARDANDO_ETAPA_ANTERIOR, segunda.getStatus());
        assertEquals(SituacaoEtapa.ALOCADO, segunda.situacao());
    }
}
