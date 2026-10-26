package com.stentio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.stentio.model.Role;
import com.stentio.model.Usuario;
import com.stentio.repository.UsuarioRepository;

@Component
public class DadosAdminInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin.enabled:false}")
    private boolean enabled;

    @Value("${app.seed.admin.email:}")
    private String email;

    @Value("${app.seed.admin.password:}")
    private String senha;

    public DadosAdminInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!enabled || email.isBlank() || senha.isBlank()) {
            return;
        }
        if (usuarioRepository.existsByEmail(email)) {
            return;
        }

        Usuario admin = new Usuario();
        admin.setNome("Administrador");
        admin.setEmail(email);
        admin.setSenha(passwordEncoder.encode(senha));
        admin.setRole(Role.ADMIN);
        admin.setAtivo(true);

        usuarioRepository.save(admin);
    }
}