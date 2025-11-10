# **JVM Interaction & Profiling**

---

## 1. The JVM as a Managed Runtime

Java doesn’t execute directly on the OS — it runs inside the **Java Virtual Machine (JVM)**, which abstracts hardware, memory, and threading.
Understanding its layers is key to performance work:

```
[ Application Code ]
        ↓
[ Bytecode ] → interpreted or JIT-compiled
        ↓
[ HotSpot JVM ]
        ↓
[ OS / Hardware ]
```

The JVM dynamically manages:

* **Memory** (heap, metaspace)
* **Threads**
* **Garbage collection**
* **JIT compilation**

---

## 2. The Memory Model

The JVM isolates **memory regions** to balance allocation speed and GC efficiency.

| Area                 | Description                                                        | Key Parameters         |
| -------------------- | ------------------------------------------------------------------ | ---------------------- |
| **Heap**             | Where all Java objects live. Split into young and old generations. | `-Xms`, `-Xmx`         |
| **Young Generation** | Short-lived objects (allocated frequently, collected fast).        |                        |
| **Old Generation**   | Long-lived or promoted objects.                                    |                        |
| **Metaspace**        | Stores class metadata (replaces PermGen).                          | `-XX:MaxMetaspaceSize` |
| **Stack**            | Each thread’s local frame storage for methods and variables.       |                        |

Allocation flow:

```
Eden → Survivor → Old Gen
```

The collector clears dead objects in young regions often, leaving old-gen GCs infrequent but heavier.

---

## 3. Garbage Collection Fundamentals

GC automates memory reclamation. You trade *control* for *safety*.

### Why tuning matters

Pauses during GC stop all threads (“stop-the-world” events).
For short-lived, low-latency services, these pauses must be predictable.

### Main algorithms (HotSpot)

| Collector              | Strategy                          | When to use                         |
| ---------------------- | --------------------------------- | ----------------------------------- |
| **Serial**             | Single-threaded mark-sweep        | Small heaps (<100MB)                |
| **Parallel**           | Multi-threaded mark-sweep         | Batch workloads needing throughput  |
| **G1 (Garbage First)** | Region-based, concurrent marking  | Default for modern JVMs             |
| **ZGC**                | Concurrent, low-pause             | Large heaps, near-real-time systems |
| **Shenandoah**         | Like ZGC, open-source alternative | Similar latency goals               |

### Basic GC tuning flags

```bash
-Xms512m -Xmx2g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:InitiatingHeapOccupancyPercent=45
```

The collector’s behavior defines the runtime “rhythm” of the app — more throughput vs. more responsiveness.

---

## 4. Observation: JVM as a Live System

Once the app runs, introspection begins.
The JVM exposes internal data structures for *observation, not modification* through **JMX** and built-in tools.

> A good way to check if the app is running is to use `top -p $(pgrep -d',' java)`

### JMX (Java Management Extensions)

* Standard interface exposing metrics as **MBeans**.
* Default domains: `java.lang`, `java.nio`, `java.util.logging`.
* Each MBean provides **attributes** (values) and **operations** (actions).

Enable remote access:

```bash
-Dcom.sun.management.jmxremote.port=9010
-Dcom.sun.management.jmxremote.authenticate=false
```

You can then connect using:

* `jconsole`
* `jvisualvm`
* Custom code via `ManagementFactory.getMemoryMXBean()`

---

## 5. Profiling & Diagnostic Toolchain

HotSpot provides native CLI tools for introspection:

| Tool     | Purpose                                                |
| -------- | ------------------------------------------------------ |
| `jstat`  | Runtime stats: GC cycles, memory usage, class loading. |
| `jmap`   | Heap dump, object histogram.                           |
| `jstack` | Thread dump (state, locks, deadlocks).                 |
| `jcmd`   | Unified command interface for JVM operations.          |

### Typical pattern:

```bash
PID=$(pgrep -f 'java.*MyApp')
jmap -heap $PID         # Inspect heap configuration
jstack -l $PID          # Thread dump
jstat -gc $PID 1000     # GC activity every second
```

Combine with GC logs:

```bash
java -Xmx2g -XX:+UseG1GC \
     -Xlog:gc*,gc+heap*:/tmp/gc.log:time,uptime \
     -jar app.jar
tail -f /tmp/gc.log
```

You can now **correlate** heap growth, GC frequency, and pause times.

---

## 6. From Observation to Control

### a. Code-level metrics

Expose memory info via MXBeans:

```java
MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
System.out.println("Heap: " + mem.getHeapMemoryUsage());
```

### b. Performance interpretation

| Symptom         | Possible Cause            | Diagnostic Tool          |
| --------------- | ------------------------- | ------------------------ |
| Frequent GC     | Heap too small            | `jstat`, GC logs         |
| Long GC pauses  | Old gen compaction        | GC log timestamps        |
| Memory leak     | Static references, caches | `jmap -histo`, heap dump |
| Thread deadlock | Synchronization error     | `jstack`                 |

---

## 7. Hands-on Lab Summary

| Goal                | Task                                    | Expected Learning         |
| ------------------- | --------------------------------------- | ------------------------- |
| **Heap sizing**     | Allocate objects until GC churns        | Understand heap pressure  |
| **Leak simulation** | Static `List<>` retains objects         | Practice dump & histogram |
| **Deadlock demo**   | Two threads locking in reverse order    | Detect with `jstack`      |
| **JMX query**       | Use `jmxterm` → `java.lang:type=Memory` | Inspect live heap stats   |

---

## 8. Reading References (for validation)

| Topic                          | Source                                                                             |
| ------------------------------ | ---------------------------------------------------------------------------------- |
| **HotSpot GC Tuning Guide**    | [Oracle Docs](https://docs.oracle.com/javase/8/docs/technotes/guides/vm/gctuning/) |
| **JMX & Management Guide**     | [Oracle Docs](https://docs.oracle.com/javase/8/docs/technotes/guides/management/)  |
| **VisualVM Docs**              | [visualvm.github.io](https://visualvm.github.io/documentation.html)                |
| **OpenJDK Tools**              | [openjdk.org/tools](https://openjdk.org/tools/)                                    |
| **ZGC & Shenandoah Internals** | [wiki.openjdk.org](https://wiki.openjdk.org/display/zgc/Main)                      |
---

---

## 9. Key Takeaways

* JVM behavior is observable and tunable, not opaque.
* GC choice affects latency, not correctness.
* JMX is the universal introspection layer.
* Profiling should be routine, not reactive.
* The goal isn’t “no GC,” but **predictable GC**.
---
