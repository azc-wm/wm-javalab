# JVM Garbage Collection and Out-of-Memory Diagnosis

## 1. Overview
The JVM heap is divided into:
- **Eden**: where new objects are allocated.
- **Survivor**: short-lived objects that survive a few GCs.
- **Old generation**: long-lived objects promoted after multiple collections.
- **Humongous regions (G1 only)**: large objects (>½ of a region) stored directly in old generation.

The **G1 Garbage Collector** (default in Java ≥9) manages the heap in fixed-size regions and collects them concurrently.

---

## 2. Reading GC Logs

### 2.1 Event Types
| Keyword | Meaning |
|----------|----------|
| `Pause Young (Normal)` | Minor GC; cleans Eden, promotes survivors. |
| `Pause Young (Concurrent Start)` | Minor GC triggering a concurrent mark cycle. |
| `Concurrent Mark Cycle` | Background scan of live objects in old regions. |
| `Pause Full (G1 Compaction Pause)` | Stop-the-world full GC; last resort compaction. |
| `Humongous Allocation` | A large object requested more than half a region; may trigger full GC. |

---

### 2.2 Typical Log Line
```

[2025-11-08T14:25:47.594+0100][0.648s][info][gc,heap]
GC(1) Pause Young (Normal) (G1 Evacuation Pause) 37M->7M(252M) 5.6ms

```
**Interpretation**  
- **37M→7M**: used heap before/after GC.  
- **(252M)**: total heap capacity (`-Xmx`).  
- **5.6ms**: pause duration.  
- Indicates healthy allocation and reclamation.

---

## 3. Recognizing Heap Saturation

As heap pressure grows:

| Symptom | Meaning |
|----------|----------|
| Increasing frequency of `Pause Young` | Eden fills quickly; short-lived allocation churn. |
| Rising **Old regions** count | Long-lived data retained; potential leak. |
| Appearing **Humongous regions** | Large arrays or buffers; risky in small heaps. |
| Full GCs (`Pause Full`) with minimal reduction | GC cannot reclaim memory; live set equals heap. |
| Repeated **`Attempting maximal full compaction clearing soft references`** | JVM has entered terminal GC state before OOM. |

---

## 4. Example: JVM Death Trace

Final GC sequence before failure:
```

GC(25) Pause Full (G1 Compaction Pause) 1018M->1018M(1024M)
GC(26) Pause Full (G1 Compaction Pause) 1018M->1018M(1024M)
GC(27) Pause Young (G1 Humongous Allocation) 1019M->1018M(1024M)
...
java.lang.OutOfMemoryError: Java heap space

```

**Diagnosis**
- Heap nearly full (`1018M/1024M`).
- Humongous regions constant (`1005->1005`): large arrays retained.
- GC compaction ineffective → all objects are live.
- JVM terminates with `OutOfMemoryError`.

---

## 5. Interpreting the Cause

### 5.1 Live Data Too Large
Application legitimately needs more heap.  
→ Increase `-Xmx`, or optimize data size.

### 5.2 Memory Leak
Objects referenced unintentionally (e.g., static collections, caches).  
→ Use a heap dump (`-XX:+HeapDumpOnOutOfMemoryError`) and inspect in **VisualVM** or **Eclipse MAT**.

### 5.3 Humongous Object Fragmentation
Large contiguous allocations fail even if free space exists in fragments.  
→ Reduce allocation size or switch to larger region size (`-XX:G1HeapRegionSize`).

---

## 6. Useful Diagnostic Commands

| Command | Description |
|----------|--------------|
| `jcmd <pid> GC.heap_info` | Summary of heap layout and usage. |
| `jcmd <pid> GC.class_histogram` | Live class histogram; shows retained byte[] or cache objects. |
| `jstat -gc <pid> 1s` | Live GC stats every second (young, old, meta). |
| `pgrep -a java` | View running JVMs and their launch parameters. |
| `jmap -histo <pid> | head` | Quick class histogram from CLI. |

---

## 7. Preventive Practices

- **Set limits explicitly**: `-Xms512m -Xmx512m`.
- **Enable GC logging**:  
```

-Xlog:gc*,gc+heap*:file=gc.log:time,uptime,level,tags

```
- **Always enable heap dump on OOM**:  
```

-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=tmp/heapdump.hprof

```
- **Avoid uncontrolled collections** (e.g., `List<byte[]>` holding memory).
- **Monitor humongous region usage** in G1GC via metrics.

---

## 8. Key Takeaway

- GC logs are chronological stories of allocation pressure.  
- `Humongous regions` + `Full compaction with no reclaim` = imminent OOM.  
- The JVM’s final act is a **“maximal full compaction”** followed by **`OutOfMemoryError`**.

