package com.banco.bffcajero.controller;

import com.banco.bffcajero.client.CoreClient;
import com.banco.bffcajero.dto.LoginRequest;
import com.banco.bffcajero.dto.LoginResponse;
import com.banco.bffcajero.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String ROL_CANAL = "ROLE_CAJERO";

    private final CoreClient coreClient;
    private final JwtUtil jwtUtil;

    public AuthController(CoreClient coreClient, JwtUtil jwtUtil) {
        this.coreClient = coreClient;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return coreClient.validarCredenciales(request.username(), request.password())
                .filter(ROL_CANAL::equals)
                .map(rol -> ResponseEntity.ok(new LoginResponse(jwtUtil.generarToken(request.username(), rol))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
