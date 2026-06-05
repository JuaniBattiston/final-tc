# Compilador Educativo — Mini Lenguaje C++

Proyecto de Técnicas de Compilación.  
Implementa cuatro fases de un compilador: **análisis léxico**, **análisis sintáctico**, **análisis semántico** y **generación de código intermedio**.

---

## Estructura del proyecto

```
demo/
├── src/main/antlr4/com/compilador/
│   └── MiLenguaje.g4                    <- gramática ANTLR4 (lexer + parser)
├── src/main/java/com/compilador/
│   ├── App.java                          <- punto de entrada
│   ├── ImprimirVisitor.java              <- visitor del árbol sintáctico
│   ├── CodigoVisitor.java                <- visitor para cod. intermedio (el arquitecto)
│   ├── GeneradorCodigo.java              <- manejo de cod. de 3 direcciones (el constructor)
│   └── AnalisisSemantico/
│       ├── Simbolo.java                  <- entrada de la tabla de símbolos
│       ├── TablaSimbolos.java            <- tabla de símbolos con impresión
│       └── AnalizadorSemantico.java      <- visitor semántico
├── ejemplo.txt                           <- programa válido básico
├── ejemplo_error.txt                     <- programa con errores sintácticos
├── ejemplo_con_errores.txt               <- programa con errores semánticos
└── pom.xml                               <- configuración Maven
```

Archivos **generados automáticamente** por ANTLR4 (no editarlos):
```
src/main/java/com/compilador/
├── MiLenguajeLexer.java
├── MiLenguajeParser.java
├── MiLenguajeVisitor.java
└── MiLenguajeBaseVisitor.java
```

---

## Cómo compilar y ejecutar

```bash
cd demo
mvn clean package

java -jar target/demo-1.0-jar-with-dependencies.jar ejemplo.txt
```

---

## Construcciones soportadas

| Construcción         | Ejemplo                              |
|----------------------|--------------------------------------|
| Declaración variable | `int x = 10;`                        |
| Declaración arreglo  | `int numeros[3];`                    |
| Asignación variable  | `x = x + 1;`                        |
| Asignación arreglo   | `numeros[0] = 10;`                   |
| Función              | `int sumar(int a, int b) { ... }`    |
| Return               | `return resultado;`                  |
| Llamada a función    | `estado = sumar(temp, 5);`           |
| Acceso a arreglo     | `temp = numeros[0] + numeros[1];`    |
| Salida               | `cout << x;`                        |
| Condicional          | `if (x > 0) { ... } else { ... }`   |
| Bucle                | `while (x < 100) { ... }`           |
| Tipos                | `int float double char string bool void` |

---

## Fases del compilador

### Fase 1 — Análisis Léxico

El lexer convierte el texto en tokens. Si hay caracteres no reconocidos reporta errores y detiene el análisis.

### Fase 2 — Análisis Sintáctico

El parser verifica que los tokens formen estructuras válidas según la gramática. Si hay errores los reporta con línea y columna. Si es exitoso abre la ventana gráfica del árbol de parseo.

### Fase 3 — Análisis Semántico

El analizador semántico recorre el árbol y construye la tabla de símbolos. Detecta:
- Variables usadas sin declarar
- Variables redeclaradas en el mismo ámbito

Produce una tabla con el siguiente formato:

```
=== TABLA DE SÍMBOLOS ===
NOMBRE          TIPO       CATEGORÍA       LÍNEA      COLUMNA    ÁMBITO          DETALLES
--------------------------------------------------------------------------------------------
contadorGlobal  int        variable        5          4          global          [private]
sumar           int        funcion         11         4          global          [private] [int, int]
a               int        parametro       11         14         sumar
numeros         int        variable        22         8          main            [arr:3] [private]
```

### Fase 4 — Generación de Código Intermedio

Traduce el código validado a un **código de tres direcciones**, empleando variables temporales (`t1`, `t2`, etc.) y etiquetas (`L1`, `L2`, etc.) para los saltos, facilitando el análisis y la optimización. 

Se implementa mediante una separación de responsabilidades:
- **`CodigoVisitor` (El Arquitecto):** Recorre el AST decidiendo *qué* operaciones ejecutar y *cuándo*. Controla la lógica central, incluyendo manejo de operadores y advertencias como división por cero.
- **`GeneradorCodigo` (El Constructor):** Responsable de la mecánica de generación; se encarga de crear variables temporales únicas, emitir las instrucciones y almacenar todo el resultado.

Al concluir exitosamente, el código intermedio se guarda en un archivo que adopta el nombre del original más el sufijo `_codigo_intermedio.txt`.

### Fase 5 — Optimización de Código

El compilador incluye un pipeline de 3 optimizaciones que se aplican en cadena sobre el código intermedio:

1. **Simplificación de expresiones (Constant Folding):** Se aplica *inline* durante la generación del código intermedio. Evalúa operaciones aritméticas en tiempo de compilación cuando todos los operandos son literales (ej. `3 + 2` se emite directamente como `5`). El resultado se guarda en `<archivo>_opt1.txt`.
2. **Propagación de constantes:** Se aplica como post-proceso sobre la lista de instrucciones generada. Busca asignaciones de literales a variables y reemplaza las apariciones futuras de esa variable por su valor constante correspondiente. El resultado se guarda en `<archivo>_opt2.txt`.
3. **Eliminación de código muerto:** Se aplica como post-proceso sobre el resultado de la propagación. Identifica variables temporales (`t1`, `t2`, etc.) que son asignadas pero nunca se usan como operandos en ninguna instrucción posterior, y elimina esas instrucciones de asignación. El resultado se guarda en `<archivo>_opt3.txt`.

Al finalizar, se imprime un resumen global indicando la cantidad de instrucciones originales, cuántas fueron eliminadas por cada optimización y el porcentaje de reducción total.

---

## Visualizador gráfico

Al completarse el análisis sintáctico se abre una ventana Swing con el árbol de parseo completo generada con `TreeViewer` de ANTLR4. Soporta zoom y scroll.
