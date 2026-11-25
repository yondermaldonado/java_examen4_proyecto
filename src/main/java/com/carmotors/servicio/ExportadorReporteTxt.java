package com.carmotors.servicio;

import com.carmotors.excepcion.ExportacionException;
import com.carmotors.modelo.FilaReporteDTO;
import com.carmotors.modelo.ReporteDTO;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class ExportadorReporteTxt {

    public String contenido(ReporteDTO reporte) {

        Objects.requireNonNull(reporte);

        String unidad = switch (reporte.tipo()) {
            case SERVICIOS_SOLICITADOS -> "órdenes";
            case PRODUCTIVIDAD_TECNICOS -> "participaciones";
            case CONSUMO_REPUESTOS -> "unidades";
        };

        StringBuilder texto = new StringBuilder();

        // Encabezado del reporte.
        texto.append("========================================\n");
        texto.append("              CARMOTORS\n");
        texto.append("           REPORTE DETALLADO\n");
        texto.append("========================================\n\n");

        texto.append("Reporte: ")
                .append(reporte.titulo()).append("\n");

        texto.append("Fecha inicial: ")
                .append(reporte.filtro().fechaInicio()).append("\n");

        texto.append("Fecha final: ")
                .append(reporte.filtro().fechaFin()).append("\n");

        texto.append("Unidad: ")
                .append(unidad).append("\n");

        texto.append("\nDETALLE DEL REPORTE\n");
        texto.append("----------------------------------------\n");

        // Validar si existen resultados.
        if (reporte.filas().isEmpty()) {
            texto.append("No hay datos para el periodo seleccionado.\n");
        }

        // Recorrer los resultados.
        for (FilaReporteDTO fila : reporte.filas()) {

            texto.append("Código: ")
                    .append(fila.codigo()).append("\n");

            texto.append("Nombre: ")
                    .append(fila.nombre()).append("\n");

            texto.append("Cantidad: ")
                    .append(fila.cantidad()).append("\n\n");
        }

        // Calcular el total.
        long total = reporte.filas()
                .stream()
                .mapToLong(FilaReporteDTO::cantidad)
                .sum();

        texto.append("----------------------------------------\n");

        texto.append("Total de registros: ")
                .append(reporte.filas().size()).append("\n");

        texto.append("Total de ")
                .append(unidad)
                .append(": ")
                .append(total).append("\n");

        texto.append("========================================\n");

        return texto.toString();
    }

    public Path exportar(ReporteDTO reporte, Path directorio) {

        Path temporal = null;

        try {

            // Obtener la ruta absoluta de la carpeta.
            Path carpeta = directorio.toAbsolutePath().normalize();

            // Crear la carpeta si no existe.
            Files.createDirectories(carpeta);

            // Generar un nombre con fecha y hora.
            String fecha = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
            );

            String nombreArchivo =
                    reporte.tipo().name().toLowerCase(java.util.Locale.ROOT)
                            + "-" + fecha + ".txt";

            // Construir la ruta del archivo final.
            Path destino = carpeta.resolve(nombreArchivo);

            // Crear un archivo temporal.
            temporal = Files.createTempFile(
                    carpeta, ".reporte-", ".tmp"
            );

            // Escribir todo el contenido.
            Files.writeString(
                    temporal,
                    contenido(reporte),
                    StandardCharsets.UTF_8
            );

            // Mover el archivo temporal al destino final.
            Files.move(temporal, destino);

            return destino;

        } catch (IOException | SecurityException e) {

            throw new ExportacionException(
                    "No se pudo exportar el reporte TXT.",
                    e
            );

        } finally {

            if (temporal != null) {

                try {
                    Files.deleteIfExists(temporal);

                } catch (IOException | SecurityException ignored) {
                }
            }
        }
    }
}