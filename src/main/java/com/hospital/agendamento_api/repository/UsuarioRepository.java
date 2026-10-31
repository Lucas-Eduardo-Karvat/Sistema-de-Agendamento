package com.hospital.agendamento_api.repository;

import com.hospital.agendamento_api.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByCpf(String cpf);
    boolean existsByEmail(String email);
    Optional<Usuario> findByPublicId(UUID publicId);
    
    // Métodos essenciais para o LOGIN:
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByCpf(String cpf);
}