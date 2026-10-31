package com.hospital.agendamento_api.controller;

import com.hospital.agendamento_api.dto.ExameCadastroRequestDTO;
import com.hospital.agendamento_api.dto.ExameResponseDTO;
import com.hospital.agendamento_api.service.ExameService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recepcao/exames")
public class RecepcaoExameController {

    private final ExameService exameService;

    public RecepcaoExameController(ExameService exameService) {
        this.exameService = exameService;
    }

    @PostMapping
    public ResponseEntity<ExameResponseDTO> cadastrar(@Valid @RequestBody ExameCadastroRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exameService.cadastrar(dto));
    }
}
