package vn.iotstar.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        JWTClaimsSet claimsSet = parseAndVerifyToken(token);
        return claimsSet != null ? claimsSet.getSubject() : null;
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        try {
            byte[] secretBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            JWSSigner signer = new MACSigner(secretBytes);
            JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);

            long now = System.currentTimeMillis();
            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .subject(userDetails.getUsername())
                    .issueTime(new Date(now))
                    .expirationTime(new Date(now + expiration));

            if (extraClaims != null) {
                extraClaims.forEach(claimsBuilder::claim);
            }

            SignedJWT signedJWT = new SignedJWT(header, claimsBuilder.build());
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Error generating JWT token", e);
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        JWTClaimsSet claimsSet = parseAndVerifyToken(token);
        if (claimsSet == null) {
            return false;
        }
        String username = claimsSet.getSubject();
        return username != null && username.equals(userDetails.getUsername());
    }

    /**
     * Parses and verifies token using Nimbus JOSE + JWT.
     * Verifies Signature, HS256 Algorithm, and Expiration.
     */
    public JWTClaimsSet parseAndVerifyToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            // 1. Check algorithm
            if (!JWSAlgorithm.HS256.equals(signedJWT.getHeader().getAlgorithm())) {
                return null;
            }

            // 2. Verify signature using secret key
            byte[] secretBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            JWSVerifier verifier = new MACVerifier(secretBytes);
            if (!signedJWT.verify(verifier)) {
                return null;
            }

            // 3. Verify expiration time
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            Date expiration = claimsSet.getExpirationTime();
            if (expiration == null || expiration.before(new Date())) {
                return null;
            }

            return claimsSet;
        } catch (Exception e) {
            return null;
        }
    }
}
