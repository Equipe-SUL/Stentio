package com.stentio.service;

import com.stentio.dto.AutoCadastroRequestDTO;
import com.stentio.dto.UsuarioRequestDTO;
import com.stentio.dto.UsuarioResponseDTO;
import com.stentio.exception.EmailJaCadastradoException;
import com.stentio.model.Role;
import com.stentio.model.Usuario;
import com.stentio.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * US-023 CA.1 e RN.1: o cadastro público.
 *
 * <p>O teste que mais importa aqui é o do papel. Enquanto existir um {@code criar()} que
 * aceita role do corpo logo ao lado, é fácil alguém "simplificar" os dois em um método só
 * — e aí a rota aberta passa a criar administradores.
 */
class UsuarioServiceAutoCadastroTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final TokenService tokenService = mock(TokenService.class);

    private final UsuarioService service = new UsuarioService(usuarios, encoder, tokenService);

    private final AutoCadastroRequestDTO pedido =
            new AutoCadastroRequestDTO("Maria Tradutora", "maria@exemplo.com", "Senha@123");

    private Usuario capturarSalvo() {
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        return captor.getValue();
    }

    private void repositorioAceita() {
        when(usuarios.existsByEmail(anyString())).thenReturn(false);
        when(encoder.encode(anyString())).thenReturn("hash-da-senha");
        when(usuarios.save(any(Usuario.class))).thenAnswer(invocacao -> {
            Usuario salvo = invocacao.getArgument(0);
            salvo.setId(UUID.randomUUID());
            return salvo;
        });
    }

    @Test
    @DisplayName("CA.1: a conta nasce com papel RECURSO e ativa")
    void nasceComoRecurso() {
        repositorioAceita();

        UsuarioResponseDTO resposta = service.autoCadastrarRecurso(pedido);

        assertEquals(Role.RECURSO, resposta.role());
        assertTrue(resposta.ativo());
        assertEquals("maria@exemplo.com", resposta.email());
    }

    @Test
    @DisplayName("Rota pública não aceita escolher o papel: comparado ao cadastro interno")
    void papelNaoVemDaEntrada() {
        repositorioAceita();

        // O cadastro interno, feito por um ADMIN, pode escolher o papel. É legítimo.
        service.criar(new UsuarioRequestDTO("Chefe", "chefe@exemplo.com", "Senha@123", Role.ADMIN));
        assertEquals(Role.ADMIN, capturarSalvo().getRole());

        // O público não tem onde pedir papel nenhum: o DTO não tem o campo, e mesmo que o
        // corpo traga "role", ele não chega a lugar algum.
        reset(usuarios);
        repositorioAceita();
        service.autoCadastrarRecurso(pedido);
        assertEquals(Role.RECURSO, capturarSalvo().getRole());
    }

    @Test
    @DisplayName("RN.1: e-mail já cadastrado é recusado, inclusive se for de alguém da equipe")
    void emailDuplicadoERecusado() {
        when(usuarios.existsByEmail("maria@exemplo.com")).thenReturn(true);

        assertThrows(EmailJaCadastradoException.class, () -> service.autoCadastrarRecurso(pedido));

        verify(usuarios, never()).save(any());
    }

    @Test
    @DisplayName("A senha é gravada codificada, nunca como veio")
    void senhaNaoEGravadaEmTextoPuro() {
        repositorioAceita();

        service.autoCadastrarRecurso(pedido);

        verify(encoder).encode("Senha@123");
        assertEquals("hash-da-senha", capturarSalvo().getSenha());
    }
}
