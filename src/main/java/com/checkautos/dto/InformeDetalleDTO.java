package com.checkautos.dto;

import java.time.LocalDateTime;

public class InformeDetalleDTO {

    private String placa;
    private String marca;
    private String modelo;
    private String año;
    private String color;
    private String kilometraje;
    private String transmision;
    private String combustible;
    private int precio;
    private String estado;
    private String fechaRegistro;
    private String nPropietarios;

    // Propietario
    private String propietarioNombre;
    private String propietarioCedula;
    private String propietarioTelefono;
    private String propietarioEmail;

    // Revisión
    private int puntajeTecnico;
    private int puntajeMecanico;
    private int puntajePromedio;
    private String observacionRevision;
    private String observacionMecanica;

    // Archivos
    private boolean archivoPdfExiste;
    private boolean archivoTxtExiste;
    private LocalDateTime fechaGeneracion;

    public InformeDetalleDTO() {}

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getAño() { return año; }
    public void setAño(String año) { this.año = año; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getKilometraje() { return kilometraje; }
    public void setKilometraje(String kilometraje) { this.kilometraje = kilometraje; }

    public String getTransmision() { return transmision; }
    public void setTransmision(String transmision) { this.transmision = transmision; }

    public String getCombustible() { return combustible; }
    public void setCombustible(String combustible) { this.combustible = combustible; }

    public int getPrecio() { return precio; }
    public void setPrecio(int precio) { this.precio = precio; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(String fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getNPropietarios() { return nPropietarios; }
    public void setNPropietarios(String nPropietarios) { this.nPropietarios = nPropietarios; }

    public String getPropietarioNombre() { return propietarioNombre; }
    public void setPropietarioNombre(String propietarioNombre) { this.propietarioNombre = propietarioNombre; }

    public String getPropietarioCedula() { return propietarioCedula; }
    public void setPropietarioCedula(String propietarioCedula) { this.propietarioCedula = propietarioCedula; }

    public String getPropietarioTelefono() { return propietarioTelefono; }
    public void setPropietarioTelefono(String propietarioTelefono) { this.propietarioTelefono = propietarioTelefono; }

    public String getPropietarioEmail() { return propietarioEmail; }
    public void setPropietarioEmail(String propietarioEmail) { this.propietarioEmail = propietarioEmail; }

    public int getPuntajeTecnico() { return puntajeTecnico; }
    public void setPuntajeTecnico(int puntajeTecnico) { this.puntajeTecnico = puntajeTecnico; }

    public int getPuntajeMecanico() { return puntajeMecanico; }
    public void setPuntajeMecanico(int puntajeMecanico) { this.puntajeMecanico = puntajeMecanico; }

    public int getPuntajePromedio() { return puntajePromedio; }
    public void setPuntajePromedio(int puntajePromedio) { this.puntajePromedio = puntajePromedio; }

    public String getObservacionRevision() { return observacionRevision; }
    public void setObservacionRevision(String observacionRevision) { this.observacionRevision = observacionRevision; }

    public String getObservacionMecanica() { return observacionMecanica; }
    public void setObservacionMecanica(String observacionMecanica) { this.observacionMecanica = observacionMecanica; }

    public boolean isArchivoPdfExiste() { return archivoPdfExiste; }
    public void setArchivoPdfExiste(boolean archivoPdfExiste) { this.archivoPdfExiste = archivoPdfExiste; }

    public boolean isArchivoTxtExiste() { return archivoTxtExiste; }
    public void setArchivoTxtExiste(boolean archivoTxtExiste) { this.archivoTxtExiste = archivoTxtExiste; }

    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDateTime fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }
}
