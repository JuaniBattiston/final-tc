package com.compilador;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OptPropagacionConstantes {

    public List<String> optimizar(List<String> instrucciones) {
        Pattern asignacion = Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+)$");
        Map<String, String> constMap = new HashMap<>();
        List<String> log = new ArrayList<>();
        List<String> resultado = new ArrayList<>();

        for (String instr : instrucciones) {
            String trimmed = instr.trim();
            String procesada = instr;

            Matcher mAsign = asignacion.matcher(trimmed);
            if (mAsign.matches()) {
                String var = mAsign.group(1);
                String rhs = mAsign.group(2).trim();
                String rhsProp = aplicarSustitucion(rhs, constMap, log);
                procesada = var + " = " + rhsProp;
                if (esLiteral(rhsProp)) {
                    constMap.put(var, rhsProp);
                } else {
                    constMap.remove(var);
                }
            } else if (!trimmed.isEmpty() && !trimmed.endsWith(":")) {
                procesada = aplicarSustitucionLinea(instr, constMap, log);
            }

            resultado.add(procesada);
        }

        instrucciones.clear();
        instrucciones.addAll(resultado);
        return log;
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

    private boolean esLiteral(String s) {
        if (s == null || s.isEmpty()) return false;
        try { Double.parseDouble(s); return true; } catch (NumberFormatException ignored) {}
        if (s.equals("true") || s.equals("false")) return true;
        if (s.startsWith("'") && s.endsWith("'")) return true;
        if (s.startsWith("\"") && s.endsWith("\"")) return true;
        return false;
    }
}
