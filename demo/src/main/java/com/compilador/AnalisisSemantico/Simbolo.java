package com.compilador.AnalisisSemantico;

public class Simbolo {

    public String  nombre;
    public String  tipo;
    public String  categoria;
    public int     linea;
    public int     columna;
    public String  ambito;
    public String  detalles;
    public boolean usado = false;

    public Simbolo(String nombre, String tipo, String categoria,
                   int linea, int columna, String ambito, String detalles) {
        this.nombre    = nombre;
        this.tipo      = tipo;
        this.categoria = categoria;
        this.linea     = linea;
        this.columna   = columna;
        this.ambito    = ambito;
        this.detalles  = detalles;
    }
}
