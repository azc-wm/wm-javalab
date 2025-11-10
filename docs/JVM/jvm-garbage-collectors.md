# **Garbage Collection Deep Dive**

---

## **1. Core Model**

Garbage Collection (GC) automates memory reclamation.
Its job: remove unreachable objects while minimizing pause time and throughput loss.

### **The lifecycle of an object**

```
Allocation → Promotion → Tenuring → Reclamation
```

1. **New** objects are created in **Eden** (Young Gen).
2. After surviving a few collections, they move to **Old Gen**.
3. When unreachable, they are **collected** by the GC.
4. Metadata lives in **Metaspace** (class structures).

---

## **2. The Memory Regions**

| Area                        | Function                          | GC Focus                      |
| --------------------------- | --------------------------------- | ----------------------------- |
| **Eden Space**              | Allocation buffer for new objects | Collected often               |
| **Survivor Spaces (S0/S1)** | Ping-pong buffer for survivors    | Promotes after N cycles       |
| **Old Generation**          | Long-lived objects                | Full or concurrent collection |
| **Metaspace**               | Class metadata                    | Grows dynamically             |
| **Code Cache**              | Compiled JIT code                 | Managed separately            |

HotSpot divides memory like this:

```
[ Young Gen (Eden + S0 + S1) ]  → Minor GCs
[ Old Gen ]                     → Major / Mixed GCs
[ Metaspace ]                   → Class data
```

---

## **3. Collector Families**

Different collectors target different trade-offs between *throughput* and *pause latency.*

| Collector             | Model                         | Use case                          | Key flags              |
| --------------------- | ----------------------------- | --------------------------------- | ---------------------- |
| **Serial GC**         | Stop-the-world, single-thread | Small apps, minimal cores         | `-XX:+UseSerialGC`     |
| **Parallel GC**       | Multi-threaded stop-the-world | CPU-heavy batch jobs              | `-XX:+UseParallelGC`   |
| **G1 GC** *(default)* | Region-based concurrent       | Balanced latency/throughput       | `-XX:+UseG1GC`         |
| **ZGC**               | Concurrent mark-compact       | Low-latency, large heaps (GB–TB)  | `-XX:+UseZGC`          |
| **Shenandoah**        | Concurrent compaction         | Similar to ZGC                    | `-XX:+UseShenandoahGC` |
| **Epsilon**           | No collection                 | Benchmarking, GC-free experiments | `-XX:+UseEpsilonGC`    |

---

## **4. Key GC Metrics**

| Metric                      | Meaning                     | Tool / Source            |
| --------------------------- | --------------------------- | ------------------------ |
| **Allocation Rate**         | MB/sec of object creation   | `jstat -gc` or JFR       |
| **Promotion Rate**          | Objects promoted to Old Gen | `jstat -gc`              |
| **Pause Time**              | Stop-the-world duration     | GC log timestamps        |
| **Live Set**                | Heap used after GC          | GC log or JMX            |
| **Collection Count / Time** | Frequency and total GC time | `GarbageCollectorMXBean` |

**Throughput** = (1 − GC_Time / Total_Time) × 100%
**Latency** = maximum or 95th percentile GC pause.

---

## **5. Logging and Interpretation**

Enable GC logs (JDK 9+ unified logging syntax):

```bash
-Xlog:gc*,gc+heap=info:file=gc.log:time,uptime,level,tags
```

Example excerpt:

```
[2.345s][info][gc,start] GC(0) Pause Young (Normal) (G1 Evacuation Pause)
[2.347s][info][gc,heap  ] GC(0) Eden regions: 15->0(20)
[2.347s][info][gc,heap  ] GC(0) Survivor regions: 2->3(3)
[2.347s][info][gc       ] GC(0) Pause Young ... 2.3ms
```

Interpretation:

* **Pause type** identifies GC phase.
* **Eden/Survivor changes** show memory promotion.
* **Pause time** shows latency footprint.
* **Region stats** indicate heap distribution.

Tools like **GCViewer**, **GCEasy**, or **VisualVM** can visualize these logs.

---

## **6. Tuning Strategy**

| Goal                 | Adjust                            | Example                                 |
| -------------------- | --------------------------------- | --------------------------------------- |
| Reduce pause time    | Target pause duration             | `-XX:MaxGCPauseMillis=200`              |
| Prevent premature GC | Increase young gen size           | `-XX:NewRatio=2`                        |
| Cap heap growth      | Limit total heap                  | `-Xmx2g`                                |
| Optimize promotion   | Adjust occupancy                  | `-XX:InitiatingHeapOccupancyPercent=45` |
| Lower fragmentation  | Use compacting collector (G1/ZGC) | `-XX:+UseG1GC`                          |

🧩 **Rule:** never tune before measuring.
Start with defaults, observe metrics, then isolate single variables per iteration.

---

## **7. Observing GC at Runtime**

### **CLI**

```bash
jstat -gc $(pgrep -f MyApp) 1000
```

Output columns:

```
S0C S1C EC EU OC OU MC MU YGC YGCT FGC FGCT GCT
```

| Symbol    | Meaning               |
| --------- | --------------------- |
| EC/EU     | Eden capacity/used    |
| OC/OU     | Old gen capacity/used |
| YGC/FGC   | GC counts             |
| YGCT/FGCT | Total GC times        |

---

### **Programmatic**

```java
for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
    System.out.printf("%s: %d collections, %d ms%n",
        gc.getName(),
        gc.getCollectionCount(),
        gc.getCollectionTime());
}
```

For live monitoring, expose these metrics via a REST endpoint or Prometheus.

---

## **8. GC Comparison Summary**

| Aspect     | Serial         | Parallel       | G1         | ZGC        | Shenandoah |
| ---------- | -------------- | -------------- | ---------- | ---------- | ---------- |
| Threads    | 1              | Many           | Many       | Many       | Many       |
| Compaction | Stop-the-world | Stop-the-world | Concurrent | Concurrent | Concurrent |
| Latency    | High           | Medium         | Low        | Very low   | Very low   |
| Throughput | Medium         | High           | Medium     | Medium     | Medium     |
| Heap size  | Small          | Medium         | Large      | Very large | Very large |
| Introduced | Java 1.3       | Java 1.4       | Java 7/9+  | Java 11+   | Java 12+   |

---

## **9. Visualization Tools**

| Tool                           | Function                  |
| ------------------------------ | ------------------------- |
| **VisualVM**                   | Live heap, GC, CPU charts |
| **GCViewer**                   | Parse GC logs             |
| **JConsole**                   | View MBeans and GC data   |
| **Eclipse MAT**                | Heap dump analysis        |
| **JMC (Java Mission Control)** | JFR-based deep profiling  |

For live services, integrate with:

* **Micrometer + Prometheus** → export `jvm_gc_pause_seconds`
* **Grafana** → visualize latency and memory trends.

---

## **10. Minimal Java Sample for GC Experiments**

```java
public class GCLoad {
    public static void main(String[] args) {
        var store = new java.util.ArrayList<byte[]>();
        while (true) {
            store.add(new byte[1024 * 100]); // allocate 100KB
            if (store.size() % 100 == 0) {
                System.out.println("Objects: " + store.size());
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            }
        }
    }
}
```

Run variants:

```bash
java -Xmx256m -XX:+UseG1GC -jar GCLoad.jar
java -Xmx256m -XX:+UseZGC -jar GCLoad.jar
```

Observe differences in pause frequency, memory usage, and throughput.

---

## **11. Key Takeaways**

* Garbage Collection = *automatic memory management + non-deterministic timing.*
* Choose collector by **latency profile**, not by trend.
* GC logs + `jstat` are primary evidence, not assumptions.
* Measure → interpret → tune one variable → measure again.
* Modern collectors (G1, ZGC, Shenandoah) make manual tuning minimal, but understanding phases helps profiling accuracy.

