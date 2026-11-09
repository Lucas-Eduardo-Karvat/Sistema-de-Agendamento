package com.hospital.agendamento_api.runner;

import com.hospital.agendamento_api.entity.Cargo;
import com.hospital.agendamento_api.entity.Endereco;
import com.hospital.agendamento_api.entity.Usuario;
import com.hospital.agendamento_api.repository.CargoRepository;
import com.hospital.agendamento_api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Cria um administrador inicial APENAS no ambiente de desenvolvimento, para poder usar o painel
 * (/api/admin/**) sem mexer direto no banco. Em produção, promova um usuário pelo banco.
 */
@Component
@Profile("dev")
public class DevAdminSeeder implements CommandLineRunner {

    private static final String EMAIL = "admin@hospital.com";

    private final UsuarioRepository usuarioRepository;
    private final CargoRepository cargoRepository;
    private final PasswordEncoder passwordEncoder;
    private final String senhaInicial;

    public DevAdminSeeder(UsuarioRepository usuarioRepository,
                          CargoRepository cargoRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.dev.admin-senha:admin123}") String senhaInicial) {
        this.usuarioRepository = usuarioRepository;
        this.cargoRepository = cargoRepository;
        this.passwordEncoder = passwordEncoder;
        this.senhaInicial = senhaInicial;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.existsByEmail(EMAIL)) {
            return;
        }

        Cargo cargo = cargoRepository.findByNome("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Cargo ADMIN não encontrado (migration V7)"));

        Endereco endereco = new Endereco();
        endereco.setCep("89340000");
        endereco.setLogradouro("Rua do Hospital");
        endereco.setNumero("1");
        endereco.setBairro("Centro");
        endereco.setCidade("Itaiópolis");
        endereco.setEstado("SC");

        Usuario u = new Usuario();
        u.setNome("Administrador (DEV)");
        u.setCpf("22222222222");
        u.setEmail(EMAIL);
        u.setSenhaHash(passwordEncoder.encode(senhaInicial));
        u.setTelefone("47999999998");
        u.setDataNascimento(LocalDate.of(1990, 1, 1));
        u.setCargo(cargo);
        u.setEndereco(endereco); // Usuario tem CascadeType.ALL no endereço, então ele é salvo junto
        u.setAtivo(true);

        usuarioRepository.save(u);
        System.out.println(">>> [DEV] Administrador criado: " + EMAIL);
    }
}
