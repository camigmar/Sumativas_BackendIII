package com.banco.core.service;

import com.banco.security.model.Usuario;
import com.banco.security.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<String> validarCredenciales(String username, String password) {
        return usuarioRepository.findByUsername(username)
                .filter(usuario -> passwordEncoder.matches(password, usuario.getPassword()))
                .map(Usuario::getRol);
    }
}
