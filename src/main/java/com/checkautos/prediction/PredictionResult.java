package com.checkautos.prediction;

public class PredictionResult {

    private final int    precioPredicho;
    private final double precioExacto;
    private final String confianza;
    private final double r2;
    private final int    margenErrorPromedio;
    private final String estadoModelo;

    public PredictionResult(int precioPredicho,
                            double precioExacto,
                            String confianza,
                            double r2,
                            int margenErrorPromedio,
                            String estadoModelo) {
        this.precioPredicho      = precioPredicho;
        this.precioExacto        = precioExacto;
        this.confianza           = confianza;
        this.r2                  = r2;
        this.margenErrorPromedio = margenErrorPromedio;
        this.estadoModelo        = estadoModelo;
    }

    public int    getPrecioPredicho()      { return precioPredicho; }
    public double getPrecioExacto()        { return precioExacto; }
    public String getConfianza()           { return confianza; }
    public double getR2()                  { return r2; }
    public int    getMargenErrorPromedio() { return margenErrorPromedio; }
    public String getEstadoModelo()        { return estadoModelo; }

    public String getPrecioFormateado() {
        return "$" + String.format("%,d", precioPredicho).replace(",", ".");
    }
}
