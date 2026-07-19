package ceil.model;

import java.util.Map;

public class DashboardAdmin {
    // Tarjetas principales
    private int usuariosRegistrados;
    private int usuariosActivos;
    private int metasCompletadas;
    private double ahorroAcumulado;
    private int quizzesRespondidos;
    private int rachaPromedioDias;

    // Porcentajes de impacto en usuarios
    private int porcAhorran;
    private int porcRegistranGastos;
    private int porcCompletaronMetas;
    private int porcMejoraronHabitos;

    public DashboardAdmin() {}

    // Getters y Setters para que Javalin los transforme en JSON automáticamente
    public int getUsuariosRegistrados() { return usuariosRegistrados; }
    public void setUsuariosRegistrados(int uR) { this.usuariosRegistrados = uR; }
    public int getUsuariosActivos() { return usuariosActivos; }
    public void setUsuariosActivos(int uA) { this.usuariosActivos = uA; }
    public int getMetasCompletadas() { return metasCompletadas; }
    public void setMetasCompletadas(int mC) { this.metasCompletadas = mC; }
    public double getAhorroAcumulado() { return ahorroAcumulado; }
    public void setAhorroAcumulado(double aA) { this.ahorroAcumulado = aA; }
    public int getQuizzesRespondidos() { return quizzesRespondidos; }
    public void setQuizzesRespondidos(int qR) { this.quizzesRespondidos = qR; }
    public int getRachaPromedioDias() { return rachaPromedioDias; }
    public void setRachaPromedioDias(int rP) { this.rachaPromedioDias = rP; }
    public int getPorcAhorran() { return porcAhorran; }
    public void setPorcAhorran(int pA) { this.porcAhorran = pA; }
    public int getPorcRegistranGastos() { return porcRegistranGastos; }
    public void setPorcRegistranGastos(int pRG) { this.porcRegistranGastos = pRG; }
    public int getPorcCompletaronMetas() { return porcCompletaronMetas; }
    public void setPorcCompletaronMetas(int pCM) { this.porcCompletaronMetas = pCM; }
    public int getPorcMejoraronHabitos() { return porcMejoraronHabitos; }
    public void setPorcMejoraronHabitos(int pMH) { this.porcMejoraronHabitos = pMH; }
}