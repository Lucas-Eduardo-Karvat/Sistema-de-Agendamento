package com.hospital.agendamento_api.service;

import com.hospital.agendamento_api.dto.*;
import com.hospital.agendamento_api.entity.Cargo;
import com.hospital.agendamento_api.entity.Endereco;
import com.hospital.agendamento_api.entity.Paciente;
import com.hospital.agendamento_api.entity.Usuario;
import com.hospital.agendamento_api.exception.BusinessException;
import com.hospital.agendamento_api.exception.ResourceNotFoundException;
import com.hospital.agendamento_api.repository.CargoRepository;
import com.hospital.agendamento_api.repository.EnderecoRepository;
import com.hospital.agendamento_api.repository.PacienteRepository;
import com.hospital.agendamento_api.repository.UsuarioRepository;
import com.hospital.agendamento_api.security.TokenService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CargoRepository cargoRepository;
    private final EnderecoRepository enderecoRepository;
    private final PacienteRepository pacienteRepository; // Injetado
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public UsuarioService(UsuarioRepository usuarioRepository, 
                          CargoRepository cargoRepository, 
                          EnderecoRepository enderecoRepository,
                          PacienteRepository pacienteRepository,
                          PasswordEncoder passwordEncoder,
                          TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.cargoRepository = cargoRepository;
        this.enderecoRepository = enderecoRepository;
        this.pacienteRepository = pacienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public UsuarioResponseDTO cadastrar(UsuarioCadastroRequestDTO dto) {
        // 1. Validações de duplicação
        if (usuarioRepository.existsByCpf(dto.cpf())) {
            throw new BusinessException("Já existe um usuário cadastrado com este CPF.");
        }
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new BusinessException("Já existe um usuário cadastrado com este E-mail.");
        }
        // O titular também vira "paciente", e o CPF de paciente é único (inclui dependentes de outras contas)
        if (pacienteRepository.existsByCpfAndAtivoTrue(dto.cpf())) {
            throw new BusinessException("Já existe um usuário cadastrado com este CPF.");
        }

        // 2. Busca o cargo PACIENTE
        Cargo cargo = cargoRepository.findByNome("PACIENTE")
                .orElseThrow(() -> new ResourceNotFoundException("Cargo PACIENTE não encontrado no sistema."));

        // 3. Monta e salva o Endereço
        Endereco endereco = new Endereco();
        endereco.setCep(dto.endereco().cep());
        endereco.setLogradouro(dto.endereco().logradouro());
        endereco.setNumero(dto.endereco().numero());
        endereco.setComplemento(dto.endereco().complemento());
        endereco.setBairro(dto.endereco().bairro());
        endereco.setCidade(dto.endereco().cidade());
        endereco.setEstado(dto.endereco().estado());
        endereco = enderecoRepository.save(endereco);

        // 4. Monta e salva o Usuário
        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome());
        usuario.setCpf(dto.cpf());
        usuario.setEmail(dto.email());
        usuario.setSenhaHash(passwordEncoder.encode(dto.senha()));
        usuario.setTelefone(dto.telefone());
        usuario.setDataNascimento(dto.dataNascimento());
        usuario.setCargo(cargo);
        usuario.setEndereco(endereco);
        usuario.setAtivo(true);

        usuario = usuarioRepository.save(usuario);

        // 5. CRIAÇÃO DO PACIENTE TITULAR (Garante consistência hospitalar)
        Paciente pacienteTitular = new Paciente();
        pacienteTitular.setUsuarioResponsavel(usuario);
        pacienteTitular.setNome(usuario.getNome());
        pacienteTitular.setCpf(usuario.getCpf());
        pacienteTitular.setDataNascimento(usuario.getDataNascimento());
        pacienteTitular.setParentesco("TITULAR");
        pacienteTitular.setConvenio("Particular"); // Padrão inicial caso não venha no cadastro

        pacienteRepository.save(pacienteTitular);

        return converterParaResponseDTO(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(this::converterParaResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorPublicId(UUID publicId) {
        Usuario usuario = usuarioRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID informado."));
        return converterParaResponseDTO(usuario);
    }

    /** Dados do próprio usuário autenticado (usado pela tela de Perfil; não exige perfil de recepção). */
    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        return converterParaResponseDTO(usuario);
    }

    /** Atualiza os dados do próprio usuário. CPF e cargo não mudam. */
    @Transactional
    public PerfilAtualizadoResponseDTO atualizarPerfil(String emailAtual, PerfilUpdateRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(emailAtual)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        String novoEmail = dto.email().trim();
        boolean emailMudou = !novoEmail.equals(usuario.getEmail());
        if (emailMudou && usuarioRepository.existsByEmail(novoEmail)) {
            throw new BusinessException("Já existe um usuário cadastrado com este E-mail.");
        }

        usuario.setNome(dto.nome().trim());
        usuario.setEmail(novoEmail);
        usuario.setTelefone(dto.telefone());
        usuario.setDataNascimento(dto.dataNascimento());

        Endereco endereco = usuario.getEndereco();
        endereco.setCep(dto.endereco().cep());
        endereco.setLogradouro(dto.endereco().logradouro());
        endereco.setNumero(dto.endereco().numero());
        endereco.setComplemento(dto.endereco().complemento());
        endereco.setBairro(dto.endereco().bairro());
        endereco.setCidade(dto.endereco().cidade());
        endereco.setEstado(dto.endereco().estado());

        // Mantém o paciente titular em sincronia com o cadastro do usuário
        pacienteRepository.findByUsuarioResponsavelIdAndParentesco(usuario.getId(), "TITULAR")
                .ifPresent(titular -> {
                    titular.setNome(usuario.getNome());
                    titular.setDataNascimento(usuario.getDataNascimento());
                    pacienteRepository.save(titular);
                });

        usuarioRepository.save(usuario);

        // O e-mail é o "subject" do JWT: se mudou, o token antigo deixa de funcionar, então devolve um novo.
        String novoToken = emailMudou ? tokenService.gerarToken(usuario) : null;
        return new PerfilAtualizadoResponseDTO(converterParaResponseDTO(usuario), novoToken);
    }

    @Transactional
    public void alterarSenha(String email, AlterarSenhaRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        if (!passwordEncoder.matches(dto.senhaAtual(), usuario.getSenhaHash())) {
            throw new BusinessException("A senha atual está incorreta.");
        }
        if (passwordEncoder.matches(dto.novaSenha(), usuario.getSenhaHash())) {
            throw new BusinessException("A nova senha deve ser diferente da senha atual.");
        }

        usuario.setSenhaHash(passwordEncoder.encode(dto.novaSenha()));
        usuarioRepository.save(usuario);
    }

    private UsuarioResponseDTO converterParaResponseDTO(Usuario usuario) {
        EnderecoResponseDTO enderecoDTO = new EnderecoResponseDTO(
                usuario.getEndereco().getPublicId(),
                usuario.getEndereco().getCep(),
                usuario.getEndereco().getLogradouro(),
                usuario.getEndereco().getNumero(),
                usuario.getEndereco().getComplemento(),
                usuario.getEndereco().getBairro(),
                usuario.getEndereco().getCidade(),
                usuario.getEndereco().getEstado()
        );

        return new UsuarioResponseDTO(
                usuario.getPublicId(),
                usuario.getNome(),
                usuario.getCpf(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.getDataNascimento(),
                usuario.getCargo().getNome(),
                usuario.getAtivo(),
                usuario.getDataCriacao(),
                enderecoDTO
        );
    }
}