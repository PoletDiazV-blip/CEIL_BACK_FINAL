package ceil.dao;

import ceil.database.DatabaseConfig;
import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class UsuarioDaoImpl implements UsuarioDao {

    @Override
    public int registrarUsuario(String username, String contacto, String password) {
        String sql = "INSERT INTO usuarios (username, contacto, password) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, username);
            stmt.setString(2, contacto);
            stmt.setString(3, password);

            if (stmt.executeUpdate() == 0) return -1;
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                // El id se devuelve para que el onboarding (privacidad, categorías,
                // presupuesto) pueda escribir sobre el usuario recién creado.
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            System.err.println("DAO ERROR (registrar): " + e.getMessage());
            return -1;
        }
    }

    @Override
    public Map<String, Object> buscarUsuarioPorContacto(String contacto) {
        String sql = "SELECT id_usuario, username, contacto, password, rol, ingreso_total, " +
                     "puntos_racha, acepto_privacidad, periodo_dias, periodo_tipo " +
                     "FROM usuarios WHERE contacto = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, contacto);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> usuario = new HashMap<>();
                    usuario.put("id_usuario", rs.getInt("id_usuario"));
                    usuario.put("username", rs.getString("username"));
                    usuario.put("contacto", rs.getString("contacto"));
                    usuario.put("password", rs.getString("password"));
                    usuario.put("rol", rs.getString("rol"));
                    usuario.put("ingreso_total", rs.getBigDecimal("ingreso_total"));
                    usuario.put("puntos_racha", rs.getInt("puntos_racha"));
                    usuario.put("acepto_privacidad", rs.getBoolean("acepto_privacidad"));
                    usuario.put("periodo_dias", rs.getObject("periodo_dias"));
                    usuario.put("periodo_tipo", rs.getString("periodo_tipo"));
                    // El front necesita saber si mandar al usuario al onboarding o
                    // directo al dashboard.
                    usuario.put("onboarding_completo", rs.getObject("periodo_dias") != null);
                    return usuario;
                }
            }
        } catch (SQLException e) {
            System.err.println("DAO ERROR (buscar): " + e.getMessage());
        }
        return null;
    }

    @Override
    public Map<String, Object> obtenerPerfil(int idUsuario) {
        String sql = "SELECT id_usuario, username, contacto, rol, edad, sexo, ingreso_total, " +
                     "periodo_dias, periodo_tipo, puntos_racha, acepto_privacidad, fecha_registro " +
                     "FROM usuarios WHERE id_usuario = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> perfil = new HashMap<>();
                    perfil.put("id_usuario", rs.getInt("id_usuario"));
                    perfil.put("username", rs.getString("username"));
                    perfil.put("contacto", rs.getString("contacto"));
                    perfil.put("rol", rs.getString("rol"));
                    perfil.put("edad", rs.getObject("edad"));
                    perfil.put("sexo", rs.getString("sexo"));
                    perfil.put("ingreso_total", rs.getBigDecimal("ingreso_total"));
                    perfil.put("periodo_dias", rs.getObject("periodo_dias"));
                    perfil.put("periodo_tipo", rs.getString("periodo_tipo"));
                    perfil.put("puntos_racha", rs.getInt("puntos_racha"));
                    perfil.put("acepto_privacidad", rs.getBoolean("acepto_privacidad"));
                    perfil.put("fecha_registro", rs.getTimestamp("fecha_registro"));
                    return perfil;
                }
            }
        } catch (SQLException e) {
            System.err.println("DAO ERROR (perfil): " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean guardarPresupuesto(int idUsuario, BigDecimal ingresoTotal, int periodoDias, String periodoTipo) {
        String sql = "UPDATE usuarios SET ingreso_total = ?, periodo_dias = ?, periodo_tipo = ? WHERE id_usuario = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, ingresoTotal);
            stmt.setInt(2, periodoDias);
            stmt.setString(3, periodoTipo);
            stmt.setInt(4, idUsuario);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("DAO ERROR (presupuesto): " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean guardarConsentimiento(int idUsuario, boolean acepto) {
        String sql = "UPDATE usuarios SET acepto_privacidad = ? WHERE id_usuario = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, acepto);
            stmt.setInt(2, idUsuario);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("DAO ERROR (privacidad): " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean ajustarIngresoTotal(int idUsuario, BigDecimal delta) {
        // El ajuste se hace en SQL (`= ingreso_total + ?`) y no leyendo-modificando-
        // escribiendo desde Java, para que dos ajustes simultáneos no se pisen.
        String sql = "UPDATE usuarios SET ingreso_total = ingreso_total + ? WHERE id_usuario = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, delta);
            stmt.setInt(2, idUsuario);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("DAO ERROR (ajuste saldo): " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean actualizarDatosPersonales(int idUsuario, Integer edad, String sexo) {
        String sql = "UPDATE usuarios SET edad = ?, sexo = ? WHERE id_usuario = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (edad == null) stmt.setNull(1, Types.INTEGER); else stmt.setInt(1, edad);
            stmt.setString(2, sexo);
            stmt.setInt(3, idUsuario);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("DAO ERROR (datos personales): " + e.getMessage());
            return false;
        }
    }
}
