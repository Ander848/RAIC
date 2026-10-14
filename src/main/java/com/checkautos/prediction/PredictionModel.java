package com.checkautos.prediction;

import java.util.List;

/**
 * Regresión lineal múltiple usando mínimos cuadrados ordinarios (OLS).
 * Sin dependencias externas — pura aritmética matricial en Java.
 *
 * Features de entrada (en este orden):
 *   0: edad del auto         (años desde fabricación)
 *   1: kilometraje           (en km, numérico)
 *   2: puntaje técnico       (0-100)
 *   3: puntaje mecánico      (0-100)
 *   4: número de propietarios
 *   5: transmisión automática (1 = automática, 0 = manual)
 *   6: combustible eléctrico o híbrido (1 = sí, 0 = no)
 */
public class PredictionModel {

    public static final int FEATURE_COUNT = 7;

    // Regularización Ridge: evita matriz singular cuando features están correlacionadas.
    // Valor pequeño relativo a las magnitudes de precio (millones de COP).
    private static final double LAMBDA = 1e6;

    private double[] weights;   // w[0..6]
    private double   bias;      // término independiente
    private boolean  trained = false;

    // -----------------------------------------------------------------------
    // Entrenamiento: OLS normal equation  β = (XᵀX)⁻¹ Xᵀy
    // -----------------------------------------------------------------------

    public void train(List<double[]> featureMatrix, List<Double> labels) {
        int n = featureMatrix.size();
        if (n < FEATURE_COUNT + 1) {
            throw new IllegalArgumentException(
                "Se necesitan al menos " + (FEATURE_COUNT + 1) + " muestras para entrenar.");
        }

        int p = FEATURE_COUNT + 1; // +1 para el bias (columna de unos)

        double[][] X = buildDesignMatrix(featureMatrix, n, p);
        double[]   y = labels.stream().mapToDouble(Double::doubleValue).toArray();

        double[][] Xt  = transpose(X, n, p);
        double[][] XtX = multiply(Xt, X, p, n, p);
        double[]   Xty = multiplyMatVec(Xt, y, p, n);

        // Ridge: sumar λ en la diagonal (excepto bias en [0][0])
        for (int i = 1; i < p; i++) XtX[i][i] += LAMBDA;

        double[][] XtXinv = invertSymmetric(XtX, p);
        double[]   beta   = multiplyMatVec(XtXinv, Xty, p, p);

        bias    = beta[0];
        weights = new double[FEATURE_COUNT];
        System.arraycopy(beta, 1, weights, 0, FEATURE_COUNT);
        trained = true;
    }

    // -----------------------------------------------------------------------
    // Predicción
    // -----------------------------------------------------------------------

    public double predict(double[] features) {
        if (!trained) throw new IllegalStateException("El modelo no ha sido entrenado.");
        if (features.length != FEATURE_COUNT)
            throw new IllegalArgumentException("Se esperan " + FEATURE_COUNT + " features.");

        double pred = bias;
        for (int i = 0; i < FEATURE_COUNT; i++) {
            pred += weights[i] * features[i];
        }
        return Math.max(0, pred);
    }

    public boolean isTrained() { return trained; }

    public double[] getWeights() { return weights; }
    public double   getBias()    { return bias; }

    // -----------------------------------------------------------------------
    // Métricas
    // -----------------------------------------------------------------------

    public double computeR2(List<double[]> featureMatrix, List<Double> labels) {
        int n = labels.size();
        double mean = labels.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        double ssTot = 0, ssRes = 0;
        for (int i = 0; i < n; i++) {
            double actual = labels.get(i);
            double pred   = predict(featureMatrix.get(i));
            ssTot += (actual - mean) * (actual - mean);
            ssRes += (actual - pred)  * (actual - pred);
        }
        return ssTot == 0 ? 1.0 : 1.0 - (ssRes / ssTot);
    }

    public double computeMAE(List<double[]> featureMatrix, List<Double> labels) {
        int n = labels.size();
        double sum = 0;
        for (int i = 0; i < n; i++) {
            sum += Math.abs(labels.get(i) - predict(featureMatrix.get(i)));
        }
        return sum / n;
    }

    // -----------------------------------------------------------------------
    // Álgebra matricial
    // -----------------------------------------------------------------------

    private double[][] buildDesignMatrix(List<double[]> fm, int n, int p) {
        double[][] X = new double[n][p];
        for (int i = 0; i < n; i++) {
            X[i][0] = 1.0; // bias
            System.arraycopy(fm.get(i), 0, X[i], 1, FEATURE_COUNT);
        }
        return X;
    }

    private double[][] transpose(double[][] A, int rows, int cols) {
        double[][] T = new double[cols][rows];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                T[j][i] = A[i][j];
        return T;
    }

    private double[][] multiply(double[][] A, double[][] B, int ra, int ca, int cb) {
        double[][] C = new double[ra][cb];
        for (int i = 0; i < ra; i++)
            for (int k = 0; k < ca; k++)
                for (int j = 0; j < cb; j++)
                    C[i][j] += A[i][k] * B[k][j];
        return C;
    }

    private double[] multiplyMatVec(double[][] A, double[] v, int rows, int cols) {
        double[] result = new double[rows];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                result[i] += A[i][j] * v[j];
        return result;
    }

    /** Inversión con eliminación gaussiana con pivoteo parcial. */
    private double[][] invertSymmetric(double[][] A, int n) {
        double[][] aug = new double[n][2 * n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, aug[i], 0, n);
            aug[i][n + i] = 1.0;
        }

        // Eliminación hacia adelante
        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int row = col + 1; row < n; row++)
                if (Math.abs(aug[row][col]) > Math.abs(aug[pivot][col]))
                    pivot = row;

            double[] tmp = aug[col]; aug[col] = aug[pivot]; aug[pivot] = tmp;

            double div = aug[col][col];
            if (Math.abs(div) < 1e-12)
                throw new ArithmeticException("Matriz singular — no invertible. Necesitas más datos o features distintas.");
            for (int j = 0; j < 2 * n; j++) aug[col][j] /= div;

            for (int row = 0; row < n; row++) {
                if (row == col) continue;
                double factor = aug[row][col];
                for (int j = 0; j < 2 * n; j++)
                    aug[row][j] -= factor * aug[col][j];
            }
        }

        double[][] inv = new double[n][n];
        for (int i = 0; i < n; i++)
            System.arraycopy(aug[i], n, inv[i], 0, n);
        return inv;
    }
}
