# 3. Manual de Usuario

Este documento está dirigido a los usuarios, evaluadores y programadores interesados en compilar y utilizar el proyecto "Compilador del Subconjunto de C++".

## 3.1. Instrucciones de instalación

Para poder ejecutar el compilador correctamente, deberá contar con las siguientes dependencias instaladas en su entorno:

1. Java Development Kit (JDK 11+ o superior).
2. Apache Maven (Utilizado para la gestión de dependencias y ejecución de empaquetado de ANTLR4).

Pasos para la instalación:

1. Extraer abrir la consola (terminal) localizándose en la carpeta de origen `demo/`.
2. Ejecutar el comando para limpiar el entorno y descargar las dependencias de ANTLR:
   `mvn clean install` o `mvn clean package`.
3. Esto generará en la carpeta o bien instalará las clases bajo `target/classes`. También producirá el empaquetado compilado si utiliza una directiva package, por ejemplo: `demo-1.0-jar-with-dependencies.jar`.

## 3.2. Guía de uso del compilador

Una vez construido el proyecto, la ejecución se invoca pasando como único argumento el archivo de texto que contiene el código fuente C++ a compilar.

Usando ejecución de Java directa (si cuenta con el `.jar` listo):

```bash
java -jar target/demo-1.0-jar-with-dependencies.jar ejemplo.txt
```

Si prefiere ejecutarlo directamente mediante la clase principal (`App.java`) a través de un IDE (ej: VS Code o IntelliJ), simplemente configure los argumentos de ejecución (args) apuntando al archivo temporal, por ejemplo: `demo/ejemplo.txt`.

El programa procesará su código terminalmente mediante 5 etapas consecutivas reportando visualmente por consola. Finalizando el ciclo, arrojará 3 archivos de texto anexos con las etapas optimizadas del código intermedio resultantes (ej: `ejemplo_opt1.txt`, `ejemplo_opt2.txt`, `ejemplo_opt3.txt`).

## 3.3. Ejemplos de compilación de programas

A continuación se muestra qué proveer como programa (archivo de entrada `.txt`):

```cpp
// Archivo temporal.txt
int variableGlobal;

int main() {
    int x;
    int y;
    x = 10;
    y = 5;

    int resultado = x + y;

    if(resultado > 10){
        cout << resultado;
    }

    return 0;
}
```

Para procesarlo, llame:
`java -jar compilador.jar temporal.txt`

Salidas Esperadas Visuales:

1. FASE 1: LÉXICO (Tabla visual mostrando Lexemas, Líneas y Columnas).
2. FASE 2: SINTÁCTICO (Validación y construcción de árbol).
3. FASE 3: SEMÁNTICO (Tabla de símbolos completa).
4. FASE 4 y 5: GENERACIÓN Y OPTIMIZACIÓN (Emitirá la generación de ficheros como `temporal_opt1.txt`).

## 3.4. Interpretación de los mensajes de error y warning

El programa provee tolerancia y manejo amistoso frente a fallos. Poseen un estándar fácil de leer:

- Error Léxico:
  Aparece en pantalla indicando: `[Línea X:Y] token unrecognized`. Significa que introdujo un carácter fuera de nuestro subconjunto de lenguaje (ej. `$`, o `&` aislado). Detendrá el proceso para prevenir corrupción.
- Error Sintáctico:
  Símbolo desencajado de lugar. Usualmente sucede si olvida cerrar una llave `}`, obvia un `;` o se interrumpe mal una operación matemática. El reporte le indicará `[Línea X:Y] cerca de 'token_erróneo'`.

- Errores Semánticos (Visibles tras la muestra de Tabla de Símbolos):
  - "Variable duplicada": Trató de hacer, por ejemplo, dos `int x;` en un mismo ámbito lógico.
  - "Asignación a función": Pretendió asignar un valor estático a la llamada bruta de una función como identificador.
  - "No pre-declarada": Instanciar asignación en un valor inexistente (ej. `variableFantasma = 10;`). Todas estas frenarán la generación del intermedio.

- Warnings Semánticos (Advertencias):
  - `Warning: Variable 'X' declarada pero nunca utilizada en el ámbito 'Y'`.
    Este es un aviso no fatal. Informa como "ayuda al programador" que declaró una variable y su gasto en memoria era completamente innecesario; pero el hilo de generación continuará sin detener la compilación de forma normal.
