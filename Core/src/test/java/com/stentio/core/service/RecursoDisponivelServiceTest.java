package com.stentio.core.service;

import com.stentio.core.dto.RecursoDisponivelResponse;
import com.stentio.core.exception.AlocacaoInvalidaException;
import com.stentio.core.mapper.RecursoMapper;
import com.stentio.core.model.Idioma;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.TipoServico;
import com.stentio.core.model.UnidadeCobranca;
import com.stentio.core.repository.IdiomaRepository;
import com.stentio.core.repository.RecursoRepository;
import com.stentio.core.repository.TipoServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// US-012 CA.2: quem a tela de alocação pode oferecer ao gestor.
class RecursoDisponivelServiceTest {

    private final RecursoRepository recursos = mock(RecursoRepository.class);
    private final IdiomaRepository idiomas = mock(IdiomaRepository.class);
    private final TipoServicoRepository tipos = mock(TipoServicoRepository.class);

    private final RecursoService service = new RecursoService(
            recursos, idiomas, tipos, new RecursoMapper());

    private final UUID portuguesId = UUID.randomUUID();
    private final UUID inglesId = UUID.randomUUID();
    private final UUID alemaoId = UUID.randomUUID();
    private final UUID traducaoId = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        when(idiomas.existsById(any())).thenReturn(true);
        when(tipos.existsById(any())).thenReturn(true);
    }

    private static Idioma idioma(UUID id) {
        Idioma i = mock(Idioma.class);
        when(i.getId()).thenReturn(id);
        return i;
    }

    private static TipoServico tipo(UUID id) {
        TipoServico t = new TipoServico();
        t.setId(id);
        t.setNome("Tradução");
        t.setAtivo(true);
        return t;
    }

    @Test
    @DisplayName("Devolve só os preços do par consultado, não a tabela inteira do profissional")
    void filtraOsPrecosPeloPar() {
        Recurso ana = new Recurso("Ana Silva", "ana@traducoes.com", null);
        ana.definirTiposServico(List.of(tipo(traducaoId)));
        ana.definirPrecos(List.of(
                new Recurso.NovoPreco(idioma(portuguesId), idioma(inglesId),
                        UnidadeCobranca.PALAVRA, new BigDecimal("0.35")),
                new Recurso.NovoPreco(idioma(portuguesId), idioma(inglesId),
                        UnidadeCobranca.HORA, new BigDecimal("90.00")),
                new Recurso.NovoPreco(idioma(portuguesId), idioma(alemaoId),
                        UnidadeCobranca.PALAVRA, new BigDecimal("0.50"))));

        when(recursos.buscarDisponiveis(portuguesId, inglesId, traducaoId)).thenReturn(List.of(ana));

        List<RecursoDisponivelResponse> disponiveis =
                service.listarDisponiveis(portuguesId, inglesId, traducaoId);

        assertEquals(1, disponiveis.size());
        assertEquals("Ana Silva", disponiveis.getFirst().nome());

        // Os dois do par Português → Inglês entram; o de Alemão fica de fora.
        List<RecursoDisponivelResponse.PrecoDoPar> precos = disponiveis.getFirst().precosDoPar();
        assertEquals(List.of(UnidadeCobranca.PALAVRA, UnidadeCobranca.HORA),
                precos.stream().map(RecursoDisponivelResponse.PrecoDoPar::unidade).toList());
        assertEquals(0, new BigDecimal("0.35").compareTo(precos.getFirst().valor()));
    }

    @Test
    @DisplayName("Ninguém habilitado devolve lista vazia, não erro")
    void semNinguemHabilitadoDevolveVazio() {
        when(recursos.buscarDisponiveis(any(), any(), any())).thenReturn(List.of());

        assertTrue(service.listarDisponiveis(portuguesId, inglesId, traducaoId).isEmpty());
    }

    @Test
    @DisplayName("Idiomas iguais são recusados antes de consultar o banco")
    void idiomasIguaisSaoRecusados() {
        assertThrows(AlocacaoInvalidaException.class,
                () -> service.listarDisponiveis(portuguesId, portuguesId, traducaoId));

        verify(recursos, never()).buscarDisponiveis(any(), any(), any());
    }

    @Test
    @DisplayName("Idioma inexistente responde erro nomeando o campo, em vez de lista vazia")
    void idiomaInexistenteNaoVoltaComoListaVazia() {
        when(idiomas.existsById(inglesId)).thenReturn(false);

        var erro = assertThrows(AlocacaoInvalidaException.class,
                () -> service.listarDisponiveis(portuguesId, inglesId, traducaoId));

        assertTrue(erro.getMessage().startsWith("Idioma de destino"));
        verify(recursos, never()).buscarDisponiveis(any(), any(), any());
    }

    @Test
    @DisplayName("Tipo de serviço inexistente responde erro nomeando o campo")
    void tipoInexistenteNaoVoltaComoListaVazia() {
        when(tipos.existsById(traducaoId)).thenReturn(false);

        var erro = assertThrows(AlocacaoInvalidaException.class,
                () -> service.listarDisponiveis(portuguesId, inglesId, traducaoId));

        assertTrue(erro.getMessage().startsWith("Tipo de serviço"));
    }
}
