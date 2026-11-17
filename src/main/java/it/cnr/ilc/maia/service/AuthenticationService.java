package it.cnr.ilc.maia.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import it.cnr.ilc.maia.dto.UserDto;
import it.cnr.ilc.maia.model.Role;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthenticationService {

    @Getter
    private String publicKey;
    private String privateKey;

    @Autowired
    private UserService userService;

    @Autowired
    private Environment environment;

    @PostConstruct
    public void init() throws IOException {
        publicKey = new String(Files.readAllBytes(Paths.get(environment.getProperty("jwt.public-key", "public.pem"))))
                .replaceAll("-----.*-----", "")
                .replaceAll("\n", "");
        privateKey = new String(Files.readAllBytes(Paths.get(environment.getProperty("jwt.private-key", "private8.pem"))))
                .replaceAll("-----.*-----", "")
                .replaceAll("\n", "");
    }

    public String createJwt(UserDto userDto) throws AuthenticationException {
        try {
            EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey));
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey key = kf.generatePrivate(keySpec);
            String jwtToken = Jwts.builder()
                    .claim("username", userDto.getUsername())
                    .claim("name", userDto.getName() + " " + userDto.getSurname())
                    .claim("email", userDto.getEmail())
                    .claim("role", userDto.getRole())
                    .id(UUID.randomUUID().toString())
                    .issuedAt(Date.from(Instant.now()))
                    .expiration(Date.from(Instant.now().plus(environment.getProperty("jwt.expiration", Long.class, 180l), ChronoUnit.MINUTES)))
                    .signWith(key)
                    .compact();
            return jwtToken;
        } catch (Exception ex) {
            throw new AuthenticationException(ex.getMessage());
        }
    }

    public String renewJwt(String jwtToken) throws AuthenticationException {
        try {
            _validate(jwtToken);
        } catch (ExpiredJwtException ex) {
            return _renewJwt(jwtToken);
        } catch (Exception e) {
            throw new AuthenticationException(e.getMessage());
        }
        return _renewJwt(jwtToken);
    }

    private final ObjectMapper mapper = new ObjectMapper();

    private String _renewJwt(String jwtToken) throws AuthenticationException {
        try {
            jwtToken = new String(Base64.getDecoder().decode(jwtToken.split("\\.")[1]));
            Map map = mapper.readValue(jwtToken, Map.class);
            UserDto userDto = new UserDto();
            userDto.setUsername((String) map.get("username"));
            userDto.setRole(Role.valueOf((String) map.get("role")));
            return createJwt(userDto);
        } catch (JsonProcessingException ex) {
            throw new AuthenticationException(ex.getMessage());
        }
    }

    public UserDto validate(String jwtToken) throws AuthenticationException {
        try {
            Claims claims = _validate(jwtToken);
            return userService.getUserByUsername(claims.get("username", String.class));
        } catch (Exception ex) {
            throw new AuthenticationException(ex.getMessage());
        }
    }

    private Claims _validate(String jwtToken) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey key = keyFactory.generatePublic(keySpec);
        Jws<Claims> jwt = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(jwtToken);
        return jwt.getPayload();
    }

    public static class AuthenticationException extends Exception {

        public AuthenticationException(String message) {
            super(message);
        }

    }
}
