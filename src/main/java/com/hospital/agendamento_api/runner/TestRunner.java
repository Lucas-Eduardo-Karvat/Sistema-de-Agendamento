package com.hospital.agendamento_api.runner;

import com.hospital.agendamento_api.repository.ExameRepository;
import com.hospital.agendamento_api.repository.MedicoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("dev-test")
public class TestRunner implements CommandLineRunner {

    private final MedicoRepository medicoRepository;
    private final ExameRepository exameRepository;

    public TestRunner(MedicoRepository medicoRepository, ExameRepository exameRepository) {
        this.medicoRepository = medicoRepository;
        this.exameRepository = exameRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== TESTANDO CONSULTAS JPA ===");

        // Teste 1: Executa a query findByUsuarioPublicId no MedicoRepository
        medicoRepository.findByUsuarioPublicId(UUID.randomUUID());
        System.out.println("[OK] MedicoRepository.findByUsuarioPublicId executado com sucesso.");

        // Teste 2: Executa a query findByAtivoTrue no ExameRepository
        exameRepository.findByAtivoTrue();
        System.out.println("[OK] ExameRepository.findByAtivoTrue executado com sucesso.");

        System.out.println("=== TODOS OS REPOSITÓRIOS ESTÃO FUNCIONANDO ===");
    }
}