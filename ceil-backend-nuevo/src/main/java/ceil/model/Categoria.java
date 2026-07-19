package ceil.model;

public class Categoria {
    private int id_categoria;
    private String nombre;
    private String emoji;
    private String color_hex;
    private boolean es_sensible;

    public Categoria() {}

    public Categoria(int idCategoria, String nombre, String emoji, String colorHex, boolean esSensible) {
        this.id_categoria = idCategoria;
        this.nombre = nombre;
        this.emoji = emoji;
        this.color_hex = colorHex;
        this.es_sensible = esSensible;
    }

    public int getId_categoria() { return id_categoria; }
    public String getNombre() { return nombre; }
    public String getEmoji() { return emoji; }
    public String getColor_hex() { return color_hex; }

    /** Dispara la dinámica "¿lo necesitas o lo quieres?" al registrar un gasto. */
    public boolean isEs_sensible() { return es_sensible; }
}
