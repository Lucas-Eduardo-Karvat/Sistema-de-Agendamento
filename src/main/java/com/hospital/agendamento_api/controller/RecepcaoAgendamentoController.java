package com.hospital.agendamento_api.controller;

import com.hospital.agendamento_api.dto.AgendamentoRecepcaoRequestDTO;
import com.hospital.agendamento_api.dto.AgendamentoResponseDTO;
import com.hospital.agendamento_api.dto.CancelarAgendamentoRequestDTO;
import com.hospital.agendamento_api.dto.ConfirmarAgendamentoRequestDTO;
import com.hospital.agendamento_api.dto.RemarcarAgendamentoRequestDTO;
import com.hospital.agendamento_api.service.RecepcaoAgendamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recepcao/agendamentos")
public class RecepcaoAgendamentoController {

    private final RecepcaoAgendamentoService recepcaoService;

    public RecepcaoAgendamentoController(RecepcaoAgendamentoService recepcaoService) {
        this.recepcaoService = recepcaoService;
    }

    // Ex.: GET /api/recepcao/agendamentos?status=PENDENTE  (fila de análise da recepção)
    @GetMapping
    public ResponseEntity<List<AgendamentoResponseDTO>> listar(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(recepcaoService.listar(status));
    }

    @PatchMapping("/{publicId}/confirmar")
    public ResponseEntity<AgendamentoResponseDTO> confirmar(
            @PathVariable UUID publicId,
            @Valid @RequestBody ConfirmarAgendamentoRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(recepcaoService.confirmar(publicId, dto, authentication.getName()));
    }

    // Agenda direto para um paciente (já nasce CONFIRMADO)
    @PostMapping
    public ResponseEntity<AgendamentoResponseDTO> criar(
            @Valid @RequestBody AgendamentoRecepcaoRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recepcaoService.criar(dto, authentication.getName()));
    }

    @PatchMapping("/{publicId}/recusar")
    public ResponseEntity<AgendamentoResponseDTO> recusar(
            @PathVariable UUID publicId,
            @Valid @RequestBody CancelarAgendamentoRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(recepcaoService.recusar(publicId, dto, authentication.getName()));
    }

    @PatchMapping("/{publicId}/remarcar")
    public ResponseEntity<AgendamentoResponseDTO> remarcar(
            @PathVariable UUID publicId,
            @Valid @RequestBody RemarcarAgendamentoRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(recepcaoService.remarcar(publicId, dto, authentication.getName()));
    }

    @PatchMapping("/{publicId}/concluir")
    public ResponseEntity<AgendamentoResponseDTO> concluir(
            @PathVariable UUID publicId,
            Authentication authentication) {
        return ResponseEntity.ok(recepcaoService.concluir(publicId, authentication.getName()));
    }

    @PatchMapping("/{publicId}/cancelar")
    public ResponseEntity<AgendamentoResponseDTO> cancelar(
            @PathVariable UUID publicId,
            @Valid @RequestBody CancelarAgendamentoRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(recepcaoService.cancelar(publicId, dto, authentication.getName()));
    }
}
