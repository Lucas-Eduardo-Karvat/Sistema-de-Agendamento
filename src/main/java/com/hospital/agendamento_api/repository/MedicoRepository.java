package com.hospital.agendamento_api.repository;

import com.hospital.agendamento_api.entity.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicoRepository extends JpaRepository<Medico, Long> {
    Optional<Medico> findByUsuarioPublicId(UUID publicId);
    Optional<Medico> findByCrm(String crm);
    List<Medico> findByEspecialidade(String especialidade);
}