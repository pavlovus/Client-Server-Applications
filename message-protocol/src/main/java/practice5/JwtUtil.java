package practice5;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.util.Date;

public class JwtUtil {
    // Утиліта для створення верифікації jwt токенів, якщо що, я розумію, що в реальному проєкті це не можна було б зберігати прямо в репозиторії в відкритому вигляді
    private static final String SECRET = "my-super-secret-key-for-warehouse-app";
    private static final Algorithm algorithm = Algorithm.HMAC256(SECRET);
    private static final JWTVerifier verifier = JWT.require(algorithm).withIssuer("warehouse-server").build();
    private static final long EXPIRATION_TIME_MS = 3600 * 1000;

    public static String generateToken(String username) {
        return JWT.create().withIssuer("warehouse-server").withSubject(username).withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME_MS)).sign(algorithm);
    }

    public static DecodedJWT verifyToken(String token) {
        return verifier.verify(token);
    }
}