package com.stentio.core.service;

import com.stentio.core.dto.RecursoRequest;
import com.stentio.core.dto.RecursoResponse;
import com.stentio.core.exception.AutoCadastroInvalidoException;
import com.stentio.core.exception.EmailJaCadastradoException;
import com.stentio.core.mapper.RecursoMapper;
import com.stentio.core.model.Idioma;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.TipoServico;
import com.stentio.core.model.UnidadeCobranca;
import com.stentio.core.repository.IdiomaRepository;
import com.stentio.core.repository.RecursoRepository;
import com.stentio.core.repository.TipoServicoRepository;
import com.stentio.core.support.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// US-023 CA.2, CA.3, RN.1 e RN.3: o profissional criando o próprio perfil.
class RecursoAutoCadastroServiceTest {

    private final RecursoRepository recursos = mock(RecursoRepository.class);
    private final IdiomaRepository idiomas = mock(IdiomaRepository.class);
    private final TipoServicoRepository tipos = mock(TipoServicoRepository.class);

    private final RecursoService service = new RecursoService(recursos, idiomas, tipos, new RecursoMapper());

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID traducaoId = UUID.randomUUID();
    private final UUID portuguesId = UUID.randomUUID();
    private final UUID inglesId = UUID.randomUUID();

    private final UsuarioAutenticado maria =
            new UsuarioAutenticado("maria@exemplo.com", usuarioId, false);

    private RecursoRequest pedido(String email) {
        return new RecursoRequest("Maria Tradutora", email, "+55 11 99999-0000",
                List.of(traducaoId),
                List.of(new RecursoRequest.PrecoRequest(
                        portuguesId, inglesId, UnidadeCobranca.PALAVRA, new BigDecimal("0.25"))));
    }

    @BeforeEach
    void preparar() {
        TipoServico traducao = new TipoServico();
        traducao.setId(traducaoId);
        traducao.setNome("Tradução");
        traducao.setAtivo(true);
        when(tipos.findAllById(any())).thenReturn(List.of(traducao));

        Idioma portugues = mock(Idioma.class);
        when(portugues.getId()).thenReturn(portuguesId);
        Idioma ingles = mock(Idioma.class);
        when(ingles.getId()).thenReturn(inglesId);
        when(idiomas.findAllById(any())).thenReturn(List.of(portugues, ingles));

        when(recursos.existsByUsuarioId(any())).thenReturn(false);
        when(recursos.existsByEmail(anyString())).thenReturn(false);
        when(recursos.saveAndFlush(any(Recurso.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Recurso capturarSalvo() {
        ArgumentCaptor<Recurso> captor = ArgumentCaptor.forClass(Recurso.class);
        verify(recursos).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Cenário 1: o perfil nasce vinculado à conta, ativo e não verificado")
    void nasceVinculadoENaoVerificado() {
        RecursoResponse resposta = service.autoCadastrar(pedido("maria@exemplo.com"), maria);

        assertTrue(resposta.ativo());
        assertFalse(resposta.verificado(), "CA.3: verificação é do gestor, não do cadastro");

        Recurso salvo = capturarSalvo();
        assertEquals(usuarioId, salvo.getUsuarioId());
        assertFalse(salvo.isVerificado());
        assertEquals(1, salvo.getTiposServico().size());
        assertEquals(1, salvo.getPrecos().size());
    }

    @Test
    @DisplayName("RN.1: o perfil tem de usar o e-mail da própria conta")
    void recusaEmailDiferenteDaConta() {
        var erro = assertThrows(AutoCadastroInvalidoException.class,
                () -> service.autoCadastrar(pedido("outra.pessoa@exemplo.com"), maria));

        assertTrue(erro.getMessage().contains("maria@exemplo.com"));
        verify(recursos, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Maiúsculas e espaços no e-mail não contam como e-mail diferente")
    void comparacaoDeEmailIgnoraFormatacao() {
        assertDoesNotThrow(() -> service.autoCadastrar(pedido("  Maria@Exemplo.com "), maria));
    }

    @Test
    @DisplayName("RN.3: sessão sem userId pede login novo em vez de criar perfil órfão")
    void recusaTokenAntigo() {
        UsuarioAutenticado tokenAntigo = new UsuarioAutenticado("maria@exemplo.com", false);

        var erro = assertThrows(AutoCadastroInvalidoException.class,
                () -> service.autoCadastrar(pedido("maria@exemplo.com"), tokenAntigo));

        assertTrue(erro.getMessage().contains("Entre novamente"));
        verify(recursos, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Uma conta tem um perfil só")
    void recusaSegundoPerfilDaMesmaConta() {
        when(recursos.existsByUsuarioId(usuarioId)).thenReturn(true);

        assertThrows(AutoCadastroInvalidoException.class,
                () -> service.autoCadastrar(pedido("maria@exemplo.com"), maria));
    }

    @Test
    @DisplayName("E-mail já usado por um recurso cadastrado pela equipe é recusado")
    void recusaEmailJaCadastrado() {
        when(recursos.existsByEmail("maria@exemplo.com")).thenReturn(true);

        assertThrows(EmailJaCadastradoException.class,
                () -> service.autoCadastrar(pedido("maria@exemplo.com"), maria));
    }
}
