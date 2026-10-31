package com.hospital.agendamento_api.controller;

import com.hospital.agendamento_api.dto.LoginRequestDTO;
import com.hospital.agendamento_api.dto.TokenResponseDTO;
import com.hospital.agendamento_api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        TokenResponseDTO response = authService.autenticar(dto);
        return ResponseEntity.ok(response);
    }
}