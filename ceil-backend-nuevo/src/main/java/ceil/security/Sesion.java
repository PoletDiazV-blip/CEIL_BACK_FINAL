package ceil.security;

import io.javalin.http.Context;

/**
 * Fase 7 — acceso a la identidad autenticada de la petición.
 *
 * El filtro `before` de App.java valida el token y deja en el contexto el id y el rol
 * del usuario. Los controladores leen SIEMPRE de aquí (nunca del id que venga en la
 * ruta, el query o el cuerpo), de modo que nadie pueda actuar en nombre de otro.
 */
public final class Sesion {

    private Sesion() {}

    public static final String ATTR_UID = "uid";
    public static final String ATTR_ROL = "rol";

    /** El id del usuario dueño del token. Asume que el filtro de auth ya corrió. */
    public static int uid(Context ctx) {
        Integer uid = ctx.attribute(ATTR_UID);
        if (uid == null) throw new IllegalStateException("Petición sin usuario autenticado.");
        return uid;
    }

    public static String rol(Context ctx) {
        String rol = ctx.attribute(ATTR_ROL);
        return rol != null ? rol : "USER";
    }

    public static boolean esAdmin(Context ctx) {
        return "ADMIN".equalsIgnoreCase(rol(ctx));
    }
}
