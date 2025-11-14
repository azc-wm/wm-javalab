plugins {
    id("java")
    id("application")
}

group = "org.wm"
version = "1.0-SNAPSHOT"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("org.wm.Main")
}

// JVM GC configuration for profiling and performance tuning.
// We're using G1GC here because it's the default for Java 9+ and provides a strong balance between throughput and low-pause behavior.
//
// ────────────────────────────────────────────────────────────────────────────────
// 👇 GC Options Overview:
//
// -XX:+UseG1GC
//   ➤ Region-based, low-pause GC for heaps from ~2GB to tens of GBs.
//   ➤ Good default for most server-side workloads.
//   ➤ [Docs: https://docs.oracle.com/en/java/javase/17/gctuning/garbage-first-garbage-collector.html]
//
// -XX:+UseZGC
//   ➤ Ultra-low pause GC, ideal for latency-critical systems (target pauses <10ms).
//   ➤ Works well with large heaps (multi-GB to TB scale).
//   ➤ [Docs: https://docs.oracle.com/en/java/javase/17/gctuning/zgc.html]
//
// -XX:+UseShenandoahGC
//   ➤ Similar to ZGC, very low-pause, concurrent compaction.
//   ➤ Great for apps with responsiveness as a top priority.
//   ➤ [Docs: https://wiki.openjdk.org/display/shenandoah/Main]
//
// -XX:+UseParallelGC
//   ➤ High-throughput GC, useful for batch jobs or compute-heavy workloads.
//   ➤ Not pause-sensitive; longer stop-the-world phases.
//   ➤ [Docs: https://docs.oracle.com/en/java/javase/17/gctuning/parallel-garbage-collector.html]
//
// -XX:+UseSerialGC
//   ➤ Single-threaded GC, only suitable for small CLI tools or constrained environments (<512MB).
//   ➤ Not recommended for long-lived or interactive services.
//   ➤ [Docs: https://docs.oracle.com/en/java/javase/17/gctuning/serial-garbage-collector.html]
//
// 🔍 To analyze GC logs, use:
//   → -Xlog:gc*,gc+heap*:./tmp/gc.log:time,uptime
//   → Visual tools: GCViewer, GCEasy.io, or JVM Dashboard in IntelliJ Ultimate
//
// 🧠 Tip: For latency-sensitive apps, experiment with ZGC or Shenandoah in Java 17+,
//         and adjust -XX:MaxGCPauseMillis to target specific pause goals.
//


//// The 'run' task is of type JavaExec, not just a generic Task.
//// To access JavaExec-specific properties like `jvmArgs`, we must explicitly cast it using tasks.named<JavaExec>("run").
//// If we don't, Kotlin DSL treats it as a generic Task, which doesn't have `jvmArgs` or `mainClass`.
//// This is a common DSL pitfall: always specify the task type when configuring task-specific fields.

// Configure the 'run' task, which comes from the 'application' plugin and is of type JavaExec
tasks.named<JavaExec>("run") {
    mainClass.set("org.wm.scripting.Main")
    // Set JVM arguments passed to the Java process
    jvmArgs = listOf(
        "-Xmx2g", // Allocate a maximum of 2 GB heap memory
        "-XX:+UseG1GC", // Use the G1 Garbage Collector for balanced throughput and pause times
        "-Xlog:gc*,gc+heap*:./tmp/gc.log:time,uptime"
        // Enable detailed GC and heap logging, and write it to ./tmp/gc.log with timestamps and uptime
    )
}

tasks.test {
    useJUnitPlatform()
    // Useful to see system.out messages
    testLogging {
        showStandardStreams = true
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
