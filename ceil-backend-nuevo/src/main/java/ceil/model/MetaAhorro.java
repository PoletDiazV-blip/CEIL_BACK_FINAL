package ceil.model;

import java.sql.Date;

public class MetaAhorro {
    private int idMeta;
    private int idUsuario;
    private String nombreMeta;
    private double montoObjetivo;
    private double montoActual;
    private Date fechaLimite;

    public MetaAhorro() {}

    public MetaAhorro(int idMeta, int idUsuario, String nombreMeta, double montoObjetivo, double montoActual, Date fechaLimite) {
        this.idMeta = idMeta;
        this.idUsuario = idUsuario;
        this.nombreMeta = nombreMeta;
        this.montoObjetivo = montoObjetivo;
        this.montoActual = montoActual;
        this.fechaLimite = fechaLimite;
    }

    public int getIdMeta() { return idMeta; }
    public void setIdMeta(int idMeta) { this.idMeta = idMeta; }
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public String getNombreMeta() { return nombreMeta; }
    public void setNombreMeta(String nombreMeta) { this.nombreMeta = nombreMeta; }
    public double getMontoObjetivo() { return montoObjetivo; }
    public void setMontoObjetivo(double montoObjetivo) { this.montoObjetivo = montoObjetivo; }
    public double getMontoActual() { return montoActual; }
    public void setMontoActual(double montoActual) { this.montoActual = montoActual; }
    public Date getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(Date fechaLimite) { this.fechaLimite = fechaLimite; }
}