package com.hospital.agendamento_api.service;

import com.hospital.agendamento_api.dto.DependenteRequestDTO;
import com.hospital.agendamento_api.dto.PacienteResponseDTO;
import com.hospital.agendamento_api.entity.Paciente;
import com.hospital.agendamento_api.entity.Usuario;
import com.hospital.agendamento_api.exception.ResourceNotFoundException;
import com.hospital.agendamento_api.repository.PacienteRepository;
import com.hospital.agendamento_api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;

    public PacienteService(PacienteRepository pacienteRepository, UsuarioRepository usuarioRepository) {
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public PacienteResponseDTO cadastrarDependente(DependenteRequestDTO dto, String emailResponsavel) {
        Usuario responsavel = usuarioRepository.findByEmail(emailResponsavel)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário responsável não encontrado"));

        Paciente dependente = new Paciente();
        dependente.setUsuarioResponsavel(responsavel);
        dependente.setNome(dto.nome());
        dependente.setCpf(dto.cpf());
        dependente.setDataNascimento(dto.dataNascimento());
        dependente.setParentesco(dto.parentesco().toUpperCase());
        dependente.setConvenio(dto.convenio() != null && !dto.convenio().isBlank() ? dto.convenio() : "Particular");

        Paciente salvo = pacienteRepository.save(dependente);

        return mapearParaDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<PacienteResponseDTO> listarMeusPacientes(String emailResponsavel) {
        Usuario responsavel = usuarioRepository.findByEmail(emailResponsavel)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário responsável não encontrado"));

        return pacienteRepository.findByUsuarioResponsavelId(responsavel.getId())
                .stream()
                .map(this::mapearParaDTO)
                .toList();
    }

    private PacienteResponseDTO mapearParaDTO(Paciente p) {
        return new PacienteResponseDTO(
                p.getPublicId(),
                p.getNome(),
                p.getCpf(),
                p.getDataNascimento(),
                p.getParentesco(),
                p.getConvenio()
        );
    }
}