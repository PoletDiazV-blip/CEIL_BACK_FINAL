package ceil.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConfig {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(requerido("CEIL_DB_URL"));
        config.setUsername(requerido("CEIL_DB_USER"));
        config.setPassword(requerido("CEIL_DB_PASSWORD"));

        // Optimizaciones de Hikari
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");

        dataSource = new HikariDataSource(config);
    }

    private static String requerido(String variable) {
        String valor = System.getenv(variable);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(
                    "Falta la variable de entorno " + variable + ". " +
                    "Copia CeilBakend/.env.example a CeilBakend/.env y rellena las credenciales.");
        }
        return valor;
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
