package com.hospital.agendamento_api.controller;

import com.hospital.agendamento_api.dto.AlterarSenhaRequestDTO;
import com.hospital.agendamento_api.dto.PerfilAtualizadoResponseDTO;
import com.hospital.agendamento_api.dto.PerfilUpdateRequestDTO;
import com.hospital.agendamento_api.dto.UsuarioCadastroRequestDTO;
import com.hospital.agendamento_api.dto.UsuarioResponseDTO;
import com.hospital.agendamento_api.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> cadastrar(@Valid @RequestBody UsuarioCadastroRequestDTO dto) {
        UsuarioResponseDTO response = usuarioService.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Perfil do usuário logado. Declarado antes de "/{publicId}" e liberado para qualquer autenticado no SecurityConfig.
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> meuPerfil(Authentication authentication) {
        return ResponseEntity.ok(usuarioService.buscarPorEmail(authentication.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<PerfilAtualizadoResponseDTO> atualizarMeuPerfil(
            @Valid @RequestBody PerfilUpdateRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(usuarioService.atualizarPerfil(authentication.getName(), dto));
    }

    @PatchMapping("/me/senha")
    public ResponseEntity<Void> alterarMinhaSenha(
            @Valid @RequestBody AlterarSenhaRequestDTO dto,
            Authentication authentication) {
        usuarioService.alterarSenha(authentication.getName(), dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(usuarioService.buscarPorPublicId(publicId));
    }
}