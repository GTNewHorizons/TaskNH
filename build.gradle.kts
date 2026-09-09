plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

// Angelica is a client renderer. Its mixins crash the dedicated server while vanilla blocks are
// registered, so it is dropped from the server runs and kept in the client ones.
tasks.withType<JavaExec>()
    .matching { it.name.startsWith("runServer") }
    .configureEach {
        classpath = classpath.filter { !it.name.startsWith("Angelica") }
    }
