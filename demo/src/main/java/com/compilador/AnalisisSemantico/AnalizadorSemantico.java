package com.compilador.AnalisisSemantico;

import com.compilador.MiLenguajeBaseVisitor;
import com.compilador.MiLenguajeParser;

import java.util.ArrayList;
import java.util.List;

public class AnalizadorSemantico extends MiLenguajeBaseVisitor<Void> {

    private final TablaSimbolos tabla   = new TablaSimbolos();
    private final List<String>  errores = new ArrayList<>();
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
        visitChildren(ctx);
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
            errores.add("[Línea " + linea + ":" + columna + "] variable '" + nombre + "' ya declarada en este ámbito.");
            return null;
        }

        tabla.agregar(new Simbolo(nombre, tipo, "variable", linea, columna, ambitoActual, "[private]"));
        return visitChildren(ctx);
    }

    @Override
    public Void visitDeclArreglo(MiLenguajeParser.DeclArregloContext ctx) {
        String nombre  = ctx.ID().getText();
        String tipo    = ctx.tipo().getText();
        int    linea   = ctx.ID().getSymbol().getLine();
        int    columna = ctx.ID().getSymbol().getCharPositionInLine();
        String tamano  = ctx.expresion().getText();

        if (tabla.existeEnAmbito(nombre, ambitoActual)) {
            errores.add("[Línea " + linea + ":" + columna + "] arreglo '" + nombre + "' ya declarado en este ámbito.");
            return null;
        }

        tabla.agregar(new Simbolo(nombre, tipo, "variable", linea, columna, ambitoActual, "[arr:" + tamano + "] [private]"));
        return null;
    }

    @Override
    public Void visitAsigVariable(MiLenguajeParser.AsigVariableContext ctx) {
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

    public TablaSimbolos getTabla()   { return tabla;              }
    public List<String>  getErrores() { return errores;            }
    public boolean       hayErrores() { return !errores.isEmpty(); }
}
