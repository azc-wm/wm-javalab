rootProject.name = "wm_javalab"
include("wm-lab-scripting", "wm-lab-spring")


dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }

    // ─────────────────────────────────────────────────────────────
    // 🧭 Version Catalog Explanation
    //
    // Gradle automatically loads a version catalog named `libs`
    // from `gradle/libs.versions.toml` if that file exists.
    // You don’t need to define it manually with:
    //    create("libs") { from(files("gradle/libs.versions.toml")) }
    // Doing so would call `from()` twice for the same catalog
    // and trigger the error:
    //    "In version catalog libs, you can only call the 'from' method a single time."
    //
    // ✅ If using the default location (`gradle/libs.versions.toml`):
    //      → Remove manual catalog creation. Gradle finds it automatically.
    //
    // ✅ If using a custom path or extra catalogs:
    //      versionCatalogs {
    //          create("coreLibs") {
    //              from(files("config/core-libs.versions.toml"))
    //          }
    //      }
    //
    // This way, subprojects automatically inherit the `libs` catalog
    // and can use dependencies like:
    //
    //      implementation(libs.spring.boot.starter.web)
    //
    // ─────────────────────────────────────────────────────────────

    //versionCatalogs {
    //    create("libs") {
    //       from(files("gradle/libs.versions.toml"))
    //    }
    //}
}
