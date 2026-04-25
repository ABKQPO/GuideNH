import java.io.File

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

tasks.named<Jar>("sourcesJar") {
    dependsOn("packageGuideTutorialResourcePack")
}

val tutorialGuideSourceDir = layout.projectDirectory.dir("wiki/resourcepack")
val tutorialGuidePackOutputDir = layout.projectDirectory.dir("src/main/resources/assets/guidenh/resourcepacks")

val packageGuideTutorialResourcePack = tasks.register<Zip>("packageGuideTutorialResourcePack") {
    group = "guidenh"
    description = "Packages the built-in tutorial guide as a standard Minecraft resource pack zip."

    archiveFileName.set("guidenh_tutorial_guide_resource_pack.zip")
    destinationDirectory.set(tutorialGuidePackOutputDir)

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    val packMetaFile = tutorialGuideSourceDir.file("pack.mcmeta")
    val packIconFile = tutorialGuideSourceDir.file("pack.png")
    val guideRootDir = tutorialGuideSourceDir.dir("assets/guidenh/guidenh")

    doFirst {
        val missing = mutableListOf<File>()
        if (!packMetaFile.asFile.isFile) {
            missing.add(packMetaFile.asFile)
        }
        if (!packIconFile.asFile.isFile) {
            missing.add(packIconFile.asFile)
        }

        check(missing.isEmpty()) {
            "Guide runtime resource pack is incomplete. Missing: " + missing.joinToString()
        }

        check(guideRootDir.asFile.walkTopDown().any { it.isFile }) {
            "Guide runtime resource pack is empty: ${guideRootDir.asFile}"
        }
    }

    from(tutorialGuideSourceDir)
}

tasks.named("processResources").configure {
    dependsOn(packageGuideTutorialResourcePack)
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
