package com.checkautos.api;

import com.checkautos.prediction.PredictionManager;
import com.checkautos.prediction.PredictionRequest;
import com.checkautos.prediction.PredictionResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/prediccion")
public class PredictionController {

    private final PredictionManager predictionManager;

    public PredictionController(PredictionManager predictionManager) {
        this.predictionManager = predictionManager;
    }

    // ------------------------------------------------------------------
    // POST /api/prediccion/entrenar
    // Entrena el modelo con los autos de la base de datos.
    // ------------------------------------------------------------------
    @PostMapping("/entrenar")
    public ResponseEntity<?> entrenar() {
        if (!predictionManager.isTrained()) {
            int n = predictionManager.trainModel();
            if (n < 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Datos insuficientes. Se necesitan al menos " +
                              (com.checkautos.prediction.PredictionModel.FEATURE_COUNT + 1) +
                              " autos con precio, puntajes y datos completos."
                ));
            }
        } else {
            predictionManager.retrainModel();
        }

        return ResponseEntity.ok(buildEstado());
    }

    // ------------------------------------------------------------------
    // POST /api/prediccion/reentrenar
    // Fuerza reentrenamiento aunque el modelo ya esté listo.
    // ------------------------------------------------------------------
    @PostMapping("/reentrenar")
    public ResponseEntity<?> reentrenar() {
        int n = predictionManager.retrainModel();
        if (n < 0) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Datos insuficientes para reentrenar."
            ));
        }
        return ResponseEntity.ok(buildEstado());
    }

    // ------------------------------------------------------------------
    // POST /api/prediccion/predecir
    // Predice el precio de un auto con los datos del body.
    // ------------------------------------------------------------------
    @PostMapping("/predecir")
    public ResponseEntity<?> predecir(@RequestBody PredictionRequest req) {
        if (!predictionManager.isTrained()) {
            int n = predictionManager.trainModel();
            if (n < 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Modelo no entrenado y datos insuficientes en la base de datos."
                ));
            }
        }

        try {
            PredictionResult result = predictionManager.predict(
                req.getAño(),
                req.getKilometraje(),
                req.getPuntajeTecnico(),
                req.getPuntajeMecanico(),
                req.getNPropietarios(),
                req.getTransmision(),
                req.getCombustible()
            );

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("precioPredicho",       result.getPrecioPredicho());
            resp.put("precioFormateado",      result.getPrecioFormateado());
            resp.put("confianza",             result.getConfianza());
            resp.put("r2",                    result.getR2());
            resp.put("margenErrorPromedio",   result.getMargenErrorPromedio());
            resp.put("estadoModelo",          result.getEstadoModelo());
            return ResponseEntity.ok(resp);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    // GET /api/prediccion/estado
    // Devuelve el estado actual del modelo.
    // ------------------------------------------------------------------
    @GetMapping("/estado")
    public ResponseEntity<?> estado() {
        return ResponseEntity.ok(buildEstado());
    }

    // ------------------------------------------------------------------
    // GET /api/prediccion/pesos
    // Devuelve los pesos del modelo para inspección / debugging.
    // ------------------------------------------------------------------
    @GetMapping("/pesos")
    public ResponseEntity<?> pesos() {
        if (!predictionManager.isTrained()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Modelo no entrenado."));
        }

        PredictionManager.ModelWeightsInfo info = predictionManager.getWeightsInfo();
        String[] nombres = {"edad", "kilometraje", "puntajeTecnico", "puntajeMecanico",
                            "nPropietarios", "automatica", "verdeEnergia"};

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("bias", info.bias());
        Map<String, Double> pesosMap = new LinkedHashMap<>();
        for (int i = 0; i < nombres.length; i++) {
            pesosMap.put(nombres[i], info.weights()[i]);
        }
        resp.put("pesos", pesosMap);
        return ResponseEntity.ok(resp);
    }

    // ------------------------------------------------------------------
    private Map<String, Object> buildEstado() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("entrenado",      predictionManager.isTrained());
        m.put("muestrasUsadas", predictionManager.getSamplesUsed());
        m.put("r2",             predictionManager.getR2Score());
        m.put("maePromedio",    predictionManager.getMaeScore());
        m.put("ultimoEntrenamiento",
              predictionManager.getLastTrainedAt() != null
                  ? predictionManager.getLastTrainedAt().toString()
                  : null);
        return m;
    }
}
