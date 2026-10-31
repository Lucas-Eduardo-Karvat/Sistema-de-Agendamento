package com.hospital.agendamento_api.controller;

import com.hospital.agendamento_api.dto.PacienteRecepcaoResponseDTO;
import com.hospital.agendamento_api.dto.PacienteResponseDTO;
import com.hospital.agendamento_api.dto.UsuarioCadastroRequestDTO;
import com.hospital.agendamento_api.service.RecepcaoPacienteService;
import jakarta.validation.Valid;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recepcao/pacientes")
public class RecepcaoPacienteController {

    private final RecepcaoPacienteService recepcaoPacienteService;

    public RecepcaoPacienteController(RecepcaoPacienteService recepcaoPacienteService) {
        this.recepcaoPacienteService = recepcaoPacienteService;
    }

    // Lista os pacientes (titulares e dependentes) com contato, última consulta e próximo exame
    @GetMapping
    public ResponseEntity<List<PacienteRecepcaoResponseDTO>> listar() {
        return ResponseEntity.ok(recepcaoPacienteService.listar());
    }

    // Cadastra um paciente novo (com acesso ao portal) e devolve o titular para agendar em seguida
    @PostMapping
    public ResponseEntity<PacienteResponseDTO> cadastrar(@Valid @RequestBody UsuarioCadastroRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recepcaoPacienteService.cadastrar(dto));
    }
}
