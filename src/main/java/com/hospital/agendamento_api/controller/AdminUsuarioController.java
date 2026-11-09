package com.hospital.agendamento_api.controller;

import com.hospital.agendamento_api.dto.AdminAlterarSenhaRequestDTO;
import com.hospital.agendamento_api.dto.AlterarCargoRequestDTO;
import com.hospital.agendamento_api.dto.AlterarStatusUsuarioRequestDTO;
import com.hospital.agendamento_api.dto.UsuarioResponseDTO;
import com.hospital.agendamento_api.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Gerenciamento de usuários pelo administrador. O acesso (ROLE_ADMIN) é garantido no SecurityConfig. */
@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuarioController {

    private final UsuarioService usuarioService;

    public AdminUsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(usuarioService.buscarPorPublicId(publicId));
    }

    @PatchMapping("/{publicId}/senha")
    public ResponseEntity<Void> redefinirSenha(
            @PathVariable UUID publicId,
            @Valid @RequestBody AdminAlterarSenhaRequestDTO dto) {
        usuarioService.redefinirSenhaAdmin(publicId, dto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{publicId}/cargo")
    public ResponseEntity<UsuarioResponseDTO> alterarCargo(
            @PathVariable UUID publicId,
            @Valid @RequestBody AlterarCargoRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(usuarioService.alterarCargoAdmin(publicId, dto, authentication.getName()));
    }

    @PatchMapping("/{publicId}/status")
    public ResponseEntity<UsuarioResponseDTO> alterarStatus(
            @PathVariable UUID publicId,
            @Valid @RequestBody AlterarStatusUsuarioRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(usuarioService.alterarStatusAdmin(publicId, dto, authentication.getName()));
    }
}
