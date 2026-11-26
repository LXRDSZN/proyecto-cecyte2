package com.mycompany.starbucks.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    private static final String DEFAULT_URL = "jdbc:mariadb://"
            + configurar("db.host", "DB_HOST", "localhost") + ":"
            + configurar("db.port", "DB_PORT", "3306") + "/"
            + configurar("db.name", "DB_NAME", "starbucks_prenomina");
    private static final String URL = configuredUrl();
    private static final String USER = configurar("db.user", "DB_USER", "root");
    private static final String PASSWORD = configurar("db.password", "DB_PASSWORD", "");

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static String getUrl() {
        return URL;
    }

    private static String configurar(String propiedad, String variable, String porDefecto) {
        String valor = System.getProperty(propiedad);
        if (valor != null) {
            return valor;
        }
        return System.getenv().getOrDefault(variable, porDefecto);
    }

    private static String configuredUrl() {
        String explicitUrl = configurar("db.url", "DB_URL", "");
        if (explicitUrl != null && !explicitUrl.trim().isEmpty()) {
            return explicitUrl;
        }

        String socket = System.getenv("DB_SOCKET");
        if (socket != null && !socket.trim().isEmpty()) {
            return DEFAULT_URL + "?localSocket=" + socket;
        }

        return DEFAULT_URL;
    }
}
