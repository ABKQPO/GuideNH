plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

minecraft {
    extraRunJvmArguments.addAll("-Xmx8G", "-Xms8G", "-Dgtnhlib.dumpkeys=true")
}

tasks.withType<JavaCompile>().configureEach {
    options.annotationProcessorPath = configurations.annotationProcessor.get()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    relocate("org.yaml.snakeyaml", "com.hfstudio.guidenh.shadow.snakeyaml")
    relocate("io.methvin", "com.hfstudio.guidenh.shadow.methvin")
    relocate("org.apache.lucene", "com.hfstudio.guidenh.shadow.lucene")
    relocate("org.apache.commons", "com.hfstudio.guidenh.shadow.commons")
    relocate("com.sun.jna", "com.hfstudio.guidenh.shadow.jna")
    relocate("net.java.dev.jna", "com.hfstudio.guidenh.shadow.jna2")
}

val runConfigs = listOf(
    "runClient" to "run/client",
    "runClient17" to "run/client_new",
    "runClient21" to "run/client_new",
    "runClient25" to "run/client_new",
    "runServer" to "run/server",
    "runServer17" to "run/server_new",
    "runServer21" to "run/server_new",
    "runServer25" to "run/server_new"
)

runConfigs.forEach { (taskName, path) ->
    tasks.named<JavaExec>(taskName) {
        workingDir = file("${projectDir}/$path")
        doFirst {
            workingDir.mkdirs()
        }
    }
}
