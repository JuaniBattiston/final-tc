package com.compilador;

import java.util.ArrayList;
import java.util.List;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

public class GeneradorCodigo {
    private List<String> instrucciones;
    private int countTemp = 1;
    private int countLabel = 1;

    public GeneradorCodigo() {
        this.instrucciones = new ArrayList<>();
    }

    public String nuevaTemp() {
        return "t" + (countTemp++);
    }

    public String nuevaEtiqueta(String prefijo) {
        return prefijo + "_" + (countLabel++);
    }

    public void emitir(String instruccion) {
        instrucciones.add(instruccion);
    }

    public void addEtiqueta(String etiqueta) {
        instrucciones.add(etiqueta + ":");
    }

    public int cantidadInstrucciones() {
        return instrucciones.size();
    }

    public List<String> getInstrucciones() {
        return instrucciones;
    }

    public void imprimir() {
        for (int i = 0; i < instrucciones.size(); i++) {
            System.out.printf("%3d: %s%n", i, instrucciones.get(i));
        }
    }

    public void guardar(String filename) {
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            for (int i = 0; i < instrucciones.size(); i++) {
                out.printf("%3d: %s%n", i, instrucciones.get(i));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
