package com.hospital.agendamento_api.controller;

import com.hospital.agendamento_api.dto.UsuarioCadastroRequestDTO;
import com.hospital.agendamento_api.dto.UsuarioResponseDTO;
import com.hospital.agendamento_api.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(usuarioService.buscarPorPublicId(publicId));
    }
}