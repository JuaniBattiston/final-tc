package com.compilador;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.gui.TreeViewer;
import com.compilador.AnalisisSemantico.AnalizadorSemantico;
import javax.swing.*;
import java.util.Arrays;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class App {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Uso: java -jar demo-1.0-jar-with-dependencies.jar <archivo.txt>");
            System.exit(1);
        }

        try {
            CharStream input = CharStreams.fromFileName(args[0]);
            System.out.println("Analizando archivo: " + args[0]);
            System.out.println("=".repeat(65));

            MiLenguajeLexer lexer = new MiLenguajeLexer(input);

            List<String> erroresLexicos = new ArrayList<>();
            lexer.removeErrorListeners();
            lexer.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer,
                                        Object offendingSymbol,
                                        int line, int charPositionInLine,
                                        String msg, RecognitionException e) {
                    erroresLexicos.add(
                        "  [Linea " + line + ":" + charPositionInLine + "] " + msg
                    );
                }
            });

            CommonTokenStream tokens = new CommonTokenStream(lexer);
            tokens.fill();

            System.out.println("\n=== FASE 1: ANALISIS LEXICO ===\n");
            System.out.printf("  %-20s %-25s %-8s %-8s%n",
                              "TIPO DE TOKEN", "LEXEMA", "LINEA", "COLUMNA");
            System.out.println("  " + "-".repeat(63));

            for (Token token : tokens.getTokens()) {
                if (token.getType() == Token.EOF) continue;

                String tipo = MiLenguajeLexer.VOCABULARY.getSymbolicName(token.getType());
                if (tipo == null) tipo = "DESCONOCIDO";

                System.out.printf("  %-20s %-25s %-8d %-8d%n",
                                  tipo,
                                  token.getText(),
                                  token.getLine(),
                                  token.getCharPositionInLine());
            }

            if (!erroresLexicos.isEmpty()) {
                System.out.println("\n  ERRORES LEXICOS:");
                for (String error : erroresLexicos) {
                    System.out.println(error);
                }
                System.out.println("\n  El analisis no puede continuar con errores lexicos.");
                return;
            }

            System.out.println("\n  Analisis lexico completado sin errores.");

            System.out.println("\n=== FASE 2: ANALISIS SINTACTICO ===\n");

            tokens.reset();

            MiLenguajeParser parser = new MiLenguajeParser(tokens);

            List<String> erroresSintacticos = new ArrayList<>();
            parser.removeErrorListeners();
            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer,
                                        Object offendingSymbol,
                                        int line, int charPositionInLine,
                                        String msg, RecognitionException e) {
                    String tokenErroneo = (offendingSymbol != null)
                                         ? "'" + offendingSymbol + "'"
                                         : "fin de archivo";
                    erroresSintacticos.add(
                        "  [Linea " + line + ":" + charPositionInLine + "] "
                        + "cerca de " + tokenErroneo + " -> " + msg
                    );
                }
            });

            MiLenguajeParser.ProgramaContext arbolParseo = parser.programa();

            if (!erroresSintacticos.isEmpty()) {
                System.out.println("  ERRORES SINTACTICOS:");
                for (String error : erroresSintacticos) {
                    System.out.println(error);
                }
                System.out.println();
                System.out.println("  Pista: revisa que cada sentencia:");
                System.out.println("    - Termine con punto y coma ';'");
                System.out.println("    - Tenga parentesis balanceados");
                System.out.println("    - Use tipos validos (int, float, string, bool, char, double)");
                return;
            }

            System.out.println("  Analisis sintactico completado sin errores.");

            System.out.println("\n=== FASE 3: ANALISIS SEMANTICO ===\n");
            System.out.println("   Tabla de simbolos:");

            AnalizadorSemantico semantico = new AnalizadorSemantico();
            semantico.visit(arbolParseo);
            semantico.getTabla().imprimir();

            if (semantico.hayErrores()) {
                System.out.println("\nERRORES SEMANTICOS:");
                for (String error : semantico.getErrores()) {
                    System.out.println("   Error: " + error);
                }
            }

            if (semantico.hayAdvertencias()) {
                System.out.println("\nWARNINGS SEMANTICOS:");
                for (String adv : semantico.getAdvertencias()) {
                    System.out.println("   " + adv);
                }
                System.out.println("   El codigo tiene warnings, pero se puede continuar.");
            }

            if (semantico.hayErrores()) {
                System.out.println("\nCompilacion detenida debido a errores semanticos.");
                return;
            }

            if (!semantico.hayErrores() && !semantico.hayAdvertencias()) {
                System.out.println("\n  Analisis semantico completado sin errores.");

                System.out.println("\n=== FASE 4: GENERACION DE CODIGO INTERMEDIO ===\n");

                GeneradorCodigo gen = new GeneradorCodigo();
                CodigoVisitor visitor = new CodigoVisitor(gen);
                visitor.visit(arbolParseo);

                System.out.println("   Codigo de tres direcciones (ANTES de optimizaciones):\n");
                gen.imprimir();
                gen.guardar("codigo_intermedio.txt");

                String base = args[0].replace(".txt", "").replace(".cpp", "");

                System.out.println("\n=== FASE 5: OPTIMIZACION DE CODIGO ===");

                System.out.println("\n   OPT-1: Constant Folding");
                OptSimplificacionExpresiones opt1 = new OptSimplificacionExpresiones();
                List<String> folds = opt1.optimizar(gen.getInstrucciones());
                gen.guardar(base + "_opt1.txt");
                if (!folds.isEmpty()) {
                    System.out.printf("   Expresiones simplificadas: %d%n", folds.size());
                    for (String fold : folds) {
                        System.out.println("      " + fold);
                    }
                } else {
                    System.out.println("   No se encontraron expresiones literales para simplificar.");
                }

                System.out.println("\n   OPT-2: Propagacion de constantes");
                OptPropagacionConstantes opt2 = new OptPropagacionConstantes();
                List<String> propag = opt2.optimizar(gen.getInstrucciones());
                gen.guardar(base + "_opt2.txt");
                if (!propag.isEmpty()) {
                    System.out.printf("   Sustituciones realizadas: %d%n", propag.size());
                    for (String p : propag) {
                        System.out.println("      " + p);
                    }
                } else {
                    System.out.println("   No se encontraron variables constantes para propagar.");
                }

                System.out.println("\n   OPT-3: Eliminacion de codigo muerto");
                OptEliminacionMuerto opt3 = new OptEliminacionMuerto();
                List<String> muertas = opt3.optimizar(gen.getInstrucciones());
                gen.guardar(base + "_opt3.txt");
                if (!muertas.isEmpty()) {
                    System.out.printf("   Temporarias eliminadas: %d%n", muertas.size());
                    for (String m : muertas) {
                        System.out.println("      " + m);
                    }
                } else {
                    System.out.println("   No se encontraron temporarias muertas para eliminar.");
                }

                System.out.println("\n   Codigo optimizado (DESPUES de las tres optimizaciones):\n");
                gen.imprimir();

                int instrFinal    = gen.cantidadInstrucciones();
                int elimFolding   = folds.size();
                int elimMuertas   = muertas.size();
                int instrOriginal = instrFinal + elimFolding + elimMuertas;
                double reduccion  = instrOriginal > 0 ? (100.0 - (instrFinal * 100.0 / instrOriginal)) : 0;

                System.out.println("\n   Resumen:");
                System.out.printf("      Sin optimizar: %d instrucciones%n", instrOriginal);
                System.out.printf("      Tras OPT-1:    %d  (-%d)%n", instrOriginal - elimFolding, elimFolding);
                System.out.printf("      Tras OPT-2+3:  %d  (-%d)%n", instrFinal, elimMuertas);
                System.out.printf("      Reduccion:     %.2f%%%n", reduccion);
            }

            System.out.println("\n" + "=".repeat(65));
            System.out.println("  Compilacion exitosa.");

            System.out.println("\n  Abriendo visualizador grafico del arbol...");
            mostrarArbol(arbolParseo, parser);

        } catch (IOException e) {
            System.err.println("No se pudo leer el archivo: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error inesperado: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void mostrarArbol(ParseTree tree, Parser parser) {
        JFrame frame = new JFrame("Arbol Sintactico");
        JPanel panel = new JPanel();
        TreeViewer viewer = new TreeViewer(Arrays.asList(parser.getRuleNames()), tree);
        viewer.setScale(1.5);
        panel.add(viewer);

        JScrollPane scrollPane = new JScrollPane(panel);
        frame.add(scrollPane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setVisible(true);
    }
}
