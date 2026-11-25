package com.carmotors.vista;

import com.carmotors.excepcion.ExportacionException;
import java.nio.file.Path;
import com.carmotors.controlador.ReporteController;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.FilaReporteDTO;
import com.carmotors.modelo.OrdenReporte;
import com.carmotors.modelo.ReporteDTO;
import com.carmotors.modelo.TipoReporte;

import java.util.Objects;

/** Vista de solo lectura: las estadísticas se calculan en las estrategias de negocio. */
public final class VistaReportes {
    private final Consola consola;
    private final ReporteController controlador;

    public VistaReportes(Consola consola, ReporteController controlador) {
        this.consola = Objects.requireNonNull(consola);
        this.controlador = Objects.requireNonNull(controlador);
    }

    public void mostrar(SesionConsola sesion) {
        Objects.requireNonNull(sesion, "La sesión es obligatoria");
        if (sesion.rol() != SesionConsola.Rol.ADMINISTRADOR) {
            throw new IllegalArgumentException("Se requiere rol administrativo");
        }
        while (true) {
            consola.titulo("CARMOTORS | REPORTES E INDICADORES");
            consola.mostrar("1. Servicios solicitados por tipo de mantenimiento");
            consola.mostrar("2. Productividad: participaciones por técnico");
            consola.mostrar("3. Consumo de repuestos (unidades)");
            consola.mostrar("0. Regresar");
            String opcion = consola.leerOpcion("Opción", "1", "2", "3", "0");
            if (opcion == null || "0".equals(opcion)) return;
            TipoReporte tipo = switch (opcion) {
                case "1" -> TipoReporte.SERVICIOS_SOLICITADOS;
                case "2" -> TipoReporte.PRODUCTIVIDAD_TECNICOS;
                case "3" -> TipoReporte.CONSUMO_REPUESTOS;
                default -> throw new IllegalStateException("Opción ya validada");
            };
            ejecutarConsulta(tipo);
        }
    }

    private void ejecutarConsulta(TipoReporte tipo) {
        consola.mostrar("Escribe 0 en cualquier pregunta para cancelar.");
        String inicio = leerDato("Fecha inicial (AAAA-MM-DD)");
        if (inicio == null) return;
        String fin = leerDato("Fecha final (AAAA-MM-DD)");
        if (fin == null) return;
        consola.mostrar("Orden: 1. Mayor cantidad | 2. Menor cantidad | 3. Nombre A-Z | 0. Cancelar");
        String opcion = consola.leerOpcion("Orden", "1", "2", "3", "0");
        if (opcion == null || "0".equals(opcion)) return;
        OrdenReporte orden = switch (opcion) {
            case "1" -> OrdenReporte.CANTIDAD_DESC;
            case "2" -> OrdenReporte.CANTIDAD_ASC;
            case "3" -> OrdenReporte.NOMBRE_ASC;
            default -> throw new IllegalStateException("Opción ya validada");
        };
        try {
            ReporteDTO reporte = controlador.generar(tipo, inicio, fin, orden);

            imprimir(reporte);

            exportarReporte(reporte);
        } catch (ValidacionException | IllegalArgumentException | IllegalStateException error) {
            consola.mostrar("Error: " + error.getMessage());
        } catch (RuntimeException error) {
            // Los errores JDBC ya fueron envueltos por la capa DAO/Service.
            consola.mostrar("No se pudo generar el reporte: " + error.getMessage());
        }
        consola.esperar();
    }

    private String leerDato(String etiqueta) {
        String valor = consola.leerTexto(etiqueta);
        return valor == null || "0".equals(valor) ? null : valor;
    }

    private void imprimir(ReporteDTO reporte) {
        consola.titulo(reporte.titulo());
        consola.mostrar("Periodo: " + reporte.filtro().fechaInicio() + " a "
                + reporte.filtro().fechaFin());
        String unidad = switch (reporte.tipo()) {
            case SERVICIOS_SOLICITADOS -> "órdenes";
            case PRODUCTIVIDAD_TECNICOS -> "participaciones";
            case CONSUMO_REPUESTOS -> "unidades";
        };
        if (reporte.filas().isEmpty()) {
            consola.mostrar("No hay datos para el periodo seleccionado.");
            return;
        }
        consola.mostrar("Resultado | " + unidad);
        for (FilaReporteDTO fila : reporte.filas()) {
            consola.mostrar(fila.nombre() + " [" + fila.codigo() + "] | " + fila.cantidad());
        }
        // El total solo suma magnitudes homogéneas del mismo reporte.
        long total = reporte.filas().stream().mapToLong(FilaReporteDTO::cantidad).sum();
        consola.mostrar("Total de " + unidad + ": " + total);
    }
    private void exportarReporte(ReporteDTO reporte) {

        boolean confirmar = consola.confirmar(
                "¿Deseas exportar el reporte en formato TXT?"
        );

        if (!confirmar) {
            return;
        }

        try {

            Path directorio = Path.of("reportes");

            Path archivo = controlador.exportar(
                    reporte,
                    directorio
            );

            consola.mostrar("[OK] Reporte exportado correctamente.");

            consola.mostrar("Archivo guardado en:");

            consola.mostrar(archivo.toString());

        } catch (ExportacionException e) {

            consola.mostrar("[ERROR] " + e.getMessage());
        }
    }
}
