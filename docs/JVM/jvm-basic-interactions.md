# **Module 1.5 — Practical JVM Interaction & Profiling Flow**

---

## **Purpose**

Learn to inspect, monitor, and profile any running Java process — from initial discovery to GC, heap, and thread analysis — using built-in JDK tools and programmatic interfaces.

---

## **1. Discover Running JVM Processes**

### **Concept**

Before monitoring or profiling, identify the target JVM process.
Each running JVM instance has a unique **PID (Process ID)**.

### **Commands**

```bash
# Lists all JVM processes and their main classes/jars
jps -l

# Lists processes with full command line (more generic)
pgrep -a java

# Or, full detail on Unix systems
ps -ef | grep java
```

**Tips:**

* Use `jps -v` to see the JVM startup flags (`-Xmx`, GC options).
* Combine with `top -p $(pgrep -d',' java)` to monitor CPU/memory live.

---

## **2. Inspect JVM Configuration**

### **Concept**

JVMs expose their runtime flags and environment.
You can inspect them without restarting the process.

### **Commands**

```bash
# Show all JVM flags and system properties
jcmd <PID> VM.flags

# View or modify system properties live
jinfo <PID>

# Dump basic VM info (version, args, classpath)
jcmd <PID> VM.info
```

**Know-how:**

* Use `jinfo` to verify if `UseG1GC`, `MaxHeapSize`, or `MetaspaceSize` are active.
* Helps confirm real runtime config vs what build tools declare.

---

## **3. Observe Memory & Garbage Collection**

### **Concept**

Monitor heap usage, GC frequency, and allocation rates.
These metrics show if the app is leaking, over-allocating, or mis-tuned.

### **Commands**

```bash
# Realtime GC stats every 1 second
jstat -gc <PID> 1000

# Heap configuration and current usage
jmap -heap <PID>

# Object histogram (number and size per class)
jmap -histo <PID>

# Dump entire heap to file for offline analysis
jmap -dump:format=b,file=heap.bin <PID>
```

**Tips:**

* Analyze `heap.bin` with **VisualVM** or **Eclipse MAT**.
* Use `-Xlog:gc*,gc+heap*` at startup to collect detailed GC logs.

**Programmatic example:**

```java
import java.lang.management.*;

public class HeapMonitor {
    public static void main(String[] args) {
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        System.out.println("Heap: " + mem.getHeapMemoryUsage());
        System.out.println("Non-Heap: " + mem.getNonHeapMemoryUsage());
    }
}
```

---

## **4. Capture and Analyze Threads**

### **Concept**

Thread dumps show thread states, locks, and stack traces — essential for diagnosing deadlocks and contention.

### **Commands**

```bash
# Dump all thread stacks
jstack <PID>

# Print thread info using the jcmd interface
jcmd <PID> Thread.print
```

**Detect deadlocks:**

```bash
jstack <PID> | grep -A5 "Found one Java-level deadlock"
```

**Programmatic access:**

```java
ThreadMXBean threads = ManagementFactory.getThreadMXBean();
System.out.println("Threads: " + threads.getThreadCount());
System.out.println("Peak Threads: " + threads.getPeakThreadCount());
```

---

## **5. Enable JMX for Remote Monitoring**

### **Concept**

JMX (Java Management Extensions) exposes internal JVM metrics (MBeans).
You can connect remotely with tools like **JConsole** or **VisualVM**.

### **Startup Flags**

```bash
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9010
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false
```

### **Connect via Tools**

* **JConsole** → `Remote Process → localhost:9010`
* **VisualVM** → Add JMX connection manually

**Common MBeans:**

* `java.lang:type=Memory`
* `java.lang:type=Threading`
* `java.lang:type=GarbageCollector,*`

**Quick scripting alternative (JMXTerm):**

```bash
open localhost:9010
bean java.lang:type=Memory
get HeapMemoryUsage
```

---

## **6. Profile CPU and Memory**

### **Concept**

Profiling identifies “hot” methods or memory allocation hotspots.

### **Commands**

```bash
# Print available performance counters
jcmd <PID> PerfCounter.print

# Start sampling CPU usage in VisualVM
# -> Open process -> Sampler -> CPU or Memory
```

**Programmatic approach:**

```java
OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
System.out.println("CPU Load: " + os.getSystemLoadAverage());
```

**Tip:**
Run short load scenarios to compare GC activity and CPU trends.

---

## **7. Automate Routine Inspections**

Combine commands in a simple shell script:

```bash
PID=$(pgrep -f 'java.*MyApp')
echo "Monitoring PID: $PID"

jstat -gc $PID 2000 > gc_stats.txt &
jstack -l $PID > threads.txt
jcmd $PID VM.flags > flags.txt
```

---

## **8. Recommended Order of Operations**

| Phase         | Goal                  | Commands                            |
| ------------- | --------------------- | ----------------------------------- |
| Discovery     | Identify running JVMs | `jps`, `pgrep`                      |
| Configuration | View flags            | `jcmd VM.flags`, `jinfo`            |
| Memory        | Monitor heap/GC       | `jstat`, `jmap`                     |
| Threads       | Diagnose locks        | `jstack`, `jcmd Thread.print`       |
| Monitoring    | Observe live metrics  | JMX / VisualVM                      |
| Profiling     | Find hot spots        | VisualVM / `jcmd PerfCounter.print` |

---

## **Key Learnings**

* Always start from **process discovery**, never guess PIDs.
* Collect **heap, thread, and GC** data before optimizing.
* Use **JMX** for remote observability and **VisualVM** for safe live profiling.
* Combine tooling and **MXBeans** for repeatable experiments.