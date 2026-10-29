package com.hospital.agendamento_api.repository;

import com.hospital.agendamento_api.entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    Optional<Paciente> findByPublicId(UUID publicId);
    
    // Lista todos os pacientes (titular + dependentes) sob o mesmo usuário responsável
    List<Paciente> findByUsuarioResponsavelId(Long usuarioResponsavelId);
    
    // Busca o registro TITULAR do usuário
    Optional<Paciente> findByUsuarioResponsavelIdAndParentesco(Long usuarioResponsavelId, String parentesco);
}