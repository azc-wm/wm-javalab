# ⚙️ Unidad 2 — Interacción con la JVM desde LINUX

> **Objetivo:** dominar las utilidades del JDK para inspeccionar, controlar y diagnosticar el estado interno de una Java Virtual Machine (JVM) activa desde el sistema operativo.

---

## 🧠 Conceptos clave

* Cada JVM es un **proceso Linux** que puede ser interrogado mediante herramientas del **JDK**.
* Estas herramientas permiten acceder a:

    * Parámetros de ejecución (`-Xmx`, `GC`, `flags`).
    * Estado del heap y del garbage collector.
    * Hilos, locks y deadlocks.
    * Dumps y estadísticas de rendimiento.

---

## 🧩 Bloque 1 — Localización y contexto

### 🔹 `jps` — Java Process Status

```bash
jps -lv
```

**Intención:** listar JVMs activas en el sistema con clase principal y argumentos.

| Opción | Descripción                                  |
| ------ | -------------------------------------------- |
| `-l`   | Muestra el nombre completo de la clase o JAR |
| `-v`   | Incluye los argumentos JVM                   |
| `-q`   | Solo muestra PIDs                            |

**Ejemplo:**

```bash
jps -lv
# 1428903 org.wm.springlab.app.Application -Xmx1g -XX:+UseG1GC
```

---

### 🔹 `ps` + `pgrep` + `grep`

```bash
ps -fp $(pgrep -f MyApp)
```

**Intención:** obtener la línea de ejecución completa de una JVM, incluyendo `-Xms`, `-Xmx`, `GC`, y rutas de logs.

---

## 🧩 Bloque 2 — Consultas dinámicas de estado

### 🔹 `jcmd` — Interfaz principal de diagnóstico

```bash
jcmd <pid> help
```

**Intención:** listar todos los comandos disponibles para esa JVM.
Ejemplo:

```bash
jcmd 1428903 VM.flags
jcmd 1428903 VM.uptime
jcmd 1428903 Thread.print
```

**Comandos más comunes:**

| Comando               | Descripción                                          |
| --------------------- | ---------------------------------------------------- |
| `VM.flags`            | Muestra los flags de arranque                        |
| `GC.heap_info`        | Estado general del heap                              |
| `GC.class_histogram`  | Conteo de objetos por clase                          |
| `Thread.print`        | Dump de hilos                                        |
| `GC.run`              | Ejecuta un GC manual                                 |
| `GC.heap_dump <ruta>` | Genera un heap dump (requiere permisos de escritura) |

**Ejemplo práctico:**

```bash
jcmd 1428903 GC.heap_info
jcmd 1428903 GC.class_histogram | head -20
```

> 💡 Si `GC.heap_dump` falla con *“No such file or directory”*, la ruta no es válida desde el contexto del proceso. Usa rutas absolutas y permisos accesibles:
>
> ```bash
> jcmd 1428903 GC.heap_dump /tmp/heap.hprof
> ```

---

## 🧩 Bloque 3 — Análisis de memoria y GC

### 🔹 `jmap` — Mapa de memoria y dumps

```bash
jmap -heap <pid>
jmap -histo <pid> | head -20
jmap -dump:live,format=b,file=/tmp/heap.hprof <pid>
```

**Intención:** inspeccionar estructura del heap o crear un dump binario.

> Desde JDK 21, algunas funciones se reemplazan por `jcmd`.

---

### 🔹 `jstat` — Estadísticas del GC y compilador

```bash
jstat -gc <pid> 1s 10
```

**Intención:** muestrea cada segundo el estado del heap.
Columnas clave:

* `S0C/S1C`: tamaño de Survivor spaces.
* `EC/OC`: Eden y Old generation.
* `YGC/FGC`: número de Young y Full GCs.

Ejemplo:

```bash
jstat -gc 1428903 1000
```

Produce salida continua hasta interrupción (`Ctrl + C`).

---

## 🧩 Bloque 4 — Inspección de hilos

### 🔹 `jstack` — Stack trace de hilos

```bash
jstack 1428903 > /tmp/thread_dump.txt
```

**Intención:** capturar el estado de todos los hilos, bloqueos y deadlocks.

> Ideal para detectar bloqueos o saturaciones por pools de threads.

---

## 🧩 Bloque 5 — Configuración de diagnóstico automático

### 📄 Flags JVM útiles

| Flag                                                                  | Descripción                   |
| --------------------------------------------------------------------- | ----------------------------- |
| `-XX:+HeapDumpOnOutOfMemoryError`                                     | Genera dump al fallar por OOM |
| `-XX:HeapDumpPath=/tmp/heap.hprof`                                    | Ruta del dump                 |
| `-Xlog:gc*,safepoint:file=/var/log/jvm_gc.log:time,uptime,level,tags` | Log detallado de GC           |

**Ejemplo Gradle (build.gradle):**

```groovy
application {
    applicationDefaultJvmArgs = [
        "-XX:+UseG1GC",
        "-Xmx1g",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:HeapDumpPath=/tmp/heap.hprof",
        "-Xlog:gc*,gc+heap*,safepoint,class+load,class+unload:file=tmp/gc.log:time,uptime,level,tags"
    ]
}
```

---

## 🧩 Bloque 6 — Flujos de diagnóstico integrados

### Escenario práctico

1. **Identificar la JVM:**

   ```bash
   jps -lv
   ```
2. **Consultar su configuración:**

   ```bash
   jcmd <pid> VM.flags
   ```
3. **Forzar un GC y ver estado:**

   ```bash
   jcmd <pid> GC.run
   jcmd <pid> GC.heap_info
   ```
4. **Generar un dump:**

   ```bash
   jcmd <pid> GC.heap_dump /tmp/heap.hprof
   ```
5. **Analizar con VisualVM o Eclipse MAT.**