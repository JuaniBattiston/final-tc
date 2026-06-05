package com.compilador;

import java.util.ArrayList;
import java.util.List;

public class CodigoVisitor extends MiLenguajeBaseVisitor<String> {
    private GeneradorCodigo gen;
    private boolean enGlobal = true;
    private List<String> foldLog = new ArrayList<>();

    public CodigoVisitor(GeneradorCodigo gen) {
        this.gen = gen;
    }

    public List<String> getFoldLog() {
        return foldLog;
    }

    @Override
    public String visitPrograma(MiLenguajeParser.ProgramaContext ctx) {
        gen.emitir("// Código de tres direcciones generado");
        gen.addEtiqueta("PROGRAMA_INICIO");
        
        boolean hasGlobals = false;
        for (MiLenguajeParser.SentenciaContext s : ctx.sentencia()) {
            if (s.declaracion() != null && enGlobal) {
                if (!hasGlobals) {
                    gen.emitir("// Declaración de variables globales");
                    hasGlobals = true;
                }
            } else if (s.declaracionFuncion() != null) {
                enGlobal = false;
            }
        }
        
        for (MiLenguajeParser.SentenciaContext s : ctx.sentencia()) {
            visit(s);
        }
        
        gen.addEtiqueta("PROGRAMA_FIN");
        return null;
    }

    @Override
    public String visitListaParametros(MiLenguajeParser.ListaParametrosContext ctx) {
        for (MiLenguajeParser.ParametroContext p : ctx.parametro()) {
            visit(p);
        }
        return null;
    }

    @Override
    public String visitDeclVariable(MiLenguajeParser.DeclVariableContext ctx) {
        String tipo = ctx.tipo().getText();
        String id = ctx.ID().getText();
        gen.emitir("DECLARE " + id + " " + tipo);
        
        if (ctx.expresion() != null) {
            String val = visit(ctx.expresion());
            gen.emitir(id + " = " + val);
        }
        return null;
    }

    @Override
    public String visitDeclArreglo(MiLenguajeParser.DeclArregloContext ctx) {
        String tipo = ctx.tipo().getText();
        String id = ctx.ID().getText();
        String size = visit(ctx.expresion());
        gen.emitir("DECLARE " + id + "[" + size + "] " + tipo);
        return null;
    }

    @Override
    public String visitDeclaracionFuncion(MiLenguajeParser.DeclaracionFuncionContext ctx) {
        String id = ctx.ID().getText();
        gen.addEtiqueta("func_" + id);
        
        if (ctx.listaParametros() != null) {
            visit(ctx.listaParametros());
        }
        
        if (ctx.bloque() != null) {
            visit(ctx.bloque());
        }
        return null;
    }

    @Override
    public String visitParametro(MiLenguajeParser.ParametroContext ctx) {
        gen.emitir("PARAM " + ctx.ID().getText() + " " + ctx.tipo().getText());
        return null;
    }

    @Override
    public String visitSentenciaReturn(MiLenguajeParser.SentenciaReturnContext ctx) {
        if (ctx.expresion() != null) {
            String temp = visit(ctx.expresion());
            gen.emitir("return " + temp);
        } else {
            gen.emitir("return");
        }
        return null;
    }

    @Override
    public String visitAsigVariable(MiLenguajeParser.AsigVariableContext ctx) {
        String id = ctx.ID().getText();
        String val = visit(ctx.expresion());
        gen.emitir(id + " = " + val);
        return null;
    }

    @Override
    public String visitAsigArreglo(MiLenguajeParser.AsigArregloContext ctx) {
        String id = ctx.ID().getText();
        String idx = visit(ctx.expresion(0));
        String val = visit(ctx.expresion(1));
        gen.emitir(id + "[" + idx + "] = " + val);
        return null;
    }

    @Override
    public String visitSentenciaCout(MiLenguajeParser.SentenciaCoutContext ctx) {
        String val = visit(ctx.expresion());
        gen.emitir("cout << " + val);
        return null;
    }

    @Override
    public String visitSentenciaIf(MiLenguajeParser.SentenciaIfContext ctx) {
        String cond = visit(ctx.expresion());
        String thenLabel = gen.nuevaEtiqueta("THEN");
        String endLabel = gen.nuevaEtiqueta("END_IF");
        String elseLabel = (ctx.ELSE() != null) ? gen.nuevaEtiqueta("ELSE") : null;

        gen.emitir("if " + cond + " goto " + thenLabel);
        if (elseLabel != null) {
            gen.emitir("goto " + elseLabel);
        } else {
            gen.emitir("goto " + endLabel);
        }

        gen.addEtiqueta(thenLabel);
        visit(ctx.bloque(0));
        
        if (elseLabel != null) {
            gen.emitir("goto " + endLabel);
            gen.addEtiqueta(elseLabel);
            visit(ctx.bloque(1));
        }

        gen.addEtiqueta(endLabel);
        return null;
    }

    @Override
    public String visitSentenciaWhile(MiLenguajeParser.SentenciaWhileContext ctx) {
        String startLabel = gen.nuevaEtiqueta("WHILE_START");
        String bodyLabel = gen.nuevaEtiqueta("WHILE_BODY");
        String endLabel = gen.nuevaEtiqueta("WHILE_END");

        gen.addEtiqueta(startLabel);
        String cond = visit(ctx.expresion());
        gen.emitir("if " + cond + " goto " + bodyLabel);
        gen.emitir("goto " + endLabel);

        gen.addEtiqueta(bodyLabel);
        visit(ctx.bloque());
        gen.emitir("goto " + startLabel);

        gen.addEtiqueta(endLabel);
        return null;
    }

    @Override
    public String visitBloque(MiLenguajeParser.BloqueContext ctx) {
        if (ctx.sentencia() != null) {
            for (MiLenguajeParser.SentenciaContext s : ctx.sentencia()) {
                visit(s);
            }
        }
        return null;
    }

    @Override
    public String visitExprIdentificador(MiLenguajeParser.ExprIdentificadorContext ctx) {
        return ctx.ID().getText();
    }

    @Override
    public String visitExprEntero(MiLenguajeParser.ExprEnteroContext ctx) {
        return ctx.INTEGER().getText();
    }

    @Override
    public String visitExprDecimal(MiLenguajeParser.ExprDecimalContext ctx) {
        return ctx.DECIMAL().getText();
    }

    @Override
    public String visitExprCaracter(MiLenguajeParser.ExprCaracterContext ctx) {
        return ctx.CHARACTER().getText();
    }

    @Override
    public String visitExprCadena(MiLenguajeParser.ExprCadenaContext ctx) {
        return ctx.CADENA().getText();
    }

    @Override
    public String visitExprVerdadero(MiLenguajeParser.ExprVerdaderoContext ctx) {
        return "true";
    }

    @Override
    public String visitExprFalso(MiLenguajeParser.ExprFalsoContext ctx) {
        return "false";
    }

    @Override
    public String visitExprAgrupada(MiLenguajeParser.ExprAgrupadaContext ctx) {
        return visit(ctx.expresion());
    }

    @Override
    public String visitExprLlamada(MiLenguajeParser.ExprLlamadaContext ctx) {
        String funcName = ctx.ID().getText();
        List<String> params = new ArrayList<>();
        if (ctx.expresion() != null) {
            for (MiLenguajeParser.ExpresionContext exprCtx : ctx.expresion()) {
                params.add(visit(exprCtx));
            }
        }
        String args = String.join(", ", params);
        if(!args.isEmpty()) {
            gen.emitir("CALL func_" + funcName + ", " + args);
        } else {
            gen.emitir("CALL func_" + funcName);
        }
        return "RETURN_VALUE";
    }

    @Override
    public String visitExprAccesoArray(MiLenguajeParser.ExprAccesoArrayContext ctx) {
        String id = ctx.ID().getText();
        String val = visit(ctx.expresion());
        String temp = gen.nuevaTemp();
        gen.emitir(temp + " = " + id + "[" + val + "]");
        return temp;
    }

    @Override
    public String visitExprNegativo(MiLenguajeParser.ExprNegativoContext ctx) {
        String val = visit(ctx.expresion());
        String temp = gen.nuevaTemp();
        gen.emitir(temp + " = -" + val);
        return temp;
    }

    @Override
    public String visitExprNot(MiLenguajeParser.ExprNotContext ctx) {
        String val = visit(ctx.expresion());
        String temp = gen.nuevaTemp();
        gen.emitir(temp + " = !" + val);
        return temp;
    }

    private boolean esNumerico(String s) {
        try { Double.parseDouble(s); return true; } catch (NumberFormatException e) { return false; }
    }

    private String foldBinaryOp(String left, String right, String op) {
        if (esNumerico(left) && esNumerico(right)) {
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
        return null;
    }

    private String handleBinaryOp(String left, String right, String op) {
        String folded = foldBinaryOp(left, right, op);
        if (folded != null) return folded;
        String temp = gen.nuevaTemp();
        gen.emitir(temp + " = " + left + " " + op + " " + right);
        return temp;
    }

    @Override
    public String visitExprMultiplicativa(MiLenguajeParser.ExprMultiplicativaContext ctx) {
        String left = visit(ctx.expresion(0));
        String right = visit(ctx.expresion(1));
        String op = "";
        if (ctx.MUL() != null) op = "*";
        else if (ctx.DIV() != null) {
            op = "/";
            if ("0".equals(right) || "0.0".equals(right)) {
                System.err.println("Advertencia: División por cero literal detectada.");
            }
        }
        else if (ctx.MOD() != null) op = "%";
        return handleBinaryOp(left, right, op);
    }

    @Override
    public String visitExprAditiva(MiLenguajeParser.ExprAditivaContext ctx) {
        String left = visit(ctx.expresion(0));
        String right = visit(ctx.expresion(1));
        String op = "";
        if (ctx.SUM() != null) op = "+";
        else if (ctx.RES() != null) op = "-";
        return handleBinaryOp(left, right, op);
    }

    @Override
    public String visitExprRelacional(MiLenguajeParser.ExprRelacionalContext ctx) {
        String left = visit(ctx.expresion(0));
        String right = visit(ctx.expresion(1));
        String op = "";
        if (ctx.MAYOR() != null) op = ">";
        else if (ctx.MENOR() != null) op = "<";
        else if (ctx.MAYOR_IGUAL() != null) op = ">=";
        else if (ctx.MENOR_IGUAL() != null) op = "<=";
        return handleBinaryOp(left, right, op);
    }

    @Override
    public String visitExprIgualdad(MiLenguajeParser.ExprIgualdadContext ctx) {
        String left = visit(ctx.expresion(0));
        String right = visit(ctx.expresion(1));
        String op = "";
        if (ctx.EQL() != null) op = "==";
        else if (ctx.DISTINTO() != null) op = "!=";
        return handleBinaryOp(left, right, op);
    }

    @Override
    public String visitExprAnd(MiLenguajeParser.ExprAndContext ctx) {
        String left = visit(ctx.expresion(0));
        String right = visit(ctx.expresion(1));
        return handleBinaryOp(left, right, "&&");
    }

    @Override
    public String visitExprOr(MiLenguajeParser.ExprOrContext ctx) {
        String left = visit(ctx.expresion(0));
        String right = visit(ctx.expresion(1));
        return handleBinaryOp(left, right, "||");
    }
}