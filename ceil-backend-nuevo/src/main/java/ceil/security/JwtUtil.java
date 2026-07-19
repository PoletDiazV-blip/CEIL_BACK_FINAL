package ceil.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;

/**
 * Fase 7 — firma y verificación de los tokens JWT.
 *
 * El token lleva quién es el usuario (`uid`) y su rol. Con eso, los endpoints dejan
 * de confiar en el id que manda el cliente (que era el agujero IDOR: cualquiera podía
 * pedir `/api/dashboard/resumen/{cualquier_id}`); la identidad sale SIEMPRE del token.
 *
 * El secreto de firma sale de la variable de entorno `CEIL_JWT_SECRET`. En desarrollo,
 * si no está definida, se usa un valor por defecto (solo para no bloquear el arranque
 * local). En un servidor real, `CEIL_JWT_SECRET` debe existir y ser larga y aleatoria.
 */
public final class JwtUtil {

    private JwtUtil() {}

    private static final String SECRETO = resolverSecreto();
    private static final Algorithm ALGORITMO = Algorithm.HMAC256(SECRETO);
    private static final JWTVerifier VERIFICADOR = JWT.require(ALGORITMO).withIssuer("ceil").build();

    // 30 días. No hay refresh token (es un proyecto escolar); se prioriza que el
    // usuario no tenga que volver a entrar todo el tiempo. Al rotar el secreto,
    // todos los tokens vigentes quedan invalidados de golpe.
    private static final long VIGENCIA_MS = 30L * 24 * 60 * 60 * 1000;

    private static String resolverSecreto() {
        String s = System.getenv("CEIL_JWT_SECRET");
        if (s != null && !s.isBlank()) return s;
        System.err.println("AVISO: CEIL_JWT_SECRET no está definida; usando un secreto de desarrollo. " +
                "Define uno propio antes de desplegar.");
        return "ceil-dev-secret-cambiar-en-produccion-1234567890";
    }

    /** Emite un token para el usuario indicado. */
    public static String generar(int idUsuario, String rol) {
        Date ahora = new Date();
        return JWT.create()
                .withIssuer("ceil")
                .withSubject(String.valueOf(idUsuario))
                .withClaim("uid", idUsuario)
                .withClaim("rol", rol != null ? rol : "USER")
                .withIssuedAt(ahora)
                .withExpiresAt(new Date(ahora.getTime() + VIGENCIA_MS))
                .sign(ALGORITMO);
    }

    /**
     * Verifica la firma y la vigencia del token.
     * @return el token decodificado, o null si es inválido, caducado o falsificado.
     */
    public static DecodedJWT verificar(String token) {
        try {
            return VERIFICADOR.verify(token);
        } catch (Exception e) {
            return null;
        }
    }
}
