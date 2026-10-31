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
 * Cria uma recepcionista inicial APENAS no ambiente de desenvolvimento,
 * já que o cadastro público (/api/usuarios) só cria usuários com cargo PACIENTE.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private static final String EMAIL = "recepcao@hospital.com";

    private final UsuarioRepository usuarioRepository;
    private final CargoRepository cargoRepository;
    private final PasswordEncoder passwordEncoder;
    private final String senhaInicial;

    public DevDataSeeder(UsuarioRepository usuarioRepository,
                         CargoRepository cargoRepository,
                         PasswordEncoder passwordEncoder,
                         @Value("${app.dev.recepcao-senha:recepcao123}") String senhaInicial) {
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

        Cargo cargo = cargoRepository.findByNome("RECEPCIONISTA")
                .orElseThrow(() -> new IllegalStateException("Cargo RECEPCIONISTA não encontrado (migration V4)"));

        Endereco endereco = new Endereco();
        endereco.setCep("89340000");
        endereco.setLogradouro("Rua do Hospital");
        endereco.setNumero("1");
        endereco.setBairro("Centro");
        endereco.setCidade("Itaiópolis");
        endereco.setEstado("SC");

        Usuario u = new Usuario();
        u.setNome("Recepção (DEV)");
        u.setCpf("11111111111");
        u.setEmail(EMAIL);
        u.setSenhaHash(passwordEncoder.encode(senhaInicial));
        u.setTelefone("47999999999");
        u.setDataNascimento(LocalDate.of(1990, 1, 1));
        u.setCargo(cargo);
        u.setEndereco(endereco); // Usuario tem CascadeType.ALL no endereço, então ele é salvo junto
        u.setAtivo(true);

        usuarioRepository.save(u);
        System.out.println(">>> [DEV] Recepcionista criada: " + EMAIL);
    }
}
