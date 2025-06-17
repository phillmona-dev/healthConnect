package com.medco.HealthConnectProvider.config.securityConfig.jwtTokenService;

import com.medco.HealthConnectProvider.exception.UnauthorizedException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtService {
    @Value("${medcoanalytics.app.jwtSecret}")
    private String jwtSecretKey;

    @Value("${medcoanalytics.app.jwtExpirationMs}")
    private Long jwtExpirationInMs;

    public String generateToken(String userName){
        Map<String,Object> claims = new HashMap<>();
        return createToken(claims,userName);
    }

    private String createToken(Map<String, Object> claims, String userName) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userName)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis()+ jwtExpirationInMs))
                .signWith(key(), SignatureAlgorithm.HS256).compact();
    }


    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecretKey));
    }


    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateJwtToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parse(token);
            return true;
        } catch (MalformedJwtException e) {
            throw new UnauthorizedException(e.getMessage());
        }
//            catch (ExpiredJwtException e) {
//                throw new UnauthorizedException(e.getMessage());
//            } catch (UnsupportedJwtException e) {
//                throw new UnauthorizedException(e.getMessage());
//            } catch (IllegalArgumentException e) {
//                throw new UnauthorizedException(e.getMessage());
//            }

    }
}
