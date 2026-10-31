package com.hospital.agendamento_api.controller;

import com.hospital.agendamento_api.dto.AgendamentoRequestDTO;
import com.hospital.agendamento_api.dto.AgendamentoResponseDTO;
import com.hospital.agendamento_api.dto.ReagendarAgendamentoRequestDTO;
import com.hospital.agendamento_api.service.AgendamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/agendamentos")
@CrossOrigin(origins = "*")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    public AgendamentoController(AgendamentoService agendamentoService) {
        this.agendamentoService = agendamentoService;
    }

    @PostMapping
    public ResponseEntity<AgendamentoResponseDTO> criar(
            @Valid @RequestBody AgendamentoRequestDTO dto,
            Authentication authentication) {

        String emailSolicitante = authentication.getName();
        AgendamentoResponseDTO criado = agendamentoService.criarAgendamento(dto, emailSolicitante);

        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @GetMapping("/meus-agendamentos")
    public ResponseEntity<List<AgendamentoResponseDTO>> listarMeusAgendamentos(Authentication authentication) {
        String emailSolicitante = authentication.getName();
        return ResponseEntity.ok(agendamentoService.listarPorSolicitante(emailSolicitante));
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<AgendamentoResponseDTO> buscarPorPublicId(
            @PathVariable UUID publicId,
            Authentication authentication) {
        String emailSolicitante = authentication.getName();
        return ResponseEntity.ok(agendamentoService.buscarPorPublicId(publicId, emailSolicitante));
    }

    // Reagendar: cria a nova solicitação com as novas datas e remove a anterior
    @PostMapping("/{publicId}/reagendar")
    public ResponseEntity<AgendamentoResponseDTO> reagendar(
            @PathVariable UUID publicId,
            @Valid @RequestBody ReagendarAgendamentoRequestDTO dto,
            Authentication authentication) {
        AgendamentoResponseDTO novo = agendamentoService.reagendar(publicId, dto, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(novo);
    }

    @PatchMapping("/{publicId}/cancelar")
    public ResponseEntity<AgendamentoResponseDTO> cancelar(
            @PathVariable UUID publicId,
            Authentication authentication) {
        String emailSolicitante = authentication.getName();
        return ResponseEntity.ok(agendamentoService.cancelarPeloPaciente(publicId, emailSolicitante));
    }
}