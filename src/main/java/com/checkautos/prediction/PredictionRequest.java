package com.checkautos.prediction;

public class PredictionRequest {

    private String año;
    private String kilometraje;
    private int    puntajeTecnico;
    private int    puntajeMecanico;
    private String nPropietarios;
    private String transmision;
    private String combustible;

    public PredictionRequest() {}

    public String getAño()              { return año; }
    public void   setAño(String año)    { this.año = año; }

    public String getKilometraje()            { return kilometraje; }
    public void   setKilometraje(String km)   { this.kilometraje = km; }

    public int  getPuntajeTecnico()           { return puntajeTecnico; }
    public void setPuntajeTecnico(int p)      { this.puntajeTecnico = p; }

    public int  getPuntajeMecanico()          { return puntajeMecanico; }
    public void setPuntajeMecanico(int p)     { this.puntajeMecanico = p; }

    public String getNPropietarios()          { return nPropietarios; }
    public void   setNPropietarios(String n)  { this.nPropietarios = n; }

    public String getTransmision()             { return transmision; }
    public void   setTransmision(String t)     { this.transmision = t; }

    public String getCombustible()             { return combustible; }
    public void   setCombustible(String c)     { this.combustible = c; }
}
