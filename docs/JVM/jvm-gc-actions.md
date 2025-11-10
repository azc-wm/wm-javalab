# JVM Garbage Collection Actions and Phases

## 1. Overview
Garbage Collection (GC) reclaims memory used by objects no longer reachable by any live references.

All GC algorithms share three fundamental actions:

| Action | Description | Typical Trigger |
|---------|--------------|-----------------|
| **Mark** | Identify which objects are still reachable. | Start of a GC cycle. |
| **Sweep** | Remove unreachable objects and free their memory. | After marking phase (non-moving collectors). |
| **Compact / Evacuate** | Move live objects to reduce fragmentation and release contiguous space. | When heap becomes fragmented or nearly full. |

---

## 2. The Need for Compaction
The JVM allocates objects in continuous memory blocks.  
When many objects are freed, gaps appear — *fragmentation*.  
Large allocations may fail even if total free space is sufficient but scattered.

Compaction resolves this by:
- Moving live objects together.
- Updating all references to their new addresses.
- Freeing a continuous region for new allocations.

Without compaction, long-lived applications can reach *allocation failure* despite having unused heap space.

---

## 3. Compaction in Different Collectors

| Collector | Compaction Method | Notes |
|------------|-------------------|-------|
| **Serial / Parallel GC** | Stop-the-world; moves all live objects; full heap compaction. | Simple but long pauses. |
| **CMS (deprecated)** | No compaction during normal cycles; occasional full compaction. | Low latency, more fragmentation. |
| **G1GC** | Evacuation-based compaction; moves live data region-by-region. | Default in modern JVMs. |
| **ZGC / Shenandoah** | Concurrent relocation with pointer coloring. | Near-zero pause collectors. |

---

## 4. G1GC Phases Explained

### 4.1 Young Collection
- Targets **Eden** region.
- Moves surviving objects to **Survivor** regions (evacuation).
- Cheap and frequent.
- Logged as:
```

Pause Young (Normal) (G1 Evacuation Pause)

```

### 4.2 Concurrent Mark Cycle
- Background phase determining which old-generation objects are still live.
- Steps:
1. **Concurrent Scan Root Regions**
2. **Concurrent Mark From Roots**
3. **Remark (stop-the-world)**
4. **Cleanup**
- After marking, G1 knows which regions are mostly garbage.

### 4.3 Mixed GC
- Cleans young + some old regions with low live ratios.
- Reduces heap usage gradually.
- Logged as `Pause Young (Mixed)`.

### 4.4 Full GC (Compaction)
- Triggered when normal G1 evacuation cannot find enough contiguous space.
- All threads paused (stop-the-world).
- Phases include:
1. **Mark live objects**
2. **Prepare compaction**
3. **Adjust pointers**
4. **Compact heap**
5. **Reset metadata**

Log example:
```

GC(25) Pause Full (G1 Compaction Pause) 1018M->1018M(1024M) 20.0ms

```
No reduction in used memory indicates all objects are live → likely OOM imminent.

---

## 5. Humongous Objects and Their Role

- Any object > ½ of a region size is stored directly in **old generation**.
- These bypass normal young GC evacuation.
- Can quickly consume contiguous regions.
- G1 triggers *full compaction* if new humongous allocation cannot fit.

Log example:
```

Pause Young (Concurrent Start) (G1 Humongous Allocation)
Humongous regions: 1005->1005

```

---

## 6. Visual Model

```

Before Compaction:        After Compaction:
|A| |B| |C| |D| |E|       |A|B|C|D|E| | | | |
^ gaps from freed objects  ^ contiguous free regions

```

- **Before:** fragmented heap; allocations may fail.
- **After:** live objects packed; contiguous space restored.

---

## 7. Summary Table

| Phase | Description | Effect |
|--------|--------------|--------|
| **Mark** | Identify live objects. | Metadata updated. |
| **Sweep** | Reclaim dead memory. | Frees scattered blocks. |
| **Compact/Evacuate** | Move live objects together. | Removes fragmentation. |
| **Concurrent Mark** | Background reachability analysis. | Guides later mixed GC. |
| **Full Compaction** | Emergency stop-the-world heap reorganization. | High pause cost. |

---

## 8. Diagnostic Keywords in Logs

| Keyword | Indicates |
|----------|------------|
| `Evacuate Collection Set` | Moving live objects during young GC. |
| `Concurrent Mark` | Background marking of old gen. |
| `Pause Full (G1 Compaction Pause)` | Stop-the-world full GC (terminal). |
| `Humongous Allocation` | Oversized object placement. |
| `Attempting maximal full compaction` | JVM at final reclaim attempt before OOM. |

---

## 9. Key Takeaways

- **Compaction** prevents fragmentation and allocation failures.
- **Evacuation** is G1’s incremental form of compaction.
- **Humongous regions** are a major cause of allocation failures in memory-pressure tests.
- When G1 performs **full compaction repeatedly with no effect**, the heap is entirely live — OOM is inevitable.
