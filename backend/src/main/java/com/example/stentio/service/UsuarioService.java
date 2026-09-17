package com.example.stentio.service;

import com.example.stentio.dto.LoginRequestDTO;
import com.example.stentio.dto.LoginResponseDTO;
import com.example.stentio.dto.UsuarioRequestDTO;
import com.example.stentio.dto.UsuarioResponseDTO;
import com.example.stentio.exception.CredenciaisLoginException;
import com.example.stentio.exception.EmailJaCadastradoException;
import com.example.stentio.exception.UsuarioNaoEncontradoException;
import com.example.stentio.model.Role;
import com.example.stentio.model.Usuario;
import com.example.stentio.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public LoginResponseDTO login(LoginRequestDTO dadosLogin) {
        Usuario usuario = usuarioRepository.findByEmail(dadosLogin.email())
                .orElseThrow(CredenciaisLoginException::new);

        if (!passwordEncoder.matches(dadosLogin.senha(), usuario.getSenha())) {
            throw new CredenciaisLoginException();
        }

        String token = tokenService.gerarToken(usuario);
        return new LoginResponseDTO(token, "Bearer", TokenService.EXPIRES_IN, usuario.getEmail(), usuario.getRole());
    }

    public UsuarioResponseDTO criar(UsuarioRequestDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new EmailJaCadastradoException(dto.getEmail());
        }

        Usuario usuario = new Usuario();
        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setRole(dto.getRole() != null ? dto.getRole() : Role.ATENDENTE);
        usuario.setAtivo(true);

        Usuario salvo = usuarioRepository.save(usuario);
        return new UsuarioResponseDTO(salvo);
    }

    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll()
                .stream()
                .map(UsuarioResponseDTO::new)
                .collect(Collectors.toList());
    }

    public UsuarioResponseDTO buscarPorId(UUID id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
        return new UsuarioResponseDTO(usuario);
    }

    public UsuarioResponseDTO buscarPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(email));
        return new UsuarioResponseDTO(usuario);
    }

    public UsuarioResponseDTO editar(UUID id, UsuarioRequestDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));

        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        if (dto.getRole() != null) {
            usuario.setRole(dto.getRole());
        }

        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        Usuario atualizado = usuarioRepository.save(usuario);
        return new UsuarioResponseDTO(atualizado);
    }

    public UsuarioResponseDTO ativarOuDesativar(UUID id, boolean ativo) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));

        usuario.setAtivo(ativo);
        Usuario atualizado = usuarioRepository.save(usuario);
        return new UsuarioResponseDTO(atualizado);
    }

    public void excluir(UUID id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));

        if (usuario.isAtivo()) {
            throw new IllegalStateException("Não é possível excluir um usuário ativo. Desative-o primeiro.");
        }

        usuarioRepository.deleteById(id);
    }
}