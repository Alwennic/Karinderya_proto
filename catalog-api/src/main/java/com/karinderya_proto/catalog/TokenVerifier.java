package com.karinderya_proto.catalog;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Checks the login token (a JWT signed with HS256) that auth-api created.
 * A JWT is three Base64 parts joined by dots: header.payload.signature.
 * We recompute the signature with the shared secret and compare it.
 */
@Component
public class TokenVerifier {

    private final byte[] secret;
    private final ObjectMapper mapper = new ObjectMapper();

    public TokenVerifier(@Value("${karinderya_proto.jwt.secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    /** Returns the role inside a valid, unexpired token, or empty if the token is not valid. */
    public Optional<String> verifiedRole(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }
            Base64.Decoder decoder = Base64.getUrlDecoder();

            Map<String, Object> header = mapper.readValue(
                    decoder.decode(parts[0]), new TypeReference<Map<String, Object>>() { });
            if (!"HS256".equals(header.get("alg"))) {
                return Optional.empty();
            }

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] expected = mac.doFinal((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8));
            byte[] actual = decoder.decode(parts[2]);
            if (!MessageDigest.isEqual(expected, actual)) {
                return Optional.empty();
            }

            Map<String, Object> claims = mapper.readValue(
                    decoder.decode(parts[1]), new TypeReference<Map<String, Object>>() { });
            Object exp = claims.get("exp");
            if (exp instanceof Number n && n.longValue() < Instant.now().getEpochSecond()) {
                return Optional.empty();
            }
            Object role = claims.get("role");
            return role instanceof String s ? Optional.of(s) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
