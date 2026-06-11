package com.compilador;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OptEliminacionMuerto {

    public List<String> optimizar(List<String> instrucciones) {
        Pattern asignTemp = Pattern.compile("^(t\\d+)\\s*=");

        Set<String> asignadas = new HashSet<>();
        for (String instr : instrucciones) {
            Matcher m = asignTemp.matcher(instr.trim());
            if (m.find()) asignadas.add(m.group(1));
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
