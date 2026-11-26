package com.mycompany.starbucks.service;

import com.mycompany.starbucks.dao.ReporteDAO;
import com.mycompany.starbucks.dao.ReporteDAO.Datos;
import com.mycompany.starbucks.dao.ReporteDAO.Filtros;
import com.mycompany.starbucks.model.Usuario;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.List;

public class ReporteService {
    private final ReporteDAO dao = new ReporteDAO();

    public Datos consultar(Usuario usuario, Filtros filtros) throws SQLException {
        if (usuario == null || !"ACTIVO".equals(usuario.getEstado())
                || !("ADMINISTRADOR".equals(usuario.getRol()) || "REVISOR".equals(usuario.getRol()))) {
            throw new IllegalArgumentException("Solo administradores y revisores pueden consultar reportes.");
        }
        return dao.consultar(filtros);
    }

    public void exportarCsv(Path destino, Datos datos) throws IOException {
        Path archivo = destino.toAbsolutePath();
        Path temporal = Files.createTempFile(archivo.getParent(), ".reporte-", ".csv");
        try {
            try (var salida = Files.newBufferedWriter(temporal, StandardCharsets.UTF_8)) {
                escribirFila(salida, datos.columnas());
                for (var fila : datos.filas()) escribirFila(salida, fila);
            }
            Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private void escribirFila(Writer salida, List<?> fila) throws IOException {
        for (int i = 0; i < fila.size(); i++) {
            if (i > 0) salida.write(',');
            Object valor = fila.get(i);
            String texto = valor == null ? "" : valor.toString();
            if (valor instanceof String && esFormula(texto)) texto = "'" + texto;
            salida.write('"');
            salida.write(texto.replace("\"", "\"\""));
            salida.write('"');
        }
        salida.write("\r\n");
    }

    private boolean esFormula(String texto) {
        String visible = texto.stripLeading();
        return (!texto.isEmpty() && "\t\r\n".indexOf(texto.charAt(0)) >= 0)
                || (!visible.isEmpty() && "=+-@".indexOf(visible.charAt(0)) >= 0);
    }
}
