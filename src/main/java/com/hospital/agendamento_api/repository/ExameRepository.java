package com.hospital.agendamento_api.repository;

import com.hospital.agendamento_api.entity.Exame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExameRepository extends JpaRepository<Exame, Long> {
    Optional<Exame> findByPublicId(UUID publicId);
    List<Exame> findByAtivoTrue();
    boolean existsByNomeIgnoreCase(String nome);
}