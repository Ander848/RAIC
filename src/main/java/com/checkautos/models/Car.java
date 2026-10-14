package com.checkautos.models;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;

@Document(collection = "autos")
public class Car {

    @Id
    private String id;

    private String marca;
    private String modelo;

    @Field("año")
    private String año;

    private String color;

    @Indexed(unique = true)
    private String placa;

    private String kilometraje;
    private String transmision;
    private String combustible;
    private String propietarioId;
    private String propietario;
    private String cedula;
    private int puntajeTecnico;
    private int puntajeMecanico;
    private int precio;
    private String estado;
    private String fechaRegistro;
    private String observacionRevision;
    private String observacionMecanica;
    private String nPropietarios;
    private boolean archivado = false;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;

    public Car() {
        this.puntajeTecnico  = 0;
        this.puntajeMecanico = 0;
        this.precio          = 0;
        this.estado          = "Disponible";
    }



    public String getId()                        { return id; }
    public void   setId(String id)               { this.id = id; }

    public String getMarca()                     { return marca; }
    public void   setMarca(String marca)         { this.marca = marca; }

    public String getModelo()                    { return modelo; }
    public void   setModelo(String modelo)       { this.modelo = modelo; }

    public String getAño()                       { return año; }
    public void   setAño(String año)             { this.año = año; }

    public String getColor()                     { return color; }
    public void   setColor(String color)         { this.color = color; }

    public String getPlaca()                     { return placa; }
    public void   setPlaca(String placa)         { this.placa = placa; }

    public String getKilometraje()               { return kilometraje; }
    public void   setKilometraje(String km)      { this.kilometraje = km; }

    public String getTransmision()               { return transmision; }
    public void   setTransmision(String t)       { this.transmision = t; }

    public String getCombustible()               { return combustible; }
    public void   setCombustible(String c)       { this.combustible = c; }

    public String getPropietario()               { return propietario; }
    public void   setPropietario(String p)       { this.propietario = p; }

    public String getCedula()                    { return cedula; }
    public void   setCedula(String c)            { this.cedula = c; }

    public String getPropietarioId()             { return propietarioId; }
    public void   setPropietarioId(String id)    { this.propietarioId = id; }

    public int  getPuntajeTecnico()              { return puntajeTecnico; }
    public void setPuntajeTecnico(int p)         { this.puntajeTecnico = p; }

    public int  getPuntajeMecanico()             { return puntajeMecanico; }
    public void setPuntajeMecanico(int p)        { this.puntajeMecanico = p; }

    public int  getPrecio()                      { return precio; }
    public void setPrecio(int precio)            { this.precio = precio; }

    public String getEstado()                    { return estado; }
    public void   setEstado(String estado)       { this.estado = estado; }

    public String getFechaRegistro()             { return fechaRegistro; }
    public void   setFechaRegistro(String f)     { this.fechaRegistro = f; }

    public String getObservacionRevision()       { return observacionRevision; }
    public void   setObservacionRevision(String o){ this.observacionRevision = o; }

    public String getObservacionMecanica()       { return observacionMecanica; }
    public void   setObservacionMecanica(String o){ this.observacionMecanica = o; }

    @JsonProperty("nPropietarios")
    public String getNPropietarios()             { return nPropietarios; }
    @JsonProperty("nPropietarios")
    public void   setNPropietarios(String n)     { this.nPropietarios = n; }

    public boolean isArchivado()             { return archivado; }
    public void   setArchivado(boolean a)    { this.archivado = a; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaModificacion() { return fechaModificacion; }
    public void setFechaModificacion(LocalDateTime fechaModificacion) { this.fechaModificacion = fechaModificacion; }

    public int getPuntajePromedio() {
        return (puntajeTecnico + puntajeMecanico) / 2;
    }

    @Override
    public String toString() {
        return "Car{id='" + id + "', marca='" + marca + "', modelo='" + modelo +
               "', placa='" + placa + "', estado='" + estado + "'}";
    }
}