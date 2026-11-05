package com.checkautos.api;

import com.checkautos.dto.InformeDetalleDTO;
import com.checkautos.reports.InformeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/informes")
public class InformeController {

    private final InformeService informeService;

    public InformeController(InformeService informeService) {
        this.informeService = informeService;
    }

    /**
     * Consulta el informe vehicular completo y del dueño por la placa.
     */
    @GetMapping("/consultar")
    public ResponseEntity<?> consultarInforme(@RequestParam(name = "placa") String placa) {
        if (placa == null || placa.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El parámetro 'placa' es obligatorio"));
        }

        InformeDetalleDTO informe = informeService.obtenerInformePorPlaca(placa);
        if (informe == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "encontrado", false,
                    "mensaje", "No se encontró ningún vehículo registrado con la placa: " + placa.trim().toUpperCase()
            ));
        }

        return ResponseEntity.ok(Map.of(
                "encontrado", true,
                "informe", informe
        ));
    }

    /**
     * Descarga el archivo de informe generado (PDF o TXT).
     */
    @GetMapping("/descargar/{placa}")
    public ResponseEntity<?> descargarInforme(@PathVariable String placa,
                                              @RequestParam(name = "formato", defaultValue = "pdf") String formato) {
        if (placa == null || placa.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Placa requerida"));
        }

        try {
            byte[] contenido = informeService.obtenerBytesArchivo(placa, formato);
            if (contenido == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "error", "No se pudo encontrar o generar el archivo para la placa " + placa
                ));
            }

            String ext = "txt".equalsIgnoreCase(formato) ? "txt" : "pdf";
            String contentType = "txt".equalsIgnoreCase(formato) ? "text/plain; charset=UTF-8" : "application/pdf";
            String nombreArchivo = "informe_" + placa.trim().toUpperCase().replaceAll("[^A-Z0-9-]", "") + "." + ext;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", nombreArchivo);
            headers.setContentLength(contenido.length);

            return new ResponseEntity<>(contenido, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Error al leer el archivo del informe: " + e.getMessage()
            ));
        }
    }

    /**
     * Lista los informes vehiculares recientes para acceso rápido.
     */
    @GetMapping("/recientes")
    public ResponseEntity<List<InformeDetalleDTO>> listarInformesRecientes(
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        int max = Math.max(1, Math.min(limit, 50));
        return ResponseEntity.ok(informeService.listarInformesRecientes(max));
    }
}
