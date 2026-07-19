package ceil.model;

public class AlumnoRendimiento {
    private String nombre;
    private int racha;
    private double ahorro;
    private String estado;
    private String nivel; // Oro, Plata, Bronce

    public AlumnoRendimiento(String nombre, int racha, double ahorro, String estado, String nivel) {
        this.nombre = nombre;
        this.racha = racha;
        this.ahorro = ahorro;
        this.estado = estado;
        this.nivel = nivel;
    }

    // Getters para que Javalin los convierta a JSON automáticamente
    public String getNombre() { return nombre; }
    public int getRacha() { return racha; }
    public double getAhorro() { return ahorro; }
    public String getEstado() { return estado; }
    public String getNivel() { return nivel; }
}