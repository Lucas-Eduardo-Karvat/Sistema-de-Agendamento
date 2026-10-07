package com.hospital.agendamento_api.service;

import com.hospital.agendamento_api.security.TokenService;
import com.hospital.agendamento_api.dto.LoginRequestDTO;
import com.hospital.agendamento_api.dto.TokenResponseDTO;
import com.hospital.agendamento_api.entity.Usuario;
import com.hospital.agendamento_api.exception.UnauthorizedException;
import com.hospital.agendamento_api.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public TokenResponseDTO autenticar(LoginRequestDTO dto) {
        // 1. Busca usuário por E-mail ou por CPF
        Usuario usuario = usuarioRepository.findByEmail(dto.login())
                .orElseGet(() -> usuarioRepository.findByCpf(dto.login())
                        .orElseThrow(() -> new UnauthorizedException("Usuário ou senha inválidos.")));

        // 2. Valida se o usuário está ativo no sistema
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new UnauthorizedException("Conta de usuário inativa.");
        }

        // 3. Compara a senha enviada no JSON com o hash BCrypt salvo no banco
        if (!passwordEncoder.matches(dto.senha(), usuario.getSenhaHash())) {
            throw new UnauthorizedException("Usuário ou senha inválidos.");
        }

        // 4. Gera o Token JWT contendo os claims (publicId, cargo, email)
        String token = tokenService.gerarToken(usuario);

        return new TokenResponseDTO(
                token,
                "Bearer",
                usuario.getPublicId(),
                usuario.getNome(),
                usuario.getCargo().getNome()
        );
    }
}