package com.carmotors.controlador;


import com.carmotors.servicio.ExportadorReporteTxt;
import java.nio.file.Path;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.FiltroReporte;
import com.carmotors.modelo.OrdenReporte;
import com.carmotors.modelo.ReporteDTO;
import com.carmotors.modelo.TipoReporte;
import com.carmotors.servicio.ReporteStrategy;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Adapta los filtros ingresados en consola y delega las estadísticas a las estrategias existentes. */
public final class ReporteController {
    private final Map<TipoReporte, ReporteStrategy> estrategias = new EnumMap<>(TipoReporte.class);
    private final ExportadorReporteTxt exportador =
            new ExportadorReporteTxt();

    public ReporteController(List<ReporteStrategy> estrategias) {
        Objects.requireNonNull(estrategias, "Las estrategias son obligatorias");
        for (ReporteStrategy estrategia : estrategias) {
            Objects.requireNonNull(estrategia, "No se admite una estrategia nula");
            TipoReporte tipo = Objects.requireNonNull(estrategia.tipo(), "El tipo del reporte es obligatorio");
            if (this.estrategias.putIfAbsent(tipo, estrategia) != null) {
                throw new IllegalArgumentException("Existe más de una estrategia para " + tipo);
            }
        }
    }

    /** Las fechas son inclusivas y deben tener formato ISO AAAA-MM-DD. */
    public ReporteDTO generar(TipoReporte tipo, String fechaInicio, String fechaFin, OrdenReporte orden) {
        Objects.requireNonNull(tipo, "Debes indicar el tipo de reporte");
        Objects.requireNonNull(orden, "Debes indicar cómo ordenar los resultados");
        ReporteStrategy estrategia = estrategias.get(tipo);
        if (estrategia == null) {
            throw new ValidacionException("El reporte solicitado no está configurado.");
        }
        FiltroReporte filtro = new FiltroReporte(
                convertirFecha(fechaInicio, "inicial"),
                convertirFecha(fechaFin, "final"), orden);
        return estrategia.generar(filtro);
    }

    private LocalDate convertirFecha(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            throw new ValidacionException("La fecha " + nombre + " es obligatoria.");
        }
        try {
            return LocalDate.parse(valor.strip());
        } catch (DateTimeParseException e) {
            throw new ValidacionException("La fecha " + nombre + " debe tener formato AAAA-MM-DD.");
        }

    }
    public Path exportar(ReporteDTO reporte, Path directorio) {

        return exportador.exportar(reporte, directorio);
    }
}
