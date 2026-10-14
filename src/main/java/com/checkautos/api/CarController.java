package com.checkautos.api;

import com.checkautos.AppContext;
import com.checkautos.models.Car;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/autos")
public class CarController {

    private final AppContext appContext;

    public CarController(AppContext appContext) {
        this.appContext = appContext;
    }


    @GetMapping
    public ResponseEntity<List<Car>> getAllCars() {
        return ResponseEntity.ok(appContext.getAllCars());
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getCarById(@PathVariable String id) {
        Car car = appContext.getCarManager().getCarById(id);
        if (car == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("error", "Auto no encontrado con id=" + id));
        }
        return ResponseEntity.ok(car);
    }


    @GetMapping("/buscar")
    public ResponseEntity<?> buscarAutos(@RequestParam String q) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Parámetro de búsqueda requerido"));
        }
        List<Car> resultados = appContext.getCarManager().searchCars(q.trim());
        return ResponseEntity.ok(Map.of("resultados", resultados, "total", resultados.size()));
    }


    /**
     * Registro de auto atómico y sin estado (REST Stateless).
     */
    @PostMapping
    public ResponseEntity<?> registrarAuto(@RequestBody Car car) {
        if (car == null || car.getMarca() == null || car.getMarca().isBlank()
                || car.getModelo() == null || car.getModelo().isBlank()
                || car.getPlaca() == null || car.getPlaca().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Marca, modelo y placa son campos obligatorios"));
        }
        Car guardado = appContext.getCarManager().createAndSaveCar(car);
        return ResponseEntity.status(201).body(Map.of(
                "mensaje", "Auto registrado y guardado exitosamente",
                "auto", guardado
        ));
    }


    @PostMapping("/nuevo")
    public ResponseEntity<?> nuevoCar() {
        appContext.startNewCarRegistration();
        return ResponseEntity.ok(Map.of(
                "mensaje", "Nuevo registro iniciado",
                "auto", appContext.getCurrentCar()
        ));
    }


    @PutMapping("/actual")
    public ResponseEntity<?> actualizarAutoActual(@RequestBody Map<String, Object> body) {
        Car car = appContext.getCurrentCar();
        if (car == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "No hay auto en edición. Llama primero a POST /api/autos/nuevo"));
        }

        if (body.containsKey("marca"))           car.setMarca((String) body.get("marca"));
        if (body.containsKey("modelo"))          car.setModelo((String) body.get("modelo"));
        if (body.containsKey("año")) {
            String anioStr = (String) body.get("año");
            try {
                if (anioStr != null && Integer.parseInt(anioStr.trim()) < 0) {
                    return ResponseEntity.badRequest().body(Map.of("error", "El año no puede ser negativo"));
                }
            } catch (NumberFormatException ignored) {}
            car.setAño(anioStr);
        }
        if (body.containsKey("color"))           car.setColor((String) body.get("color"));
        if (body.containsKey("placa"))           car.setPlaca((String) body.get("placa"));
        if (body.containsKey("kilometraje")) {
            String kmStr = (String) body.get("kilometraje");
            try {
                if (kmStr != null && Double.parseDouble(kmStr.trim().replaceAll("[^\\d.-]", "")) < 0) {
                    return ResponseEntity.badRequest().body(Map.of("error", "El kilometraje no puede ser negativo"));
                }
            } catch (NumberFormatException ignored) {}
            car.setKilometraje(kmStr);
        }
        if (body.containsKey("transmision"))     car.setTransmision((String) body.get("transmision"));
        if (body.containsKey("combustible"))     car.setCombustible((String) body.get("combustible"));
        if (body.containsKey("propietario"))     car.setPropietario((String) body.get("propietario"));
        if (body.containsKey("cedula"))          car.setCedula((String) body.get("cedula"));
        if (body.containsKey("nPropietarios"))   car.setNPropietarios((String) body.get("nPropietarios"));
        if (body.containsKey("precio")) {
            int p = ((Number) body.get("precio")).intValue();
            if (p < 0) return ResponseEntity.badRequest().body(Map.of("error", "El precio no puede ser negativo"));
            car.setPrecio(p);
        }
        if (body.containsKey("fechaRegistro"))
            car.setFechaRegistro((String) body.get("fechaRegistro"));
        if (body.containsKey("observacionRevision"))
            car.setObservacionRevision((String) body.get("observacionRevision"));
        if (body.containsKey("observacionMecanica"))
            car.setObservacionMecanica((String) body.get("observacionMecanica"));

        if (body.containsKey("puntajeTecnico")) {
            int pt = ((Number) body.get("puntajeTecnico")).intValue();
            if (pt < 0) return ResponseEntity.badRequest().body(Map.of("error", "El puntaje técnico no puede ser negativo"));
            car.setPuntajeTecnico(pt);
        }
        if (body.containsKey("puntajeMecanico")) {
            int pm = ((Number) body.get("puntajeMecanico")).intValue();
            if (pm < 0) return ResponseEntity.badRequest().body(Map.of("error", "El puntaje mecánico no puede ser negativo"));
            car.setPuntajeMecanico(pm);
        }

        return ResponseEntity.ok(Map.of("mensaje", "Auto actualizado", "auto", car));
    }


    @PostMapping("/publicar")
    public ResponseEntity<?> publicarAuto() {
        Car car = appContext.getCurrentCar();
        if (car == null || car.getMarca() == null || car.getMarca().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Completa los datos del auto antes de publicar"));
        }
        boolean ok = appContext.saveAndPublishCar();
        if (!ok) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Verifica que marca, modelo y placa estén completos"));
        }
        return ResponseEntity.ok(Map.of("mensaje", "Auto publicado y guardado en MongoDB"));
    }


    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable String id,
                                           @RequestBody Map<String, String> body) {
        String nuevoEstado = body.get("estado");
        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Campo 'estado' requerido"));
        }
        List<String> estadosValidos = List.of("Disponible", "Vendido", "En revisión");
        if (!estadosValidos.contains(nuevoEstado)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Estado inválido. Opciones: " + estadosValidos));
        }
        boolean ok = appContext.getCarManager().changeCarStatus(id, nuevoEstado);
        if (!ok) {
            return ResponseEntity.status(404)
                    .body(Map.of("error", "Auto no encontrado con id=" + id));
        }
        return ResponseEntity.ok(Map.of("mensaje", "Estado actualizado a '" + nuevoEstado + "'"));
    }


    @PatchMapping("/{id}/archivar")
    public ResponseEntity<?> archivarAuto(@PathVariable String id) {
        boolean ok = appContext.getCarManager().archivarAuto(id);
        if (!ok) return ResponseEntity.status(404).body(Map.of("error", "Auto no encontrado"));
        return ResponseEntity.ok(Map.of("mensaje", "Auto archivado correctamente"));
    }


    @PatchMapping("/{id}/restaurar")
    public ResponseEntity<?> restaurarAuto(@PathVariable String id) {
        boolean ok = appContext.getCarManager().restaurarAuto(id);
        if (!ok) return ResponseEntity.status(404).body(Map.of("error", "Auto no encontrado"));
        return ResponseEntity.ok(Map.of("mensaje", "Auto restaurado correctamente"));
    }


    @GetMapping("/archivados")
    public ResponseEntity<?> getArchivados() {
        return ResponseEntity.ok(appContext.getCarManager().getAutosArchivados());
    }


    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        var cm = appContext.getCarManager();
        return ResponseEntity.ok(Map.of(
                "total",       cm.getTotalCars(),
                "disponibles", cm.getAvailableCars(),
                "vendidos",    cm.getSoldCars(),
                "enRevision",  cm.getCarsInReview()
        ));
    }
}