import java.io.File
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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

configurations.matching { it.name.startsWith("test", ignoreCase = true) }.configureEach {
    exclude(group = "com.github.GTNewHorizons", module = "ForgeMultipart")
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

tasks.named<ShadowJar>("shadowJar") {
    relocate("org.yaml.snakeyaml", "com.hfstudio.guidenh.shadow.snakeyaml")
    relocate("io.methvin", "com.hfstudio.guidenh.shadow.methvin")
    relocate("org.apache.lucene", "com.hfstudio.guidenh.shadow.lucene")
    relocate("org.apache.commons", "com.hfstudio.guidenh.shadow.commons")
    relocate("com.sun.jna", "com.hfstudio.guidenh.shadow.jna")
    relocate("net.java.dev.jna", "com.hfstudio.guidenh.shadow.jna2")
    filesMatching(
        listOf(
            "META-INF/services/javax.imageio.spi.ImageReaderSpi",
            "META-INF/services/javax.imageio.spi.ImageWriterSpi")) {
        filter { line: String ->
            line.replace(
                "com.luciad.imageio.webp.",
                "com.hfstudio.guidenh.shadow.com.luciad.imageio.webp.")
        }
    }
    minimize {
        exclude(dependency("org.sejda.imageio:webp-imageio:.*"))
    }
    from({
        project.configurations.getByName("shadowImplementation")
            .resolvedConfiguration
            .resolvedArtifacts
            .filter { it.moduleVersion.id.group == "org.sejda.imageio" && it.name == "webp-imageio" }
            .map { zipTree(it.file) }
    })
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

val guidePreviewSourceDir = tutorialGuideSourceDir.dir("assets/guidenh/guidenh").asFile.absolutePath
val guideRunConfigs = listOf(
    Triple("runGuide", "runClient", "run/client"),
    Triple("runGuide17", "runClient17", "run/client_new"),
    Triple("runGuide21", "runClient21", "run/client_new"),
    Triple("runGuide25", "runClient25", "run/client_new"))

guideRunConfigs.forEach { (taskName, baseTaskName, path) ->
    val baseTask = tasks.named<JavaExec>(baseTaskName).get()
    tasks.register<JavaExec>(taskName) {
        group = baseTask.group
        description = "Runs GuideNH live preview based on $baseTaskName."
        dependsOn(baseTask.taskDependencies.getDependencies(baseTask))
        standardInput = System.`in`
        workingDir = file("${projectDir}/$path")
        classpath = baseTask.classpath
        javaLauncher.set(baseTask.javaLauncher)
        baseTask.mainClass.orNull?.let { mainClass.set(it) }
        setArgs(baseTask.args ?: emptyList())
        setJvmArgs(baseTask.jvmArgs ?: emptyList())
        environment(baseTask.environment)
        systemProperties.putAll(baseTask.systemProperties)
        systemProperty("guideme.guidenh.guidenh.sources", guidePreviewSourceDir)
        systemProperty("guideme.showOnStartup", "guidenh:guidenh!index.md")
        doFirst {
            workingDir.mkdirs()
        }
    }
}
