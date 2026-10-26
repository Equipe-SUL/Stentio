package com.stentio.core.service;

import com.stentio.core.dto.OrcamentoCriacaoRequest;
import com.stentio.core.dto.OrcamentoDetalheResponse;
import com.stentio.core.dto.OrcamentoItensRequest;
import com.stentio.core.dto.OrcamentoPaginaResponse;
import com.stentio.core.exception.OrcamentoInvalidoException;
import com.stentio.core.exception.OrcamentoNaoEditavelException;
import com.stentio.core.exception.OrcamentoNaoEncontradoException;
import com.stentio.core.exception.ReferenciaInvalidaException;
import com.stentio.core.exception.SolicitacaoNaoEncontradaException;
import com.stentio.core.exception.SolicitacaoInvalidaException;
import com.stentio.core.model.CategoriaProjeto;
import com.stentio.core.model.Cliente;
import com.stentio.core.model.Idioma;
import com.stentio.core.model.Orcamento;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.model.StatusOrcamento;
import com.stentio.core.model.StatusSolicitacao;
import com.stentio.core.model.UnidadeCobranca;
import com.stentio.core.repository.ClienteRepository;
import com.stentio.core.repository.IdiomaRepository;
import com.stentio.core.repository.OrcamentoRepository;
import com.stentio.core.repository.SolicitacaoRepository;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrcamentoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-20T10:00:00Z");

    @Mock
    private OrcamentoRepository orcamentos;
    @Mock
    private SolicitacaoRepository solicitacoes;
    @Mock
    private ClienteRepository clientes;
    @Mock
    private IdiomaRepository idiomas;

    private final CalculoOrcamentoService calculo = new CalculoOrcamentoService();
    private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);

    private OrcamentoService service;
    private ValidatorFactory validatorFactory;

    private final UUID solicitacaoId = UUID.randomUUID();
    private final UUID clienteId = UUID.randomUUID();
    private final UUID idiomaOrigemId = UUID.randomUUID();
    private final UUID idiomaDestinoId = UUID.randomUUID();

    private Cliente cliente;
    private Solicitacao solicitacao;

    @BeforeEach
    void setup() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        service = new OrcamentoService(orcamentos, solicitacoes, clientes, idiomas, calculo, clock,
                validatorFactory.getValidator());

        cliente = mock(Cliente.class);
        when(cliente.getId()).thenReturn(clienteId);

        solicitacao = spy(new Solicitacao(cliente, new CategoriaProjeto(), new Idioma(), new Idioma(), "Teste"));
        when(solicitacao.getId()).thenReturn(solicitacaoId);
        when(solicitacao.getCliente()).thenReturn(cliente);
    }

    @AfterEach
    void fecharValidator() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("CA.1 e RN.1: Deve criar orçamento com EM_ELABORACAO, versao 1 e valor total zero")
    void deveCriarOrcamentoComSucesso() {
        Instant expiracao = AGORA.plusSeconds(86400);
        OrcamentoCriacaoRequest request = new OrcamentoCriacaoRequest(solicitacaoId, clienteId, expiracao);

        when(solicitacoes.buscarPorIdComLock(solicitacaoId)).thenReturn(Optional.of(solicitacao));
        when(clientes.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(orcamentos.save(any(Orcamento.class))).thenAnswer(inv -> {
            Orcamento o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        OrcamentoDetalheResponse resposta = service.criar(request);

        assertNotNull(resposta.id());
        assertEquals(StatusOrcamento.EM_ELABORACAO, resposta.status());
        assertEquals(1, resposta.versao());
        assertEquals(new BigDecimal("0.00"), resposta.valorTotal());
        assertEquals(clienteId, resposta.clienteId());
        assertEquals(solicitacaoId, resposta.solicitacaoId());
        assertTrue(resposta.itens().isEmpty());
        assertEquals(StatusSolicitacao.AGUARDANDO_APROVACAO, solicitacao.getStatus());
    }

    @Test
    void naoCriaOutroOrcamentoParaSolicitacaoAntigaAindaPendente() {
        when(solicitacoes.buscarPorIdComLock(solicitacaoId)).thenReturn(Optional.of(solicitacao));
        when(clientes.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(orcamentos.existsBySolicitacaoId(solicitacaoId)).thenReturn(true);

        assertThrows(OrcamentoInvalidoException.class, () -> service.criar(
                new OrcamentoCriacaoRequest(solicitacaoId, clienteId, AGORA.plusSeconds(3600))));

        assertEquals(StatusSolicitacao.PENDENTE_ORCAMENTO, solicitacao.getStatus());
        verify(orcamentos, never()).save(any());
    }

    @Test
    void naoCriaOrcamentoParaSolicitacaoForaDePendente() {
        when(solicitacoes.buscarPorIdComLock(solicitacaoId)).thenReturn(Optional.of(solicitacao));
        when(clientes.findById(clienteId)).thenReturn(Optional.of(cliente));
        for (StatusSolicitacao status : List.of(StatusSolicitacao.AGUARDANDO_APROVACAO,
                StatusSolicitacao.ORCAMENTO_REJEITADO, StatusSolicitacao.EM_EXECUCAO)) {
            org.springframework.test.util.ReflectionTestUtils.setField(solicitacao, "status", status);
            assertThrows(SolicitacaoInvalidaException.class, () -> service.criar(
                    new OrcamentoCriacaoRequest(solicitacaoId, clienteId, AGORA.plusSeconds(3600))));
        }
        verify(orcamentos, never()).save(any());
    }

    @Test
    void rejeitaValoresAusentesOuNaoPositivosAntesDeAlterarItens() {
        for (BigDecimal valor : new BigDecimal[]{null, BigDecimal.ZERO, BigDecimal.ONE.negate()}) {
            for (boolean quantidadeInvalida : new boolean[]{false, true}) {
                var servico = new OrcamentoItensRequest.Servico("Tradução",
                        quantidadeInvalida ? valor : BigDecimal.ONE, UnidadeCobranca.PALAVRA,
                        quantidadeInvalida ? BigDecimal.ONE : valor);
                var request = new OrcamentoItensRequest(List.of(new OrcamentoItensRequest.Item(
                        1, idiomaOrigemId, idiomaDestinoId, List.of(servico))));

                assertThrows(OrcamentoInvalidoException.class,
                        () -> service.substituirItens(UUID.randomUUID(), request));
            }
        }
        assertThrows(OrcamentoInvalidoException.class,
                () -> service.substituirItens(UUID.randomUUID(), new OrcamentoItensRequest(List.of())));
        var semServicos = new OrcamentoItensRequest(List.of(new OrcamentoItensRequest.Item(
                1, idiomaOrigemId, idiomaDestinoId, List.of())));
        assertThrows(OrcamentoInvalidoException.class,
                () -> service.substituirItens(UUID.randomUUID(), semServicos));
        verifyNoInteractions(orcamentos, idiomas);
    }

    @Test
    @DisplayName("RN.5: Deve rejeitar criação com data de expiração no passado ou presente")
    void deveRejeitarDataExpiracaoNoPassado() {
        OrcamentoCriacaoRequest request = new OrcamentoCriacaoRequest(solicitacaoId, clienteId, AGORA.minusSeconds(10));

        OrcamentoInvalidoException ex = assertThrows(OrcamentoInvalidoException.class, () -> service.criar(request));
        assertEquals(OrcamentoService.MENSAGEM_EXPIRACAO, ex.getMessage());
        verify(orcamentos, never()).save(any());
    }

    @Test
    @DisplayName("RN.1: Deve lançar 404 quando solicitação não existir")
    void deveFalharQuandoSolicitacaoNaoExistir() {
        OrcamentoCriacaoRequest request = new OrcamentoCriacaoRequest(solicitacaoId, clienteId, AGORA.plusSeconds(3600));
        when(solicitacoes.buscarPorIdComLock(solicitacaoId)).thenReturn(Optional.empty());

        assertThrows(SolicitacaoNaoEncontradaException.class, () -> service.criar(request));
    }

    @Test
    @DisplayName("RN.1: Deve lançar ReferenciaInvalidaException quando cliente não existir")
    void deveFalharQuandoClienteNaoExistir() {
        OrcamentoCriacaoRequest request = new OrcamentoCriacaoRequest(solicitacaoId, clienteId, AGORA.plusSeconds(3600));
        when(solicitacoes.buscarPorIdComLock(solicitacaoId)).thenReturn(Optional.of(solicitacao));
        when(clientes.findById(clienteId)).thenReturn(Optional.empty());

        assertThrows(ReferenciaInvalidaException.class, () -> service.criar(request));
    }

    @Test
    @DisplayName("RN.1: Deve falhar se a solicitação pertencer a outro cliente")
    void deveFalharSeSolicitacaoPertencerAOutroCliente() {
        Cliente outroCliente = mock(Cliente.class);
        when(outroCliente.getId()).thenReturn(UUID.randomUUID());
        when(solicitacao.getCliente()).thenReturn(outroCliente);

        OrcamentoCriacaoRequest request = new OrcamentoCriacaoRequest(solicitacaoId, clienteId, AGORA.plusSeconds(3600));
        when(solicitacoes.buscarPorIdComLock(solicitacaoId)).thenReturn(Optional.of(solicitacao));
        when(clientes.findById(clienteId)).thenReturn(Optional.of(cliente));

        OrcamentoInvalidoException ex = assertThrows(OrcamentoInvalidoException.class, () -> service.criar(request));
        assertEquals(OrcamentoService.MENSAGEM_SOLICITACAO_DE_OUTRO_CLIENTE, ex.getMessage());
    }

    @Test
    @DisplayName("CA.2 / RN.2: Deve rejeitar itens com idioma de origem igual ao de destino")
    void deveRejeitarItensComIdiomasIguais() {
        UUID orcamentoId = UUID.randomUUID();
        OrcamentoItensRequest.Servico servico = new OrcamentoItensRequest.Servico(
                "TRADUCAO", new BigDecimal("1000"), UnidadeCobranca.PALAVRA, new BigDecimal("0.20"));
        OrcamentoItensRequest.Item item = new OrcamentoItensRequest.Item(
                1, idiomaOrigemId, idiomaOrigemId, List.of(servico));
        OrcamentoItensRequest request = new OrcamentoItensRequest(List.of(item));

        OrcamentoInvalidoException ex = assertThrows(OrcamentoInvalidoException.class,
                () -> service.substituirItens(orcamentoId, request));
        assertEquals(OrcamentoService.MENSAGEM_IDIOMAS_IGUAIS, ex.getMessage());
    }

    @Test
    @DisplayName("Deve rejeitar itens com ordem repetida")
    void deveRejeitarOrdemRepetida() {
        UUID orcamentoId = UUID.randomUUID();
        UUID idiomaOutro = UUID.randomUUID();
        OrcamentoItensRequest.Servico s = new OrcamentoItensRequest.Servico(
                "TRADUCAO", new BigDecimal("1000"), UnidadeCobranca.PALAVRA, new BigDecimal("0.20"));
        OrcamentoItensRequest.Item item1 = new OrcamentoItensRequest.Item(1, idiomaOrigemId, idiomaDestinoId, List.of(s));
        OrcamentoItensRequest.Item item2 = new OrcamentoItensRequest.Item(1, idiomaOrigemId, idiomaOutro, List.of(s));
        OrcamentoItensRequest request = new OrcamentoItensRequest(List.of(item1, item2));

        OrcamentoInvalidoException ex = assertThrows(OrcamentoInvalidoException.class,
                () -> service.substituirItens(orcamentoId, request));
        assertTrue(ex.getMessage().contains("repetida"));
    }

    @Test
    @DisplayName("CA.3 e CA.4: Substituição de itens calcula subtotais e total do orçamento corretamente")
    void deveSubstituirItensECalcularTotais() {
        UUID orcamentoId = UUID.randomUUID();
        Orcamento orcamento = new Orcamento();
        orcamento.setId(orcamentoId);
        orcamento.setCliente(cliente);
        orcamento.setSolicitacao(solicitacao);
        orcamento.setStatus(StatusOrcamento.EM_ELABORACAO);
        orcamento.setNumeroVersao(1);

        Idioma idiomaOrigem = new Idioma();
        idiomaOrigem.setId(idiomaOrigemId);
        Idioma idiomaDestino = new Idioma();
        idiomaDestino.setId(idiomaDestinoId);

        when(orcamentos.buscarPorIdComLock(orcamentoId)).thenReturn(Optional.of(orcamento));
        when(idiomas.findAllById(any())).thenReturn(List.of(idiomaOrigem, idiomaDestino));
        when(orcamentos.saveAndFlush(any(Orcamento.class))).thenAnswer(inv -> inv.getArgument(0));

        OrcamentoItensRequest.Servico s1 = new OrcamentoItensRequest.Servico(
                "TRADUCAO", new BigDecimal("1500"), UnidadeCobranca.PALAVRA, new BigDecimal("0.25"));
        OrcamentoItensRequest.Servico s2 = new OrcamentoItensRequest.Servico(
                "REVISAO", new BigDecimal("1500"), UnidadeCobranca.PALAVRA, new BigDecimal("0.10"));
        OrcamentoItensRequest.Item item = new OrcamentoItensRequest.Item(
                1, idiomaOrigemId, idiomaDestinoId, List.of(s1, s2));

        OrcamentoDetalheResponse resposta = service.substituirItens(orcamentoId, new OrcamentoItensRequest(List.of(item)));

        assertEquals(1, resposta.itens().size());
        assertEquals(new BigDecimal("525.00"), resposta.itens().get(0).subtotalItem());
        assertEquals(new BigDecimal("525.00"), resposta.valorTotal());
        assertEquals(new BigDecimal("375.00"), resposta.itens().get(0).servicos().get(0).subtotal());
        assertEquals(new BigDecimal("150.00"), resposta.itens().get(0).servicos().get(1).subtotal());
    }

    @Test
    @DisplayName("Deve impedir alteração de itens em orçamento que não está em elaboração")
    void deveImpedirAlteracaoDeItensForaDeElaboracao() {
        UUID orcamentoId = UUID.randomUUID();
        Orcamento orcamento = new Orcamento();
        orcamento.setStatus(StatusOrcamento.AGUARDANDO_APROVACAO);

        when(orcamentos.buscarPorIdComLock(orcamentoId)).thenReturn(Optional.of(orcamento));

        OrcamentoItensRequest.Servico s = new OrcamentoItensRequest.Servico(
                "TRADUCAO", new BigDecimal("100"), UnidadeCobranca.PALAVRA, new BigDecimal("1.00"));
        OrcamentoItensRequest request = new OrcamentoItensRequest(List.of(
                new OrcamentoItensRequest.Item(1, idiomaOrigemId, idiomaDestinoId, List.of(s))));

        assertThrows(OrcamentoNaoEditavelException.class, () -> service.substituirItens(orcamentoId, request));
    }

    @Test
    @DisplayName("CA.5: Listagem deve retornar página base 1 com filtros")
    void deveListarPaginado() {
        Orcamento orcamento = new Orcamento();
        orcamento.setId(UUID.randomUUID());
        orcamento.setCliente(cliente);
        orcamento.setSolicitacao(solicitacao);
        orcamento.setStatus(StatusOrcamento.EM_ELABORACAO);
        orcamento.setNumeroVersao(1);
        orcamento.setValorTotal(new BigDecimal("525.00"));

        PageImpl<Orcamento> page = new PageImpl<>(List.of(orcamento), Pageable.ofSize(10), 1);
        when(orcamentos.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        OrcamentoPaginaResponse pagina = service.listar(StatusOrcamento.EM_ELABORACAO, clienteId, solicitacaoId, 1, 10);

        assertEquals(1, pagina.pagina());
        assertEquals(10, pagina.tamanhoPagina());
        assertEquals(1, pagina.totalElementos());
        assertEquals(1, pagina.conteudo().size());
        assertEquals(StatusOrcamento.EM_ELABORACAO, pagina.conteudo().get(0).status());
    }

    @Test
    @DisplayName("CA.6: Consulta por ID retorna detalhe completo")
    void deveBuscarPorIdComSucesso() {
        UUID orcamentoId = UUID.randomUUID();
        Orcamento orcamento = new Orcamento();
        orcamento.setId(orcamentoId);
        orcamento.setCliente(cliente);
        orcamento.setSolicitacao(solicitacao);
        orcamento.setStatus(StatusOrcamento.EM_ELABORACAO);
        orcamento.setNumeroVersao(1);
        orcamento.setValorTotal(new BigDecimal("975.00"));

        when(orcamentos.buscarDetalhePorId(orcamentoId)).thenReturn(Optional.of(orcamento));

        OrcamentoDetalheResponse detalhe = service.buscarPorId(orcamentoId);
        assertEquals(orcamentoId, detalhe.id());
        assertEquals(new BigDecimal("975.00"), detalhe.valorTotal());
    }

    @Test
    @DisplayName("Consulta por ID inexistente lança 404")
    void deveLancar404QuandoOrcamentoInexistente() {
        UUID id = UUID.randomUUID();
        when(orcamentos.buscarDetalhePorId(id)).thenReturn(Optional.empty());

        assertThrows(OrcamentoNaoEncontradoException.class, () -> service.buscarPorId(id));
    }
}
