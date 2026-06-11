package com.compilador;

import java.util.ArrayList;
import java.util.List;

public class OptSimplificacionExpresiones {
    private List<String> foldLog = new ArrayList<>();

    public List<String> getFoldLog() {
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
