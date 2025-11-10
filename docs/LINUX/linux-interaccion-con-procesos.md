# 🧩 Unidad 1.3 — Inspección práctica de procesos en Linux

> **Objetivo:** dominar las herramientas de observación, filtrado y análisis de procesos del sistema, con foco en la identificación y control de JVMs activas.

---

## 🧠 Conceptos clave

* Un **proceso** es una instancia de un programa en ejecución identificada por un **PID**.
* Linux permite inspeccionar sus atributos (usuario, CPU, memoria, comando, jerarquía) usando el subsistema `/proc` y utilidades clásicas.
* Estas herramientas son la base para diagnosticar, perfilar o monitorear aplicaciones Java en producción.

---

## 🧩 Bloque 1 — Identificación y listado

### 🔹 `ps` — Mostrar procesos activos

```bash
ps -ef
ps -fp 1464441
```

**Intención:** listar información detallada sobre procesos activos.
**Flags comunes:**

| Flag       | Significado                                 |
| ---------- | ------------------------------------------- |
| `-e`       | Todos los procesos del sistema              |
| `-f`       | Formato extendido (UID, PID, PPID, comando) |
| `-p <pid>` | Solo el proceso indicado                    |

**Ejemplo:**

```bash
ps -fp 1464441
```

Muestra información completa del proceso con PID `1464441`.
Suele usarse antes de inspeccionar una JVM con `jcmd` o `jmap`.

---

### 🔹 `pgrep` — Buscar procesos por patrón

```bash
pgrep -a java
pgrep -f MyApp
```

**Intención:** localizar procesos por nombre o comando.
**Flags útiles:**

| Flag | Descripción                                                       |
| ---- | ----------------------------------------------------------------- |
| `-a` | Muestra PID y comando completo                                    |
| `-f` | Hace coincidir contra toda la línea de comando, no solo el nombre |
| `-l` | Incluye el nombre del proceso                                     |

**Uso práctico:**

```bash
pgrep -f 'java.*spring'    # Encuentra la JVM del servicio Spring
```

---

### 🔹 `pstree` — Ver jerarquía de procesos

```bash
pstree -p
```

**Intención:** visualizar la relación padre-hijo entre procesos.
**Útil para:** ver qué lanzó una JVM (por ejemplo, Gradle o systemd).

---

## 🧩 Bloque 2 — Filtrado y composición

### 🔹 `grep` — Filtrar texto

```bash
ps -ef | grep java
```

**Intención:** aislar líneas que contienen un patrón.
**Combinaciones frecuentes:**

* `ps -fp $(pgrep -f MyApp) | grep java`
* `netstat -tulnp | grep 8080`
* `lsof -p 1464441 | grep jar`

---

### 🔹 Piping (`|`) — Conectar comandos

**Intención:** encadenar herramientas para procesar datos paso a paso.
Ejemplo:

```bash
ps -eo pid,comm,%mem,%cpu --sort=-%mem | head -5
```

Muestra los 5 procesos que más memoria consumen.

---

## 🧩 Bloque 3 — Monitoreo y control

### 🔹 `top` / `htop` — Vista en tiempo real

```bash
top -p 1464441
```

**Intención:** observar consumo de CPU y memoria.
`htop` ofrece versión interactiva con búsqueda (`/java`).

---

### 🔹 `kill` / `pkill` / `killall` — Enviar señales

```bash
kill -9 1464441
pkill -f MyApp
```

**Intención:** terminar o reiniciar procesos.

| Señal        | Significado                  |
| ------------ | ---------------------------- |
| `-15 (TERM)` | Cierre limpio (por defecto)  |
| `-9 (KILL)`  | Forzar terminación inmediata |

---

### 🔹 `nice` / `renice` — Prioridad de CPU

```bash
nice -n 10 java MyApp.jar
renice 5 -p 1464441
```

**Intención:** ajustar la prioridad de planificación del proceso.

---

## 🧩 Bloque 4 — Integración con la JVM

### 🔹 `jps` — Listar JVMs activas

```bash
jps -lv
```

**Intención:** listar procesos Java junto con su clase principal y parámetros.

---

### 🔹 `ps -fp $(pgrep -f java)` — Inspección avanzada

**Intención:** ver los argumentos de inicio exactos de una JVM.
Permite validar:

* `-Xmx`, `-Xms`, `-XX:+UseG1GC`, etc.
* rutas de `gc.log` o `heap dumps`.
* usuario que la ejecuta.

---

### 🔹 `lsof -p <pid>` — Archivos abiertos por el proceso

**Intención:** verificar logs, sockets o pipes activos dentro de una JVM.

---

## 🧩 Bloque 5 — Diagnóstico práctico

### Ejercicios rápidos

1. Identificar todos los procesos Java:

   ```bash
   pgrep -a java
   ```
2. Mostrar el árbol de Gradle → JVM:

   ```bash
   pstree -ap $(pgrep -f gradle)
   ```
3. Ver parámetros del heap de la JVM:

   ```bash
   ps -fp $(pgrep -f MyApp) | grep Xmx
   ```
4. Medir uso de CPU en tiempo real:

   ```bash
   top -p $(pgrep -f MyApp)
   ```