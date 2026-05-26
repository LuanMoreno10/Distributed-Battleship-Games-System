package edu.ufp.inf.sd.battleshipgame.rmi;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtils {

    // Chave de 32 bytes (256-bit) para HS256 — em produção viria de config
    private static final byte[] SECRET_BYTES = "BattleshipSD2026SecretKey!xYz789".getBytes();
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET_BYTES);
    private static final long EXPIRATION_MS = 8 * 3600_000L; // 8 horas

    public static String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(KEY)
                .compact();
    }

    public static String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public static boolean isTokenValid(String token) {
        try {
            getUsernameFromToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
