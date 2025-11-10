package org.wm.springlab.app.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * MemoryController provides REST endpoints for generating and observing heap activity
 * within a running Spring Boot JVM process.
 *
 * <p>This controller is designed for experimentation and diagnostics, allowing developers
 * to trigger manual memory allocations, clear retained objects, and observe garbage
 * collection behavior via tools such as JConsole, VisualVM, or jmap/jstat.</p>
 *
 * <p>Intended usage:
 * <ul>
 *   <li>Call <b>/allocate</b> repeatedly to simulate heap pressure.</li>
 *   <li>Call <b>/clear</b> to release references and request garbage collection.</li>
 *   <li>Call <b>/status</b> to query the number of currently retained objects.</li>
 * </ul>
 * </p>
 *
 * <p>This controller is useful for:
 * <ul>
 *   <li>Testing JVM garbage collector configurations (e.g., G1, ZGC, Shenandoah).</li>
 *   <li>Practicing heap dump and JMX monitoring commands.</li>
 *   <li>Profiling memory allocation behavior under load.</li>
 * </ul>
 * </p>
 *
 * <p><b>Note:</b> This class is intended strictly for laboratory or educational use.
 * It should not be deployed in production environments.</p>
 *
 * @author Axl
 * @version 1.0
 * @since 2025-11
 */
@RestController()
@RequestMapping("/memory")
public class MemoryController {

    private final List<byte[]> memoryPressure = new ArrayList<>();

    @GetMapping("/allocate")
    public String allocate(
            @RequestParam(required = false,defaultValue = "10") int size
    ) {
        // Allocate 10 MB chunks until you run out of heap
        byte[] chunk = new byte[size * 1024 * 1024];
        memoryPressure.add(chunk);
        return "Allocated " + (memoryPressure.stream().mapToInt(Array::getLength).sum() / 1024 / 1024) + " MB so far";
    }

    @GetMapping("/clear")
    public String clear() {
        memoryPressure.clear();
        System.gc();
        return "Cleared and requested GC";
    }

    @GetMapping("/status")
    public String status() {
        return "Objects retained: " + memoryPressure.size();
    }
}