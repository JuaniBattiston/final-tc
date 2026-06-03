package com.compilador.AnalisisSemantico;

import java.util.ArrayList;
import java.util.List;

public class TablaSimbolos {

    private final List<Simbolo> simbolos = new ArrayList<>();

    public void agregar(Simbolo s) {
        simbolos.add(s);
    }

    public boolean existeEnAmbito(String nombre, String ambito) {
        for (Simbolo s : simbolos) {
            if (s.nombre.equals(nombre) && s.ambito.equals(ambito)) return true;
        }
        return false;
    }

    public Simbolo buscar(String nombre) {
        for (int i = simbolos.size() - 1; i >= 0; i--) {
            if (simbolos.get(i).nombre.equals(nombre)) return simbolos.get(i);
        }
        return null;
    }

    public void marcarUsado(String nombre) {
        for (int i = simbolos.size() - 1; i >= 0; i--) {
            if (simbolos.get(i).nombre.equals(nombre)) {
                simbolos.get(i).usado = true;
                return;
            }
        }
    }

    public List<Simbolo> getSimbolos() {
        return simbolos;
    }

    public void imprimir() {
        System.out.println("\n=== TABLA DE SÍMBOLOS ===");
        System.out.printf("%-20s%-11s%-16s%-11s%-11s%-16s%s%n",
                "NOMBRE", "TIPO", "CATEGORÍA", "LÍNEA", "COLUMNA", "ÁMBITO", "DETALLES");
        System.out.println("-".repeat(96));
        for (Simbolo s : simbolos) {
            System.out.printf("%-20s%-11s%-16s%-11d%-11d%-16s%s%n",
                    s.nombre, s.tipo, s.categoria,
                    s.linea, s.columna, s.ambito, s.detalles);
        }
    }
}
