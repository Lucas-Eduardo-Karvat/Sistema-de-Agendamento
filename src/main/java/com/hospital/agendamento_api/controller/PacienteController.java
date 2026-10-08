package com.hospital.agendamento_api.controller;

import com.hospital.agendamento_api.dto.DependenteRequestDTO;
import com.hospital.agendamento_api.dto.PacienteResponseDTO;
import com.hospital.agendamento_api.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
@CrossOrigin(origins = "*")
public class PacienteController {

    private final PacienteService pacienteService;

    public PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @PostMapping("/dependentes")
    public ResponseEntity<PacienteResponseDTO> cadastrarDependente(
            @Valid @RequestBody DependenteRequestDTO dto,
            Authentication authentication) {
        String emailResponsavel = authentication.getName();
        PacienteResponseDTO dependente = pacienteService.cadastrarDependente(dto, emailResponsavel);
        return ResponseEntity.status(HttpStatus.CREATED).body(dependente);
    }

    @GetMapping("/meus-pacientes")
    public ResponseEntity<List<PacienteResponseDTO>> listarMeusPacientes(Authentication authentication) {
        String emailResponsavel = authentication.getName();
        return ResponseEntity.ok(pacienteService.listarMeusPacientes(emailResponsavel));
    }
}