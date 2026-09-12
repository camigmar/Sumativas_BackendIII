package com.banco.security.config;

import com.banco.security.model.Usuario;
import com.banco.security.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final String PASSWORD_PRUEBA = "Clave123!";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() == 0) {
            usuarioRepository.save(crearUsuario("cliente.web", "ROLE_WEB"));
            usuarioRepository.save(crearUsuario("cliente.movil", "ROLE_MOVIL"));
            usuarioRepository.save(crearUsuario("cajero.central", "ROLE_CAJERO"));
        }
    }

    private Usuario crearUsuario(String username, String rol) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(PASSWORD_PRUEBA));
        usuario.setRol(rol);
        return usuario;
    }
}
