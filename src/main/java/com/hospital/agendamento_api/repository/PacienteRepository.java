package com.hospital.agendamento_api.repository;

import com.hospital.agendamento_api.entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    Optional<Paciente> findByPublicId(UUID publicId);

    // Lista da recepção: já traz responsável e endereço para não disparar uma consulta por paciente
    @Query("SELECT p FROM Paciente p JOIN FETCH p.usuarioResponsavel u JOIN FETCH u.endereco WHERE p.ativo = true")
    List<Paciente> listarTodosComResponsavel();
    
    // Lista todos os pacientes (titular + dependentes) sob o mesmo usuário responsável
    List<Paciente> findByUsuarioResponsavelId(Long usuarioResponsavelId);
    
    // Só os pacientes que ainda não foram removidos pelo responsável
    List<Paciente> findByUsuarioResponsavelIdAndAtivoTrue(Long usuarioResponsavelId);

    // CPF é único apenas entre pacientes ativos
    boolean existsByCpfAndAtivoTrue(String cpf);

    boolean existsByCpfAndAtivoTrueAndIdNot(String cpf, Long id);

    // Busca o registro TITULAR do usuário
    Optional<Paciente> findByUsuarioResponsavelIdAndParentesco(Long usuarioResponsavelId, String parentesco);
}