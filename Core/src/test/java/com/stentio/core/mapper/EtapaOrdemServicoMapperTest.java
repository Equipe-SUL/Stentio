package com.stentio.core.mapper;

import com.stentio.core.dto.EtapaOrdemServicoResponse;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.model.TipoServico;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EtapaOrdemServicoMapperTest {

    @Test
    void incluiDataEArquivoDaEntregaNaResposta() {
        UUID etapaId = UUID.randomUUID();
        UUID tipoServicoId = UUID.randomUUID();
        Instant entregueEm = Instant.parse("2026-11-21T12:00:00Z");
        TipoServico tipoServico = mock(TipoServico.class);
        when(tipoServico.getId()).thenReturn(tipoServicoId);
        when(tipoServico.getNome()).thenReturn("Revisão");

        EtapaOrdemServico etapa = mock(EtapaOrdemServico.class);
        when(etapa.getId()).thenReturn(etapaId);
        when(etapa.getOrdem()).thenReturn(2);
        when(etapa.getNome()).thenReturn("Revisão final");
        when(etapa.getTipoServico()).thenReturn(tipoServico);
        when(etapa.getStatus()).thenReturn(StatusEtapaOrdemServico.ENTREGUE);
        when(etapa.getPrazoEstimadoHoras()).thenReturn(12);
        when(etapa.getEntregueEm()).thenReturn(entregueEm);
        when(etapa.getArquivoEntregueNome()).thenReturn("revisao-final.docx");

        EtapaOrdemServicoResponse resposta = new EtapaOrdemServicoMapper().paraEtapa(etapa);

        assertEquals(entregueEm, resposta.entregueEm());
        assertEquals("revisao-final.docx", resposta.arquivoNome());
    }
}
