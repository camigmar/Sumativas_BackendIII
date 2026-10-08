package com.banco.core.controller;

import com.banco.core.dto.ValidarCredencialesRequest;
import com.banco.core.dto.ValidarCredencialesResponse;
import com.banco.core.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/validar")
    public ResponseEntity<ValidarCredencialesResponse> validar(@RequestBody ValidarCredencialesRequest request) {
        return authService.validarCredenciales(request.username(), request.password())
                .map(rol -> ResponseEntity.ok(new ValidarCredencialesResponse(rol)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
