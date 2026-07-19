package ceil.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Pregunta {
    private int idPregunta;
    private String pregunta;
    private String opcionA;
    private String opcionB;
    private String opcionC;
    private String opcionCorrecta;
    private int puntos;
    private String retroalimentacion;

    public Pregunta() {}

    public Pregunta(int idPregunta, String pregunta, String opcionA, String opcionB, String opcionC, String opcionCorrecta, int puntos, String retroalimentacion) {
        this.idPregunta = idPregunta;
        this.pregunta = pregunta;
        this.opcionA = opcionA;
        this.opcionB = opcionB;
        this.opcionC = opcionC;
        this.opcionCorrecta = opcionCorrecta;
        this.puntos = puntos;
        this.retroalimentacion = retroalimentacion;
    }

    // Getters y Setters
    public int getIdPregunta() { return idPregunta; }
    public void setIdPregunta(int idPregunta) { this.idPregunta = idPregunta; }
    public String getPregunta() { return pregunta; }
    public void setPregunta(String pregunta) { this.pregunta = pregunta; }
    public String getOpcionA() { return opcionA; }
    public void setOpcionA(String opcionA) { this.opcionA = opcionA; }
    public String getOpcionB() { return opcionB; }
    public void setOpcionB(String opcionB) { this.opcionB = opcionB; }
    public String getOpcionC() { return opcionC; }
    public void setOpcionC(String opcionC) { this.opcionC = opcionC; }
    // Nunca sale en el JSON: si el cliente conoce la respuesta correcta, la trivia
    // se puede hacer trampa. La corrección se valida en el servidor.
    @JsonIgnore
    public String getOpcionCorrecta() { return opcionCorrecta; }
    public void setOpcionCorrecta(String opcionCorrecta) { this.opcionCorrecta = opcionCorrecta; }
    public int getPuntos() { return puntos; }
    public void setPuntos(int puntos) { this.puntos = puntos; }
    public String getRetroalimentacion() { return retroalimentacion; }
    public void setRetroalimentacion(String retroalimentacion) { this.retroalimentacion = retroalimentacion; }
}