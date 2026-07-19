package ceil.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Movimiento {
    private int id_movimiento;
    private int id_usuario;
    private BigDecimal monto;
    private String tipo_movimiento;   // 'GASTO' | 'INGRESO'
    private String concepto;
    private String categoria_nombre;
    private Integer id_categoria;
    private String prioridad;
    private String descripcion;
    private Timestamp fecha;

    public Movimiento() {}

    public Movimiento(int id, int idUsuario, BigDecimal monto, String tipo, String concepto,
                      String categoriaNombre, Integer idCategoria, String prioridad,
                      String descripcion, Timestamp fecha) {
        this.id_movimiento = id;
        this.id_usuario = idUsuario;
        this.monto = monto;
        this.tipo_movimiento = tipo;
        this.concepto = concepto;
        this.categoria_nombre = categoriaNombre;
        this.id_categoria = idCategoria;
        this.prioridad = prioridad;
        this.descripcion = descripcion;
        this.fecha = fecha;
    }

    // Getters y Setters necesarios para que Jackson genere el JSON
    public int getId_movimiento() { return id_movimiento; }
    public int getId_usuario() { return id_usuario; }
    public BigDecimal getMonto() { return monto; }
    public String getTipo_movimiento() { return tipo_movimiento; }
    public String getConcepto() { return concepto; }
    public String getCategoria_nombre() { return categoria_nombre; }
    public Integer getId_categoria() { return id_categoria; }
    public String getPrioridad() { return prioridad; }
    public String getDescripcion() { return descripcion; }
    public Timestamp getFecha() { return fecha; }
}
