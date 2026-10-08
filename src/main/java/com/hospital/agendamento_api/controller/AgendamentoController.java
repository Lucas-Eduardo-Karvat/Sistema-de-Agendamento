package com.hospital.agendamento_api.controller;

import com.hospital.agendamento_api.dto.AgendamentoRequestDTO;
import com.hospital.agendamento_api.dto.AgendamentoResponseDTO;
import com.hospital.agendamento_api.service.AgendamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}