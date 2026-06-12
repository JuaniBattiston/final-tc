package com.compilador;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OptSimplificacionExpresiones {
    private List<String> foldLog = new ArrayList<>();

    public List<String> getFoldLog() {
        return foldLog;
    }

    public List<String> optimizar(List<String> instrucciones) {
        Pattern p = Pattern.compile("^(t\\d+)\\s*=\\s*(-?\\d+\\.?\\d*)\\s*([+\\-*/%])\\s*(-?\\d+\\.?\\d*)$");
        List<String> resultado = new ArrayList<>(instrucciones);
        for (int i = 0; i < resultado.size(); i++) {
            Matcher m = p.matcher(resultado.get(i).trim());
            if (m.matches()) {
                String folded = tryFold(m.group(2), m.group(4), m.group(3));
                if (folded != null) {
                    resultado.set(i, m.group(1) + " = " + folded);
                }
            }
        }
        instrucciones.clear();
        instrucciones.addAll(resultado);
        return foldLog;
    }

    private boolean esNumerico(String s) {
        try { Double.parseDouble(s); return true; } catch (NumberFormatException e) { return false; }
    }

    public String tryFold(String left, String right, String op) {
        if (!esNumerico(left) || !esNumerico(right)) return null;

        double l = Double.parseDouble(left);
        double r = Double.parseDouble(right);
        double result;
        switch (op) {
            case "+": result = l + r; break;
            case "-": result = l - r; break;
            case "*": result = l * r; break;
            case "/": result = l / r; break;
            case "%": result = l % r; break;
            default: return null;
        }

        String folded;
        if (result == Math.floor(result) && !Double.isInfinite(result))
            folded = String.valueOf((long) result);
        else
            folded = String.valueOf(result);

        foldLog.add(left + " " + op + " " + right + "  →  " + folded);
        return folded;
    }
}
