package com.hospital.agendamento_api.service;

import com.hospital.agendamento_api.dto.*;
import com.hospital.agendamento_api.entity.Cargo;
import com.hospital.agendamento_api.entity.Endereco;
import com.hospital.agendamento_api.entity.Usuario;
import com.hospital.agendamento_api.exception.BusinessException;
import com.hospital.agendamento_api.exception.ResourceNotFoundException;
import com.hospital.agendamento_api.repository.CargoRepository;
import com.hospital.agendamento_api.repository.EnderecoRepository;
import com.hospital.agendamento_api.repository.UsuarioRepository;

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
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, 
                          CargoRepository cargoRepository, 
                          EnderecoRepository enderecoRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.cargoRepository = cargoRepository;
        this.enderecoRepository = enderecoRepository;
        this.passwordEncoder = passwordEncoder;
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

        // 2. Cadastro público sempre vira PACIENTE (não aceita cargo escolhido pelo cliente)
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
        
        // CRIPTOGRAFIA ATIVADA: Criptografa a senha com BCrypt antes de salvar no PostgreSQL
        usuario.setSenhaHash(passwordEncoder.encode(dto.senha()));
        
        usuario.setTelefone(dto.telefone());
        usuario.setDataNascimento(dto.dataNascimento());
        usuario.setCargo(cargo);
        usuario.setEndereco(endereco);
        usuario.setAtivo(true);

        usuario = usuarioRepository.save(usuario);

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