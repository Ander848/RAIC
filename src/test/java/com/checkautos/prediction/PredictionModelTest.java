package com.checkautos.prediction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PredictionModelTest {

    private PredictionModel model;

    @BeforeEach
    void setUp() {
        model = new PredictionModel();
    }

    @Test
    void noEntrenadoLanzaExcepcion() {
        assertFalse(model.isTrained());
        assertThrows(IllegalStateException.class,
            () -> model.predict(new double[PredictionModel.FEATURE_COUNT]));
    }

    @Test
    void entrenaNormalmenteCon10Muestras() {
        List<double[]> features = new ArrayList<>();
        List<Double>   labels   = new ArrayList<>();

        double[][] datos = {
            {1, 10000,  90, 88, 1, 1, 0},
            {3, 35000,  80, 75, 1, 1, 0},
            {5, 60000,  70, 65, 2, 0, 0},
            {7, 85000,  60, 58, 2, 0, 0},
            {2, 20000,  85, 82, 1, 1, 1},
            {4, 50000,  75, 70, 1, 0, 0},
            {6, 72000,  65, 60, 3, 1, 0},
            {8, 100000, 50, 48, 2, 0, 0},
            {10,120000, 40, 38, 3, 0, 0},
            {3, 30000,  88, 85, 1, 1, 1},
            {9, 110000, 45, 42, 2, 1, 0},
            {2, 25000,  92, 90, 1, 0, 1},
        };
        double[] precios = {
            75_000_000, 60_000_000, 50_000_000, 42_000_000,
            70_000_000, 55_000_000, 45_000_000, 35_000_000,
            28_000_000, 65_000_000, 30_000_000, 72_000_000,
        };

        for (int i = 0; i < datos.length; i++) {
            features.add(datos[i]);
            labels.add(precios[i]);
        }

        assertDoesNotThrow(() -> model.train(features, labels));
        assertTrue(model.isTrained());
    }

    @Test
    void prediccionRazonable() {
        List<double[]> features = new ArrayList<>();
        List<Double>   labels   = new ArrayList<>();

        double[][] datos = {
            {1, 10000,  90, 88, 1, 1, 0},
            {3, 35000,  80, 75, 1, 1, 0},
            {5, 60000,  70, 65, 2, 0, 0},
            {7, 85000,  60, 58, 2, 0, 0},
            {2, 20000,  85, 82, 1, 1, 1},
            {4, 50000,  75, 70, 1, 0, 0},
            {6, 72000,  65, 60, 3, 1, 0},
            {8, 100000, 50, 48, 2, 0, 0},
            {10,120000, 40, 38, 3, 0, 0},
            {3, 30000,  88, 85, 1, 1, 1},
            {9, 110000, 45, 42, 2, 1, 0},
            {2, 25000,  92, 90, 1, 0, 1},
        };
        double[] precios = {
            75_000_000, 60_000_000, 50_000_000, 42_000_000,
            70_000_000, 55_000_000, 45_000_000, 35_000_000,
            28_000_000, 65_000_000, 30_000_000, 72_000_000,
        };

        for (int i = 0; i < datos.length; i++) {
            features.add(datos[i]);
            labels.add(precios[i]);
        }

        model.train(features, labels);

        double pred = model.predict(new double[]{3, 30_000, 85, 80, 1, 1, 0});
        assertTrue(pred > 0, "Precio predicho debe ser positivo");
        assertTrue(pred < 200_000_000, "Precio predicho no debe ser absurdamente alto");
    }

    @Test
    void r2CercaDeUnoConDatosLineales() {
        List<double[]> features = new ArrayList<>();
        List<Double>   labels   = new ArrayList<>();

        double[][] datos = {
            {1, 10000,  90, 88, 1, 1, 0},
            {3, 35000,  80, 75, 1, 1, 0},
            {5, 60000,  70, 65, 2, 0, 0},
            {7, 85000,  60, 58, 2, 0, 0},
            {2, 20000,  85, 82, 1, 1, 1},
            {4, 50000,  75, 70, 1, 0, 0},
            {6, 72000,  65, 60, 3, 1, 0},
            {8, 100000, 50, 48, 2, 0, 0},
            {10,120000, 40, 38, 3, 0, 0},
            {3, 30000,  88, 85, 1, 1, 1},
            {9, 110000, 45, 42, 2, 1, 0},
            {2, 25000,  92, 90, 1, 0, 1},
            {6, 65000,  68, 63, 2, 0, 0},
            {4, 45000,  78, 74, 1, 1, 0},
            {8, 95000,  52, 50, 3, 0, 0},
            {1, 12000,  93, 91, 1, 1, 1},
            {5, 55000,  72, 68, 2, 1, 0},
            {7, 80000,  62, 59, 2, 0, 0},
            {3, 28000,  86, 83, 1, 0, 0},
            {9, 105000, 47, 44, 3, 1, 0},
        };
        double[] precios = {
            75_000_000, 60_000_000, 50_000_000, 42_000_000,
            70_000_000, 55_000_000, 45_000_000, 35_000_000,
            28_000_000, 65_000_000, 30_000_000, 72_000_000,
            47_000_000, 57_000_000, 33_000_000, 78_000_000,
            52_000_000, 43_000_000, 63_000_000, 31_000_000,
        };

        for (int i = 0; i < datos.length; i++) {
            features.add(datos[i]);
            labels.add(precios[i]);
        }

        model.train(features, labels);
        double r2 = model.computeR2(features, labels);
        assertTrue(r2 > 0.80, "R² esperado > 0.80, fue: " + r2);
    }

    @Test
    void featuresCantidadIncorrectaLanzaExcepcion() {
        List<double[]> features = List.of(new double[]{1, 2, 3});
        List<Double>   labels   = List.of(1.0, 2.0, 3.0);
        // Solo 3 features en vez de 7 → entrenamiento fallará al construir X
        assertThrows(Exception.class, () -> model.train(features, labels));
    }

    @Test
    void datoInsuficientesLanzaExcepcion() {
        List<double[]> features = new ArrayList<>();
        List<Double>   labels   = new ArrayList<>();

        // Solo 5 muestras — menos que FEATURE_COUNT + 1
        for (int i = 0; i < 5; i++) {
            features.add(new double[]{i, i * 1000.0, 70, 65, 1, 0, 0});
            labels.add(50_000_000.0);
        }

        assertThrows(IllegalArgumentException.class, () -> model.train(features, labels));
    }
}
