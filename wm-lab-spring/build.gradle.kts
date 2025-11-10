
plugins {
    id("java")
    alias(libs.plugins.spring.boot)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {

    // for the sake of doing tests in dumps and data extraction and being
    // able to drop inside the project's location
    workingDir(projectDir)

    mainClass.set("org.wm.springlab.app.Application")

    jvmArgs = listOf(
        "-Xmx1g",
        "-XX:+UseG1GC",
        "-Xlog:gc*,gc+heap*,safepoint,class+load,class+unload:file=${project.rootDir}/tmp/gc.log:time,uptime,level,tags",
//      "-Xlog:gc*,gc+heap*:file=${project.rootDir}/tmp/gc.log:time,uptime" -> minimalistic logginc for gc
    )
}

dependencies {
    // Core web + actuator
    implementation(platform(libs.spring.boot.bom))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)

    // Metrics and observability
    implementation(libs.micrometer.core)
    implementation(libs.micrometer.prometheus)

    // Logging
    implementation(libs.logback.classic)

    // Testing stack
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.junit.jupiter)
}