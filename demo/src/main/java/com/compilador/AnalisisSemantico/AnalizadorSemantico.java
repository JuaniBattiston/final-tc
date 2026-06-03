package com.compilador.AnalisisSemantico;

import com.compilador.MiLenguajeBaseVisitor;
import com.compilador.MiLenguajeParser;

import java.util.ArrayList;
import java.util.List;

public class AnalizadorSemantico extends MiLenguajeBaseVisitor<Void> {

    private final TablaSimbolos tabla        = new TablaSimbolos();
    private final List<String>  errores      = new ArrayList<>();
    private final List<String>  advertencias = new ArrayList<>();
    private       String        ambitoActual = "global";

    @Override
    public Void visitDeclaracionFuncion(MiLenguajeParser.DeclaracionFuncionContext ctx) {
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
    public Void visitParametro(MiLenguajeParser.ParametroContext ctx) {
        String nombre  = ctx.ID().getText();
        String tipo    = ctx.tipo().getText();
        int    linea   = ctx.ID().getSymbol().getLine();
        int    columna = ctx.ID().getSymbol().getCharPositionInLine();
        tabla.agregar(new Simbolo(nombre, tipo, "parametro", linea, columna, ambitoActual, ""));
        return null;
    }

    @Override
    public Void visitDeclVariable(MiLenguajeParser.DeclVariableContext ctx) {
        String nombre  = ctx.ID().getText();
        String tipo    = ctx.tipo().getText();
        int    linea   = ctx.ID().getSymbol().getLine();
        int    columna = ctx.ID().getSymbol().getCharPositionInLine();

        if (tabla.existeEnAmbito(nombre, ambitoActual)) {
            errores.add("La variable '" + nombre + "' ya está declarada en el ámbito '" + ambitoActual + "' [Línea " + linea + ":" + columna + "]");
            return null;
        }

        tabla.agregar(new Simbolo(nombre, tipo, "variable", linea, columna, ambitoActual, "[private]"));
        if (ctx.expresion() != null) visit(ctx.expresion());
        return null;
    }

    @Override
    public Void visitDeclArreglo(MiLenguajeParser.DeclArregloContext ctx) {
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
    public Void visitAsigVariable(MiLenguajeParser.AsigVariableContext ctx) {
        String nombre  = ctx.ID().getText();
        int    linea   = ctx.ID().getSymbol().getLine();

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

        visit(ctx.expresion());
        return null;
    }

    @Override
    public Void visitAsigArreglo(MiLenguajeParser.AsigArregloContext ctx) {
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

    @Override
    public Void visitExprIdentificador(MiLenguajeParser.ExprIdentificadorContext ctx) {
        String nombre = ctx.ID().getText();
        int    linea  = ctx.ID().getSymbol().getLine();

        Simbolo s = tabla.buscar(nombre);
        if (s == null) {
            errores.add("Variable '" + nombre + "' no declarada en el ámbito '" + ambitoActual + "' [Línea " + linea + "]");
        } else {
            s.usado = true;
        }
        return null;
    }

    @Override
    public Void visitExprAccesoArray(MiLenguajeParser.ExprAccesoArrayContext ctx) {
        String nombre = ctx.ID().getText();
        int    linea  = ctx.ID().getSymbol().getLine();

        Simbolo s = tabla.buscar(nombre);
        if (s == null) {
            errores.add("Variable '" + nombre + "' no declarada en el ámbito '" + ambitoActual + "' [Línea " + linea + "]");
        } else {
            s.usado = true;
        }
        visit(ctx.expresion());
        return null;
    }

    public TablaSimbolos getTabla()        { return tabla;                   }
    public List<String>  getErrores()      { return errores;                 }
    public List<String>  getAdvertencias() { return advertencias;            }
    public boolean       hayErrores()      { return !errores.isEmpty();      }
    public boolean       hayAdvertencias() { return !advertencias.isEmpty(); }
}
