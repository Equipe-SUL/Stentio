package com.stentio.core.service;

import com.stentio.core.dto.SolicitacaoRequest;
import com.stentio.core.exception.ReferenciaInvalidaException;
import com.stentio.core.exception.SolicitacaoInvalidaException;
import com.stentio.core.mapper.SolicitacaoMapper;
import com.stentio.core.model.ArquivoArmazenado;
import com.stentio.core.model.CategoriaProjeto;
import com.stentio.core.model.Cliente;
import com.stentio.core.model.Idioma;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.model.StatusSolicitacao;
import com.stentio.core.repository.CategoriaProjetoRepository;
import com.stentio.core.repository.ClienteRepository;
import com.stentio.core.repository.IdiomaRepository;
import com.stentio.core.repository.SolicitacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitacaoServiceTest {

    @Mock
    private SolicitacaoRepository solicitacaoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private CategoriaProjetoRepository categoriaProjetoRepository;
    @Mock
    private IdiomaRepository idiomaRepository;
    @Mock
    private ArquivoStorageService arquivoStorageService;
    @Mock
    private SolicitacaoMapper solicitacaoMapper;

    @InjectMocks
    private SolicitacaoService solicitacaoService;

    private final UUID clienteId = UUID.randomUUID();
    private final UUID categoriaId = UUID.randomUUID();
    private final UUID idiomaOrigemId = UUID.randomUUID();
    private final UUID idiomaDestinoId = UUID.randomUUID();

    private Cliente cliente;
    private CategoriaProjeto categoria;
    private Idioma ingles;
    private Idioma portugues;

    @BeforeEach
    void prepararVinculos() {
        cliente = new Cliente("Stentio Tecnologia", "Wesley Xavier", "wesley@stentio.com", "11987654321", "52998224725");
        categoria = new CategoriaProjeto();
        categoria.setId(categoriaId);
        categoria.setNome("Jurídico");

        ingles = new Idioma();
        ingles.setId(idiomaOrigemId);
        ingles.setNome("Inglês");
        ingles.setCodigoIso("en");

        portugues = new Idioma();
        portugues.setId(idiomaDestinoId);
        portugues.setNome("Português");
        portugues.setCodigoIso("pt-BR");

        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(categoriaProjetoRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(idiomaRepository.findById(idiomaOrigemId)).thenReturn(Optional.of(ingles));
        when(idiomaRepository.findById(idiomaDestinoId)).thenReturn(Optional.of(portugues));
    }

    private SolicitacaoRequest requestValido() {
        return new SolicitacaoRequest(clienteId, categoriaId, idiomaOrigemId, idiomaDestinoId, "Contrato por e-mail");
    }

    @Test
    @DisplayName("Cenário 1: deve persistir a solicitação com status PENDENTE_ORCAMENTO e o arquivo anexado")
    void deveCriarSolicitacaoComArquivo() {
        ArquivoArmazenado armazenado =
                new ArquivoArmazenado("documento.pdf", "2026/10/abc.pdf", "application/pdf", 8L);
        when(arquivoStorageService.salvarTodos(anyList())).thenReturn(List.of(armazenado));
        when(solicitacaoRepository.saveAndFlush(any(Solicitacao.class)))
                .thenAnswer(chamada -> chamada.getArgument(0));

        solicitacaoService.criar(requestValido(), List.of());

        ArgumentCaptor<Solicitacao> capturada = ArgumentCaptor.forClass(Solicitacao.class);
        verify(solicitacaoRepository).saveAndFlush(capturada.capture());

        Solicitacao solicitacao = capturada.getValue();
        // CA.4: o status é definido pelo backend.
        assertEquals(StatusSolicitacao.PENDENTE_ORCAMENTO, solicitacao.getStatus());
        assertEquals(cliente, solicitacao.getCliente());
        assertEquals(categoria, solicitacao.getCategoriaProjeto());
        // CA.3: o caminho gravado no banco é o do storage, nunca o nome enviado pelo usuário.
        assertEquals(1, solicitacao.getArquivos().size());
        assertEquals("documento.pdf", solicitacao.getArquivos().getFirst().getNomeOriginal());
        assertEquals("2026/10/abc.pdf", solicitacao.getArquivos().getFirst().getCaminho());
    }

    @Test
    @DisplayName("Cenário 2: deve recusar idiomas de origem e destino iguais sem gravar arquivo")
    void deveRecusarIdiomasIguais() {
        SolicitacaoRequest request =
                new SolicitacaoRequest(clienteId, categoriaId, idiomaOrigemId, idiomaOrigemId, null);

        SolicitacaoInvalidaException erro = assertThrows(SolicitacaoInvalidaException.class,
                () -> solicitacaoService.criar(request, List.of()));

        assertEquals("Os idiomas de origem e destino não devem ser iguais", erro.getMessage());
        // A validação acontece antes do disco: nada é gravado nem persistido.
        verify(arquivoStorageService, never()).salvarTodos(anyList());
        verify(solicitacaoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("RN.1: deve recusar cliente inexistente informando qual vínculo falhou")
    void deveRecusarClienteInexistente() {
        UUID inexistente = UUID.randomUUID();
        when(clienteRepository.findById(inexistente)).thenReturn(Optional.empty());

        SolicitacaoRequest request =
                new SolicitacaoRequest(inexistente, categoriaId, idiomaOrigemId, idiomaDestinoId, null);

        ReferenciaInvalidaException erro = assertThrows(ReferenciaInvalidaException.class,
                () -> solicitacaoService.criar(request, List.of()));

        assertTrue(erro.getMessage().startsWith("Cliente"), "mensagem deve dizer qual vínculo falhou");
        verify(solicitacaoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Deve apagar os arquivos já gravados quando a persistência falha")
    void deveRemoverArquivosQuandoPersistenciaFalha() {
        ArquivoArmazenado armazenado =
                new ArquivoArmazenado("documento.pdf", "2026/10/abc.pdf", "application/pdf", 8L);
        when(arquivoStorageService.salvarTodos(anyList())).thenReturn(List.of(armazenado));
        when(solicitacaoRepository.saveAndFlush(any(Solicitacao.class)))
                .thenThrow(new IllegalStateException("falha no banco"));

        assertThrows(IllegalStateException.class, () -> solicitacaoService.criar(requestValido(), List.of()));

        verify(arquivoStorageService).remover(List.of("2026/10/abc.pdf"));
    }
}
