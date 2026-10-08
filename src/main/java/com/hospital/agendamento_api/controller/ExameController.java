package com.hospital.agendamento_api.controller;

import com.hospital.agendamento_api.dto.ExameResponseDTO;
import com.hospital.agendamento_api.service.ExameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exames")
@CrossOrigin(origins = "*")
public class ExameController {

    private final ExameService exameService;

    public ExameController(ExameService exameService) {
        this.exameService = exameService;
    }

    @GetMapping
    public ResponseEntity<List<ExameResponseDTO>> listar() {
        return ResponseEntity.ok(exameService.listarExamesAtivos());
    }
}