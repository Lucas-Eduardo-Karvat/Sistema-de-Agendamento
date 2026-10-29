package com.hospital.agendamento_api.security;

import com.hospital.agendamento_api.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class TokenService {

    @Value("${api.security.token.secret:sua-chave-secreta-super-segura-de-32-caracteres-aqui}")
    private String secret;

    private static final long EXPIRATION_IN_SECONDS = 86400;

    public record TokenData(String email, String publicId, String cargo) {}

    public String gerarToken(Usuario usuario) {
        SecretKey key = getSigningKey();

        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("publicId", usuario.getPublicId().toString())
                .claim("cargo", usuario.getCargo().getNome())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(EXPIRATION_IN_SECONDS)))
                .signWith(key)
                .compact();
    }

    /**
     * Valida o token e retorna os claims principais.
     * Retorna null se o token for inválido ou estiver expirado.
     */
    public TokenData validarToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return new TokenData(
                    claims.getSubject(),
                    claims.get("publicId", String.class),
                    claims.get("cargo", String.class)
            );
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public String validarTokenEObterSubject(String token) {
        TokenData data = validarToken(token);
        return data != null ? data.email() : null;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
