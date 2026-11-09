package com.hospital.agendamento_api.controller;

import com.hospital.agendamento_api.dto.AlterarStatusExameRequestDTO;
import com.hospital.agendamento_api.dto.ExameAdminResponseDTO;
import com.hospital.agendamento_api.dto.ExameCadastroRequestDTO;
import com.hospital.agendamento_api.dto.ExameUpdateRequestDTO;
import com.hospital.agendamento_api.service.ExameService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Gerenciamento completo de exames pelo administrador. O acesso (ROLE_ADMIN) é garantido no SecurityConfig. */
@RestController
@RequestMapping("/api/admin/exames")
public class AdminExameController {

    private final ExameService exameService;

    public AdminExameController(ExameService exameService) {
        this.exameService = exameService;
    }

    @GetMapping
    public ResponseEntity<List<ExameAdminResponseDTO>> listarTodos() {
        return ResponseEntity.ok(exameService.listarTodosAdmin());
    }

    @PostMapping
    public ResponseEntity<ExameAdminResponseDTO> cadastrar(@Valid @RequestBody ExameCadastroRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exameService.cadastrarAdmin(dto));
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<ExameAdminResponseDTO> atualizar(
            @PathVariable UUID publicId,
            @Valid @RequestBody ExameUpdateRequestDTO dto) {
        return ResponseEntity.ok(exameService.atualizar(publicId, dto));
    }

    @PatchMapping("/{publicId}/status")
    public ResponseEntity<ExameAdminResponseDTO> alterarStatus(
            @PathVariable UUID publicId,
            @Valid @RequestBody AlterarStatusExameRequestDTO dto) {
        return ResponseEntity.ok(exameService.alterarStatus(publicId, dto.ativo()));
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> excluir(@PathVariable UUID publicId) {
        exameService.excluir(publicId);
        return ResponseEntity.noContent().build();
    }
}
