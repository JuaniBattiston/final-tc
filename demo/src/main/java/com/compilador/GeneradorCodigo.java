package com.compilador;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    public List<String> optimizarPropagacionConstantes() {
        Pattern asignacion = Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+)$");
        Map<String, String> constMap = new HashMap<>();
        List<String> propagaciones = new ArrayList<>();
        List<String> resultado = new ArrayList<>();

        for (String instr : instrucciones) {
            String trimmed = instr.trim();
            String procesada = instr;

            Matcher mAsign = asignacion.matcher(trimmed);
            if (mAsign.matches()) {
                String var = mAsign.group(1);
                String rhs = mAsign.group(2).trim();
                String rhsProp = aplicarSustitucion(rhs, constMap, propagaciones);
                procesada = var + " = " + rhsProp;
                if (esLiteralTexto(rhsProp)) {
                    constMap.put(var, rhsProp);
                } else {
                    constMap.remove(var);
                }
            } else if (!trimmed.isEmpty() && !trimmed.endsWith(":")) {
                procesada = aplicarSustitucionLinea(instr, constMap, propagaciones);
            }

            resultado.add(procesada);
        }

        instrucciones = resultado;
        return propagaciones;
    }

    private String aplicarSustitucion(String rhs, Map<String, String> constMap, List<String> log) {
        String resultado = rhs;
        for (Map.Entry<String, String> entry : constMap.entrySet()) {
            String var = entry.getKey();
            String val = entry.getValue();
            String reemplazado = resultado.replaceAll("\\b" + Pattern.quote(var) + "\\b", val);
            if (!reemplazado.equals(resultado)) {
                log.add(var + "  →  " + val);
                resultado = reemplazado;
            }
        }
        return resultado;
    }

    private String aplicarSustitucionLinea(String instr, Map<String, String> constMap, List<String> log) {
        String resultado = instr;
        for (Map.Entry<String, String> entry : constMap.entrySet()) {
            String var = entry.getKey();
            String val = entry.getValue();
            String reemplazado = resultado.replaceAll("\\b" + Pattern.quote(var) + "\\b", val);
            if (!reemplazado.equals(resultado)) {
                log.add(var + "  →  " + val);
                resultado = reemplazado;
            }
        }
        return resultado;
    }

    private boolean esLiteralTexto(String s) {
        if (s == null || s.isEmpty()) return false;
        try { Double.parseDouble(s); return true; } catch (NumberFormatException ignored) {}
        if (s.equals("true") || s.equals("false")) return true;
        if (s.startsWith("'") && s.endsWith("'")) return true;
        if (s.startsWith("\"") && s.endsWith("\"")) return true;
        return false;
    }

    public List<String> optimizarEliminacionMuerto() {
        Pattern asignTemp = Pattern.compile("^(t\\d+)\\s*=");

        Set<String> asignadas = new HashSet<>();
        for (String instr : instrucciones) {
            Matcher m = asignTemp.matcher(instr.trim());
            if (m.find()) {
                asignadas.add(m.group(1));
            }
        }

        Set<String> usadas = new HashSet<>();
        for (String temp : asignadas) {
            for (String instr : instrucciones) {
                String trimmed = instr.trim();
                if (trimmed.matches("^" + Pattern.quote(temp) + "\\s*=.*")) continue;
                if (trimmed.contains(temp)) {
                    usadas.add(temp);
                    break;
                }
            }
        }

        Set<String> muertas = new HashSet<>(asignadas);
        muertas.removeAll(usadas);

        List<String> eliminadas = new ArrayList<>();
        Iterator<String> it = instrucciones.iterator();
        while (it.hasNext()) {
            String instr = it.next();
            Matcher m = asignTemp.matcher(instr.trim());
            if (m.find() && muertas.contains(m.group(1))) {
                eliminadas.add(instr);
                it.remove();
            }
        }

        return eliminadas;
    }
}
