# **GC Log Interpretation Companion**

*(Appendix to Garbage Collection Deep Dive)*

This guide teaches how to read GC logs from the three main collectors: **G1**, **Parallel**, and **ZGC**.
You’ll learn how to map each log line to collector activity, memory movement, and pause impact.

---

## **1. Enabling GC Logging**

Modern JDK (9+):

```bash
-Xlog:gc*,gc+heap=debug:file=gc.log:time,uptime,level,tags
```

Key log parts:

```
[timestamp][level][tag] message
```

| Example     | Description                |
| ----------- | -------------------------- |
| `[2.345s]`  | Time since JVM start       |
| `[info]`    | Verbosity level            |
| `[gc,heap]` | Subsystem producing output |
| `message`   | Event details              |

Common tags: `gc`, `gc+heap`, `gc+phases`, `gc+ref`, `gc+age`, `gc+stats`.

---

## **2. G1 GC (Default Collector)**

Example:

```
[2.123s][info][gc,start    ] GC(0) Pause Young (Normal) (G1 Evacuation Pause)
[2.124s][info][gc,heap     ] GC(0) Eden regions: 15->0(20)
[2.124s][info][gc,heap     ] GC(0) Survivor regions: 2->3(3)
[2.124s][info][gc,heap     ] GC(0) Old regions: 3->5
[2.124s][info][gc,metaspace] Metaspace: 24M->24M(105M)
[2.125s][info][gc          ] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 2.123ms
```

### **How to Read**

| Field                       | Meaning                                   |
| --------------------------- | ----------------------------------------- |
| `Pause Young (Normal)`      | Minor GC in the young generation          |
| `Eden regions: 15->0(20)`   | 15 regions cleared, 20 total capacity     |
| `Survivor regions: 2->3(3)` | Surviving objects copied to 3 regions     |
| `Old regions: 3->5`         | Two promoted (survivors moved to old gen) |
| `Metaspace: 24M->24M(105M)` | Class metadata, stable usage              |
| `2.123ms`                   | Stop-the-world pause time                 |

🧩 *If Old regions steadily increase → promotion pressure → consider larger heap or higher pause target.*

---

## **3. Parallel GC**

Example:

```
[4.310s][info][gc,start ] GC(2) Pause Full (System.gc())
[4.311s][info][gc,phases] Phase 1: Mark live objects
[4.315s][info][gc,phases] Phase 2: Compact heap
[4.316s][info][gc        ] GC(2) Pause Full (System.gc()) 6.12ms
```

### **How to Read**

* Entire heap collected — both young and old.
* `Compact heap` → objects moved to remove fragmentation.
* Pauses are **stop-the-world**; no concurrent work.

🧩 *Good for batch workloads, not interactive apps.*

---

## **4. ZGC (Concurrent Low-Latency)**

Example:

```
[0.250s][info][gc,start   ] GC(0) Pause Mark Start
[0.252s][info][gc         ] GC(0) Concurrent Mark 2.15ms
[0.254s][info][gc,load    ] GC(0) Load: 2.2% marking, 0.3% relocation
[0.255s][info][gc         ] GC(0) Pause Relocate Start 0.36ms
[0.256s][info][gc         ] GC(0) Concurrent Relocate 3.82ms
```

### **How to Read**

| Event                  | Description                       |
| ---------------------- | --------------------------------- |
| `Pause Mark Start`     | Stop-the-world snapshot for roots |
| `Concurrent Mark`      | Trace live objects while app runs |
| `Relocate`             | Compact memory concurrently       |
| `Pause Relocate Start` | Brief synchronization pause       |

🧠 ZGC aims for sub-millisecond pauses regardless of heap size.

---

## **5. Common Metrics**

| Symbol               | Interpretation                          |
| -------------------- | --------------------------------------- |
| `Pause Young`        | Minor collection of new objects         |
| `Pause Mixed`        | Both young + old regions                |
| `Full GC`            | Whole heap compaction                   |
| `Concurrent Mark`    | Background live-object tracing          |
| `Eden/Old regions`   | Memory composition before and after GC  |
| `Metaspace`          | Class metadata, separate from heap      |
| `Evacuation Failure` | Object couldn’t move (allocation stall) |

---

## **6. Quantitative Reading**

To quantify GC efficiency:

| Metric              | Formula                   | Ideal                       |
| ------------------- | ------------------------- | --------------------------- |
| **Promotion rate**  | Δ Old Gen used / Δ time   | Stable, not rising too fast |
| **Pause ratio**     | GC_pause_time / wall_time | < 5–10%                     |
| **Live set**        | Heap usage after GC       | Flat line in steady state   |
| **Allocation rate** | Δ Eden usage / Δ time     | Predictable per load unit   |

These can be derived from GC logs or `jstat -gc`.

---

## **7. Tools to Visualize Logs**

| Tool                      | Description                                          |
| ------------------------- | ---------------------------------------------------- |
| **GCViewer**              | Parse GC log → graphs throughput, pause, heap growth |
| **GCEasy.io**             | Web-based parser with summaries                      |
| **VisualVM**              | Live GC charts (JMX + MBeans)                        |
| **JMC (Mission Control)** | Advanced correlation via JFR                         |

To analyze a log:

```bash
java -jar GCViewer.jar gc.log
```

---

## **8. Programmatic Correlation**

Example: correlate GC counts and time with `GarbageCollectorMXBean`.

```java
import java.lang.management.*;

public class GCMonitor {
    public static void main(String[] args) throws InterruptedException {
        while (true) {
            for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
                System.out.printf("%s → count=%d, time=%dms%n",
                    gc.getName(), gc.getCollectionCount(), gc.getCollectionTime());
            }
            Thread.sleep(3000);
        }
    }
}
```

Cross-check output with your GC log to verify consistency between telemetry and raw logs.

---

## **9. Quick Reference Cheat Table**

| Symptom            | Likely Cause              | Mitigation                  |
| ------------------ | ------------------------- | --------------------------- |
| Frequent young GCs | High object churn         | Reuse buffers, pool objects |
| Rising Old Gen     | Retained references       | Fix leaks or increase heap  |
| Long pauses        | Fragmentation or full GCs | Switch to G1/ZGC            |
| GC time >10%       | Over-tuned pause target   | Relax `MaxGCPauseMillis`    |
| Metaspace growth   | Classloader leak          | Check dynamic loading paths |

---

## **10. Cross-Verification Pipeline**

To ensure accurate understanding:

1. **Run GC with `-Xlog` → analyze with GCViewer**
2. **Correlate `GarbageCollectorMXBean` metrics → JMX dashboard**
3. **Verify memory stability with `jstat -gc`**
4. **Compare pause times with application response times**

This loop forms the minimal viable observability workflow for JVM memory behavior.
