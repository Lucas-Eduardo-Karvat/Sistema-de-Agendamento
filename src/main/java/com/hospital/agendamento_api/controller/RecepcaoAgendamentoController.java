package com.hospital.agendamento_api.controller;

import com.hospital.agendamento_api.dto.AgendamentoResponseDTO;
import com.hospital.agendamento_api.dto.CancelarAgendamentoRequestDTO;
import com.hospital.agendamento_api.dto.ConfirmarAgendamentoRequestDTO;
import com.hospital.agendamento_api.service.RecepcaoAgendamentoService;
import jakarta.validation.Valid;
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

    @PatchMapping("/{publicId}/cancelar")
    public ResponseEntity<AgendamentoResponseDTO> cancelar(
            @PathVariable UUID publicId,
            @Valid @RequestBody CancelarAgendamentoRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(recepcaoService.cancelar(publicId, dto, authentication.getName()));
    }
}
