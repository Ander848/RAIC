package com.checkautos.prediction;

import com.checkautos.models.Car;
import com.checkautos.repository.CarRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio que convierte autos de la base de datos en features,
 * entrena el PredictionModel y expone predicciones.
 *
 * Se entrena automáticamente al arrancar la app y puede reentrenarse
 * en caliente vía retrainModel().
 */
@Service
public class PredictionManager {

    private final CarRepository  carRepository;
    private final PredictionModel model;

    private LocalDateTime lastTrainedAt;
    private int           samplesUsed;
    private double        r2Score;
    private double        maeScore;

    public PredictionManager(CarRepository carRepository) {
        this.carRepository = carRepository;
        this.model         = new PredictionModel();
    }

    // -----------------------------------------------------------------------
    // Entrenamiento
    // -----------------------------------------------------------------------

    /**
     * Entrena el modelo con todos los autos que tengan precio > 0
     * y ambos puntajes ingresados.
     *
     * @return número de muestras usadas, o -1 si no había suficientes datos.
     */
    public int trainModel() {
        List<Car> autos = carRepository.findByArchivadoFalseOrderByIdDesc();

        List<double[]> features = new ArrayList<>();
        List<Double>   labels   = new ArrayList<>();

        for (Car car : autos) {
            if (car.getPrecio() <= 0) continue;
            if (car.getPuntajeTecnico() == 0 && car.getPuntajeMecanico() == 0) continue;

            double[] f = extractFeatures(car);
            if (f == null) continue;

            features.add(f);
            labels.add((double) car.getPrecio());
        }

        if (features.size() < PredictionModel.FEATURE_COUNT + 1) {
            return -1;
        }

        model.train(features, labels);
        samplesUsed   = features.size();
        r2Score       = model.computeR2(features, labels);
        maeScore      = model.computeMAE(features, labels);
        lastTrainedAt = LocalDateTime.now();
        return samplesUsed;
    }

    /** Alias conveniente para reentrenar en caliente. */
    public int retrainModel() { return trainModel(); }

    // -----------------------------------------------------------------------
    // Predicción
    // -----------------------------------------------------------------------

    /**
     * Predice el precio de un auto a partir de sus datos directos.
     *
     * @param año            año de fabricación (string "2018", etc.)
     * @param kilometraje    kilometraje como string ("85000" o "85,000")
     * @param puntajeTecnico 0-100
     * @param puntajeMecanico 0-100
     * @param nPropietarios  número de propietarios como string
     * @param transmision    "Automática" / "Manual"
     * @param combustible    "Eléctrico" / "Híbrido" / otro
     * @return resultado de predicción con precio y confianza
     */
    public PredictionResult predict(String año,
                                    String kilometraje,
                                    int    puntajeTecnico,
                                    int    puntajeMecanico,
                                    String nPropietarios,
                                    String transmision,
                                    String combustible) {
        double[] f = buildFeatures(año, kilometraje, puntajeTecnico,
                                   puntajeMecanico, nPropietarios,
                                   transmision, combustible);
        double precio = model.predict(f);
        return buildResult(precio, puntajeTecnico, puntajeMecanico);
    }

    /** Versión que acepta directamente un Car ya construido. */
    public PredictionResult predictFromCar(Car car) {
        double[] f = extractFeatures(car);
        if (f == null) throw new IllegalArgumentException("Datos insuficientes para predecir.");
        double precio = model.predict(f);
        return buildResult(precio, car.getPuntajeTecnico(), car.getPuntajeMecanico());
    }

    // -----------------------------------------------------------------------
    // Estado del modelo
    // -----------------------------------------------------------------------

    public boolean isTrained()          { return model.isTrained(); }
    public int     getSamplesUsed()     { return samplesUsed; }
    public double  getR2Score()         { return r2Score; }
    public double  getMaeScore()        { return maeScore; }
    public LocalDateTime getLastTrainedAt() { return lastTrainedAt; }

    /** Devuelve los pesos del modelo para poder inspeccionarlos. */
    public ModelWeightsInfo getWeightsInfo() {
        if (!model.isTrained()) return null;
        return new ModelWeightsInfo(
            model.getBias(),
            model.getWeights()
        );
    }

    // -----------------------------------------------------------------------
    // Extracción de features
    // -----------------------------------------------------------------------

    private double[] extractFeatures(Car car) {
        return buildFeatures(
            car.getAño(),
            car.getKilometraje(),
            car.getPuntajeTecnico(),
            car.getPuntajeMecanico(),
            car.getNPropietarios(),
            car.getTransmision(),
            car.getCombustible()
        );
    }

    private double[] buildFeatures(String año,
                                   String kilometraje,
                                   int    puntajeTecnico,
                                   int    puntajeMecanico,
                                   String nPropietarios,
                                   String transmision,
                                   String combustible) {
        double edad = parseEdad(año);
        double km   = parseKilometraje(kilometraje);
        if (edad < 0 || km < 0) return null;

        double propietarios   = parsePropietarios(nPropietarios);
        double esAutomatica   = isAutomatica(transmision) ? 1.0 : 0.0;
        double esVerdeEnergia = isVerdeEnergia(combustible) ? 1.0 : 0.0;

        return new double[]{
            edad,
            km,
            puntajeTecnico,
            puntajeMecanico,
            propietarios,
            esAutomatica,
            esVerdeEnergia
        };
    }

    // -----------------------------------------------------------------------
    // Helpers de parsing
    // -----------------------------------------------------------------------

    private double parseEdad(String año) {
        try {
            int y = Integer.parseInt(año.trim());
            int edad = LocalDate.now().getYear() - y;
            return edad < 0 ? -1 : edad;
        } catch (Exception e) { return -1; }
    }

    private double parseKilometraje(String km) {
        if (km == null || km.isBlank()) return 0;
        try {
            return Double.parseDouble(km.trim().replaceAll("[^\\d.]", ""));
        } catch (Exception e) { return 0; }
    }

    private double parsePropietarios(String n) {
        if (n == null || n.isBlank()) return 1;
        try { return Double.parseDouble(n.trim()); }
        catch (Exception e) { return 1; }
    }

    private boolean isAutomatica(String transmision) {
        return transmision != null &&
               transmision.toLowerCase().contains("autom");
    }

    private boolean isVerdeEnergia(String combustible) {
        if (combustible == null) return false;
        String c = combustible.toLowerCase();
        return c.contains("eléctr") || c.contains("electr") || c.contains("híbrid") || c.contains("hibrid");
    }

    // -----------------------------------------------------------------------
    // Construcción del resultado
    // -----------------------------------------------------------------------

    private PredictionResult buildResult(double precio, int pt, int pm) {
        int precioRedondeado = (int) Math.round(precio / 100_000.0) * 100_000;

        String confianza;
        if (r2Score >= 0.85)      confianza = "Alta";
        else if (r2Score >= 0.65) confianza = "Media";
        else                      confianza = "Baja";

        int margenError = (int) Math.round(maeScore);

        return new PredictionResult(
            precioRedondeado,
            precio,
            confianza,
            r2Score,
            margenError,
            getModelStatus()
        );
    }

    private String getModelStatus() {
        if (!model.isTrained()) return "No entrenado";
        return String.format("Entrenado con %d muestras — R²=%.2f", samplesUsed, r2Score);
    }

    // -----------------------------------------------------------------------
    // Inner classes de datos de retorno
    // -----------------------------------------------------------------------

    public record ModelWeightsInfo(
        double bias,
        double[] weights
    ) {}
}
