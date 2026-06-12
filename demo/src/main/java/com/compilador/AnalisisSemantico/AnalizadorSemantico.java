package com.compilador.AnalisisSemantico;

import com.compilador.MiLenguajeBaseVisitor;
import com.compilador.MiLenguajeParser;

import java.util.ArrayList;
import java.util.List;

public class AnalizadorSemantico extends MiLenguajeBaseVisitor<String> {

    private final TablaSimbolos tabla        = new TablaSimbolos();
    private final List<String>  errores      = new ArrayList<>();
    private final List<String>  advertencias = new ArrayList<>();
    private       String        ambitoActual = "global";

    // ── helpers ──────────────────────────────────────────────────────────

    private boolean esNumerico(String t) {
        return t != null && (t.equals("int") || t.equals("float") || t.equals("double"));
    }

    private String tipoAmplio(String t1, String t2) {
        if ("double".equals(t1) || "double".equals(t2)) return "double";
        if ("float".equals(t1)  || "float".equals(t2))  return "float";
        return "int";
    }

    private boolean compatible(String declarado, String expresion) {
        return declarado.equals(expresion) || (esNumerico(declarado) && esNumerico(expresion));
    }

    // ── sentencias / declaraciones ────────────────────────────────────────

    @Override
    public String visitDeclaracionFuncion(MiLenguajeParser.DeclaracionFuncionContext ctx) {
        String nombre  = ctx.ID().getText();
        String tipo    = ctx.tipo().getText();
        int    linea   = ctx.ID().getSymbol().getLine();
        int    columna = ctx.ID().getSymbol().getCharPositionInLine();

        List<String> tiposParam = new ArrayList<>();
        if (ctx.listaParametros() != null) {
            for (MiLenguajeParser.ParametroContext p : ctx.listaParametros().parametro()) {
                tiposParam.add(p.tipo().getText());
            }
        }

        String detalles = tiposParam.isEmpty()
                ? "[private]"
                : "[private] [" + String.join(", ", tiposParam) + "]";

        tabla.agregar(new Simbolo(nombre, tipo, "funcion", linea, columna, "global", detalles));

        String ambitoAnterior = ambitoActual;
        ambitoActual = nombre;

        if (ctx.listaParametros() != null) visit(ctx.listaParametros());
        visit(ctx.bloque());

        for (Simbolo s : tabla.getSimbolos()) {
            if (s.ambito.equals(nombre) && s.categoria.equals("variable") && !s.usado) {
                advertencias.add("Warning: Variable '" + s.nombre + "' declarada pero nunca utilizada en el ámbito '" + s.ambito + "' [Línea " + s.linea + "]");
            }
        }

        ambitoActual = ambitoAnterior;
        return null;
    }

    @Override
    public String visitParametro(MiLenguajeParser.ParametroContext ctx) {
        String nombre  = ctx.ID().getText();
        String tipo    = ctx.tipo().getText();
        int    linea   = ctx.ID().getSymbol().getLine();
        int    columna = ctx.ID().getSymbol().getCharPositionInLine();
        tabla.agregar(new Simbolo(nombre, tipo, "parametro", linea, columna, ambitoActual, ""));
        return null;
    }

    @Override
    public String visitDeclVariable(MiLenguajeParser.DeclVariableContext ctx) {
        String nombre  = ctx.ID().getText();
        String tipo    = ctx.tipo().getText();
        int    linea   = ctx.ID().getSymbol().getLine();
        int    columna = ctx.ID().getSymbol().getCharPositionInLine();

        if (tabla.existeEnAmbito(nombre, ambitoActual)) {
            errores.add("La variable '" + nombre + "' ya está declarada en el ámbito '" + ambitoActual + "' [Línea " + linea + ":" + columna + "]");
            return null;
        }

        tabla.agregar(new Simbolo(nombre, tipo, "variable", linea, columna, ambitoActual, "[private]"));

        if (ctx.expresion() != null) {
            String tipoExpr = visit(ctx.expresion());
            if (tipoExpr != null && !compatible(tipo, tipoExpr)) {
                errores.add("No se puede asignar '" + tipoExpr + "' a variable de tipo '" + tipo + "' [Línea " + linea + "]");
            }
        }
        return null;
    }

    @Override
    public String visitDeclArreglo(MiLenguajeParser.DeclArregloContext ctx) {
        String nombre  = ctx.ID().getText();
        String tipo    = ctx.tipo().getText();
        int    linea   = ctx.ID().getSymbol().getLine();
        int    columna = ctx.ID().getSymbol().getCharPositionInLine();
        String tamano  = ctx.expresion().getText();

        if (tabla.existeEnAmbito(nombre, ambitoActual)) {
            errores.add("La variable '" + nombre + "' ya está declarada en el ámbito '" + ambitoActual + "' [Línea " + linea + ":" + columna + "]");
            return null;
        }

        tabla.agregar(new Simbolo(nombre, tipo, "variable", linea, columna, ambitoActual, "[arr:" + tamano + "] [private]"));
        return null;
    }

    @Override
    public String visitAsigVariable(MiLenguajeParser.AsigVariableContext ctx) {
        String nombre = ctx.ID().getText();
        int    linea  = ctx.ID().getSymbol().getLine();

        Simbolo s = tabla.buscar(nombre);
        if (s != null && s.categoria.equals("funcion")) {
            errores.add("No se puede asignar valor a '" + nombre + "' porque no es una variable [Línea " + linea + "]");
            return null;
        }

        if (s == null) {
            errores.add("Variable '" + nombre + "' no declarada en el ámbito '" + ambitoActual + "' [Línea " + linea + "]");
        } else {
            s.usado = true;
        }

        String tipoExpr = visit(ctx.expresion());
        if (s != null && tipoExpr != null && !compatible(s.tipo, tipoExpr)) {
            errores.add("No se puede asignar '" + tipoExpr + "' a variable de tipo '" + s.tipo + "' [Línea " + linea + "]");
        }
        return null;
    }

    @Override
    public String visitAsigArreglo(MiLenguajeParser.AsigArregloContext ctx) {
        String nombre = ctx.ID().getText();
        int    linea  = ctx.ID().getSymbol().getLine();

        Simbolo s = tabla.buscar(nombre);
        if (s == null) {
            errores.add("Variable '" + nombre + "' no declarada en el ámbito '" + ambitoActual + "' [Línea " + linea + "]");
        } else {
            s.usado = true;
        }

        visitChildren(ctx);
        return null;
    }

    // ── expresiones: literales ────────────────────────────────────────────

    @Override public String visitExprEntero    (MiLenguajeParser.ExprEnteroContext     ctx) { return "int";    }
    @Override public String visitExprDecimal   (MiLenguajeParser.ExprDecimalContext    ctx) { return "float";  }
    @Override public String visitExprVerdadero (MiLenguajeParser.ExprVerdaderoContext  ctx) { return "bool";   }
    @Override public String visitExprFalso     (MiLenguajeParser.ExprFalsoContext      ctx) { return "bool";   }
    @Override public String visitExprCadena    (MiLenguajeParser.ExprCadenaContext     ctx) { return "string"; }
    @Override public String visitExprCaracter  (MiLenguajeParser.ExprCaracterContext   ctx) { return "char";   }

    @Override
    public String visitExprAgrupada(MiLenguajeParser.ExprAgrupadaContext ctx) {
        return visit(ctx.expresion());
    }

    // ── expresiones: identificadores ─────────────────────────────────────

    @Override
    public String visitExprIdentificador(MiLenguajeParser.ExprIdentificadorContext ctx) {
        String nombre = ctx.ID().getText();
        int    linea  = ctx.ID().getSymbol().getLine();

        Simbolo s = tabla.buscar(nombre);
        if (s == null) {
            errores.add("Variable '" + nombre + "' no declarada en el ámbito '" + ambitoActual + "' [Línea " + linea + "]");
            return null;
        }
        s.usado = true;
        return s.tipo;
    }

    @Override
    public String visitExprAccesoArray(MiLenguajeParser.ExprAccesoArrayContext ctx) {
        String nombre = ctx.ID().getText();
        int    linea  = ctx.ID().getSymbol().getLine();

        Simbolo s = tabla.buscar(nombre);
        if (s == null) {
            errores.add("Variable '" + nombre + "' no declarada en el ámbito '" + ambitoActual + "' [Línea " + linea + "]");
            return null;
        }
        s.usado = true;
        visit(ctx.expresion());
        return s.tipo;
    }

    @Override
    public String visitExprLlamada(MiLenguajeParser.ExprLlamadaContext ctx) {
        for (MiLenguajeParser.ExpresionContext arg : ctx.expresion()) visit(arg);
        Simbolo s = tabla.buscar(ctx.ID().getText());
        return s != null ? s.tipo : null;
    }

    // ── expresiones: operaciones aritméticas ─────────────────────────────

    @Override
    public String visitExprAditiva(MiLenguajeParser.ExprAditivaContext ctx) {
        String t1 = visit(ctx.expresion(0));
        String t2 = visit(ctx.expresion(1));
        if (!esNumerico(t1) || !esNumerico(t2)) {
            errores.add("Operación aritmética no permitida entre '" + t1 + "' y '" + t2 + "' [Línea " + ctx.getStart().getLine() + "]");
            return null;
        }
        return tipoAmplio(t1, t2);
    }

    @Override
    public String visitExprMultiplicativa(MiLenguajeParser.ExprMultiplicativaContext ctx) {
        String t1 = visit(ctx.expresion(0));
        String t2 = visit(ctx.expresion(1));
        if (!esNumerico(t1) || !esNumerico(t2)) {
            errores.add("Operación aritmética no permitida entre '" + t1 + "' y '" + t2 + "' [Línea " + ctx.getStart().getLine() + "]");
            return null;
        }
        return tipoAmplio(t1, t2);
    }

    // ── expresiones: comparaciones ────────────────────────────────────────

    @Override
    public String visitExprRelacional(MiLenguajeParser.ExprRelacionalContext ctx) {
        String t1 = visit(ctx.expresion(0));
        String t2 = visit(ctx.expresion(1));
        if ((!esNumerico(t1) && !"char".equals(t1)) || (!esNumerico(t2) && !"char".equals(t2))) {
            errores.add("Operación relacional no permitida entre '" + t1 + "' y '" + t2 + "' [Línea " + ctx.getStart().getLine() + "]");
        }
        return "bool";
    }

    @Override
    public String visitExprIgualdad(MiLenguajeParser.ExprIgualdadContext ctx) {
        String t1 = visit(ctx.expresion(0));
        String t2 = visit(ctx.expresion(1));
        if (t1 != null && t2 != null && !t1.equals(t2) && !(esNumerico(t1) && esNumerico(t2))) {
            errores.add("Comparación no permitida entre '" + t1 + "' y '" + t2 + "' [Línea " + ctx.getStart().getLine() + "]");
        }
        return "bool";
    }

    // ── expresiones: lógicas ──────────────────────────────────────────────

    @Override
    public String visitExprOr(MiLenguajeParser.ExprOrContext ctx) {
        String t1 = visit(ctx.expresion(0));
        String t2 = visit(ctx.expresion(1));
        if (!"bool".equals(t1) || !"bool".equals(t2)) {
            errores.add("Operador '||' requiere operandos bool, se encontró '" + t1 + "' y '" + t2 + "' [Línea " + ctx.getStart().getLine() + "]");
        }
        return "bool";
    }

    @Override
    public String visitExprAnd(MiLenguajeParser.ExprAndContext ctx) {
        String t1 = visit(ctx.expresion(0));
        String t2 = visit(ctx.expresion(1));
        if (!"bool".equals(t1) || !"bool".equals(t2)) {
            errores.add("Operador '&&' requiere operandos bool, se encontró '" + t1 + "' y '" + t2 + "' [Línea " + ctx.getStart().getLine() + "]");
        }
        return "bool";
    }

    @Override
    public String visitExprNot(MiLenguajeParser.ExprNotContext ctx) {
        String t = visit(ctx.expresion());
        if (!"bool".equals(t)) {
            errores.add("Operador '!' requiere operando bool, se encontró '" + t + "' [Línea " + ctx.getStart().getLine() + "]");
        }
        return "bool";
    }

    @Override
    public String visitExprNegativo(MiLenguajeParser.ExprNegativoContext ctx) {
        String t = visit(ctx.expresion());
        if (!esNumerico(t)) {
            errores.add("Operador unario '-' no permitido sobre tipo '" + t + "' [Línea " + ctx.getStart().getLine() + "]");
        }
        return t;
    }

    // ── getters ───────────────────────────────────────────────────────────

    public TablaSimbolos getTabla()        { return tabla;                   }
    public List<String>  getErrores()      { return errores;                 }
    public List<String>  getAdvertencias() { return advertencias;            }
    public boolean       hayErrores()      { return !errores.isEmpty();      }
    public boolean       hayAdvertencias() { return !advertencias.isEmpty(); }
}
