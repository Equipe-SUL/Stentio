package com.stentio.core.mapper;

import com.stentio.core.model.Cliente;
import com.stentio.core.model.Orcamento;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.StatusOrdemServico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrdemServicoMapperTest {

    private final OrdemServicoMapper mapper = new OrdemServicoMapper();

    @Test
    @DisplayName("Deve formatar o código visual como OS-<ano>-<id> e variar por id")
    void deveFormatarCodigoVisual() {
        UUID id = UUID.fromString("a1b2c3d4-0000-0000-0000-000000000000");
        Instant criadoEm = Instant.parse("2026-10-09T08:00:00Z");

        assertEquals("OS-2026-A1B2C3", OrdemServicoMapper.codigoVisual(criadoEm, id));
        // O sufixo sai do id, então dois registros do mesmo ano nunca colidem.
        assertEquals("OS-2026-D4C3B2",
                OrdemServicoMapper.codigoVisual(criadoEm, UUID.fromString("d4c3b2a1-0000-0000-0000-000000000000")));
    }

    @Test
    @DisplayName("Deve usar o ano em UTC, para o código não mudar conforme o fuso")
    void deveUsarAnoUtc() {
        Instant naVirada = Instant.parse("2025-12-31T23:30:00Z");

        assertEquals("OS-2025-000000",
                OrdemServicoMapper.codigoVisual(naVirada, UUID.fromString("00000000-0000-0000-0000-000000000000")));
    }

    @Test
    @DisplayName("paraResumo deve preencher o codigo_visual a partir da entidade")
    void paraResumoPreencheCodigoVisual() {
        OrdemServico ordem = mock(OrdemServico.class);
        Orcamento orcamento = mock(Orcamento.class);
        Cliente cliente = mock(Cliente.class);

        when(ordem.getOrcamento()).thenReturn(orcamento);
        when(orcamento.getCliente()).thenReturn(cliente);
        when(ordem.getId()).thenReturn(UUID.fromString("a1b2c3d4-0000-0000-0000-000000000000"));
        when(ordem.getCriadoEm()).thenReturn(Instant.parse("2026-10-09T08:00:00Z"));
        when(ordem.getStatus()).thenReturn(StatusOrdemServico.PENDENTE_ALOCACAO);
        when(cliente.getId()).thenReturn(UUID.randomUUID());
        when(cliente.getNomeEmpresa()).thenReturn("Empresa XPTO");

        assertEquals("OS-2026-A1B2C3", mapper.paraResumo(ordem).codigoVisual());
    }
}
