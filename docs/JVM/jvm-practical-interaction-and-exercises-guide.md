# **JVM Practical Interaction & Exercises Guide**

*(Companion to the theoretical “JVM Interaction & Profiling” module)*

This chapter provides **guided procedures** for every hands-on example and exercise, including **command-line**, **tool-based**, and **programmatic** interactions. Each guide describes what is being measured, why, and how to interpret results.

---

## **1. GC Logging and Analysis**

### **Goal**

Understand how the Garbage Collector behaves under load and how to interpret its logs.

### **CLI Procedure**

1. Run your application with explicit GC logging:

   ```bash
   java -Xmx2g -XX:+UseG1GC \
        -Xlog:gc*,gc+heap*:/tmp/gc.log:time,uptime \
        -jar app.jar
   ```
2. Stream log output:

   ```bash
   tail -f /tmp/gc.log
   ```
3. Look for:

   * **Pause times:** `Pause Young (Normal) (G1 Evacuation Pause)`
   * **Heap after GC:** `Heap: before X MB → after Y MB`
   * **Frequency:** how often GC triggers at steady state.

### **Interpretation**

If GC runs every few seconds with long pauses → heap too small or excessive object churn.

### **Programmatic View**

Use JMX to watch heap in real time:

```java
import java.lang.management.*;

public class GCInspector {
    public static void main(String[] args) throws InterruptedException {
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        while (true) {
            var usage = mem.getHeapMemoryUsage();
            System.out.printf("Used: %d MB / Max: %d MB%n",
                    usage.getUsed() / (1024*1024),
                    usage.getMax() / (1024*1024));
            Thread.sleep(2000);
        }
    }
}
```

Run it alongside your GC-logged process. The output should correlate with the GC log timestamps.

---

## **2. Heap Inspection with jmap**

### **Goal**

Visualize what lives in memory and identify potential leaks or unexpected retention.

### **CLI Procedure**

```bash
PID=$(pgrep -f 'java.*MyApp')
jmap -heap $PID             # heap config
jmap -histo $PID | head     # top 20 object types
```

Optional full dump:

```bash
jmap -dump:live,format=b,file=/tmp/heap.hprof $PID
```

### **Interpretation**

If a specific class grows monotonically across snapshots, it’s likely leaked (unreleased references).

### **Programmatic View**

Use the `MemoryPoolMXBean` interface:

```java
import java.lang.management.*;

for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
    System.out.printf("%s: %s%n", pool.getName(), pool.getUsage());
}
```

Each pool corresponds to a GC generation (Eden, Survivor, Old Gen).

---

## **3. Thread Dump and Deadlock Detection**

### **Goal**

Analyze thread states and detect deadlocks.

### **CLI Procedure**

```bash
PID=$(pgrep -f 'java.*MyApp')
jstack -l $PID > threaddump.txt
```

Look for lines like:

```
Found one Java-level deadlock:
"Thread-1" waiting to lock monitor 0x00007f8f1809...
"Thread-2" waiting to lock monitor 0x00007f8f1808...
```

### **Programmatic View**

Detect deadlocks from within the process:

```java
import java.lang.management.*;

ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
long[] deadlocked = tbean.findDeadlockedThreads();
if (deadlocked != null) {
    ThreadInfo[] infos = tbean.getThreadInfo(deadlocked, true, true);
    for (ThreadInfo info : infos) {
        System.out.println("Deadlock: " + info);
    }
}
```

---

## **4. Live GC and Memory Stats (jstat)**

### **Goal**

Track GC events and allocation rates over time.

### **CLI Procedure**

```bash
PID=$(pgrep -f 'java.*MyApp')
jstat -gc $PID 1000
```

Output columns:

* `S0C`, `S1C` — Survivor spaces
* `EC`, `EU` — Eden capacity/usage
* `OC`, `OU` — Old gen
* `YGC` / `FGC` — Young/Full GC count

### **Interpretation**

If `FGC` grows rapidly, old-gen pressure is high — tune `-Xmx` or GC strategy.

### **Programmatic View**

Gather similar metrics via MXBeans:

```java
for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
    System.out.printf("%s: %d collections, %d ms total%n",
        gc.getName(),
        gc.getCollectionCount(),
        gc.getCollectionTime());
}
```

---

## **5. Remote JMX Access**

### **Goal**

Connect external tools (VisualVM, Prometheus JMX exporter) to a live JVM.

### **Configuration**

Start app with:

```bash
-Dcom.sun.management.jmxremote.port=9010 \
-Dcom.sun.management.jmxremote.authenticate=false \
-Dcom.sun.management.jmxremote.ssl=false
```

Connect using:

```bash
jconsole localhost:9010
```

### **Programmatic Counterpart**

Expose a custom MBean:

```java
public interface MemoryStatsMBean {
    long getUsedHeap();
}
public class MemoryStats implements MemoryStatsMBean {
    public long getUsedHeap() {
        return ManagementFactory.getMemoryMXBean()
                .getHeapMemoryUsage().getUsed();
    }
}
public class Agent {
    public static void main(String[] args) throws Exception {
        ManagementFactory.getPlatformMBeanServer()
                .registerMBean(new MemoryStats(), new ObjectName("org.wm:type=MemoryStats"));
        Thread.sleep(Long.MAX_VALUE);
    }
}
```

You can query this via `jconsole → org.wm → MemoryStats → Attributes → UsedHeap`.

---

## **6. Leak Simulation & Diagnosis**

### **Goal**

Train recognition of memory leaks and heap dump analysis.

### **Java Program**

```java
import java.util.*;

public class LeakSim {
    static List<byte[]> store = new ArrayList<>();
    public static void main(String[] args) {
        while (true) {
            store.add(new byte[1024 * 100]); // 100 KB per iteration
            if (store.size() % 100 == 0)
                System.out.println("Stored: " + store.size() + " objects");
        }
    }
}
```

### **Procedure**

1. Run with `-Xmx256m`
2. Watch heap in VisualVM or `jstat -gc`
3. Capture dump at OOM:

   ```bash
   jmap -dump:live,format=b,file=/tmp/leak.hprof $(pgrep -f LeakSim)
   ```
4. Open `/tmp/leak.hprof` in **Eclipse MAT**, check “Dominators” — static list retains all arrays.

---

## **7. JMX Query Automation**

### **Goal**

Query JVM metrics remotely without a GUI.

### **CLI Procedure (jmxterm)**

```bash
java -jar jmxterm.jar -l localhost:9010
> domain java.lang
> bean java.lang:type=Memory
> get HeapMemoryUsage
```

### **Programmatic Script**

Use Jolokia (HTTP-JMX bridge):

```bash
curl http://localhost:8778/jolokia/read/java.lang:type=Memory/HeapMemoryUsage
```

Example response:

```json
{
  "used": 134217728,
  "max": 536870912
}
```

Integrate this into monitoring pipelines or Prometheus exporters.

---

## **8. Consolidation Exercise**

Build a small service (`/metrics`) that exposes:

* Heap usage (`MemoryMXBean`)
* GC collection count (`GarbageCollectorMXBean`)
* Thread count (`ThreadMXBean`)

Spring Boot makes this trivial via Micrometer, but this exercise forces manual interaction:

```java
@RestController
class MetricsController {
    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        var mem = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        var gcs = ManagementFactory.getGarbageCollectorMXBeans();
        long totalCollections = gcs.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();

        return Map.of(
            "heap.used", mem.getUsed(),
            "heap.max", mem.getMax(),
            "gc.count", totalCollections,
            "threads", ManagementFactory.getThreadMXBean().getThreadCount()
        );
    }
}
```

Now compare `/metrics` with what VisualVM reports — both derive from the same JMX data.

---

## **Key Learning Goals**

| Skill                 | Verifiable Indicator                             |
| --------------------- | ------------------------------------------------ |
| GC log interpretation | You can predict GC cause from log patterns       |
| jmap/jstack fluency   | You can isolate leaks and deadlocks              |
| JMX comprehension     | You can query and expose JVM MBeans              |
| Profiling mindset     | You correlate heap and CPU with runtime behavior |
