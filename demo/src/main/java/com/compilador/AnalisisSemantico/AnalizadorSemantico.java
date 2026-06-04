package com.compilador.AnalisisSemantico;

import com.compilador.MiLenguajeBaseVisitor;
import com.compilador.MiLenguajeParser;

import java.util.ArrayList;
import java.util.List;

public class AnalizadorSemantico extends MiLenguajeBaseVisitor<Void> {

    private final TablaSimbolos tabla     = new TablaSimbolos();
    private final List<String>  errores   = new ArrayList<>();
    private       String        ambitoActual = "global";

    @Override
    public Void visitDeclaracion(MiLenguajeParser.DeclaracionContext ctx) {
        String nombre  = ctx.ID().getText();
        String tipo    = ctx.tipo().getText();
        int    linea   = ctx.ID().getSymbol().getLine();
        int    columna = ctx.ID().getSymbol().getCharPositionInLine();

        if (tabla.existeEnAmbito(nombre, ambitoActual)) {
            errores.add("[Línea " + linea + ":" + columna + "] variable '" + nombre + "' ya declarada en este ámbito.");
            return null;
        }

        tabla.agregar(new Simbolo(nombre, tipo, "variable", linea, columna, ambitoActual, "[private]"));
        return visitChildren(ctx);
    }

    @Override
    public Void visitAsignacion(MiLenguajeParser.AsignacionContext ctx) {
        String nombre  = ctx.ID().getText();
        int    linea   = ctx.ID().getSymbol().getLine();
        int    columna = ctx.ID().getSymbol().getCharPositionInLine();

        if (tabla.buscar(nombre) == null) {
            errores.add("[Línea " + linea + ":" + columna + "] variable '" + nombre + "' usada sin declarar.");
        }
        return visitChildren(ctx);
    }

    @Override
    public Void visitExprIdentificador(MiLenguajeParser.ExprIdentificadorContext ctx) {
        String nombre  = ctx.ID().getText();
        int    linea   = ctx.ID().getSymbol().getLine();
        int    columna = ctx.ID().getSymbol().getCharPositionInLine();

        if (tabla.buscar(nombre) == null) {
            errores.add("[Línea " + linea + ":" + columna + "] variable '" + nombre + "' usada sin declarar.");
        }
        return null;
    }

    public TablaSimbolos getTabla()       { return tabla;              }
    public List<String>  getErrores()     { return errores;            }
    public boolean       hayErrores()     { return !errores.isEmpty(); }
}
