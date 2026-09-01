plugins {
    id("net.fabricmc.fabric-loom") version "1.17.18"
    id("maven-publish")
}

version = property("mod.version") as String
group = property("mod.group") as String

base {
    archivesName = property("mod.archives_base_name") as String
}

repositories {
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    implementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
    compileOnly(fileTree(rootProject.file("libs")) { include("*.jar") })
}

loom {
    mods {
        create("phantomaddons") {
            sourceSet(sourceSets.main.get())
        }
    }
}

data class Edition(val label: String, val suffix: String)

val editionKey = (project.findProperty("edition") as String? ?: "standard").lowercase()
val editions = mapOf(
    "full" to Edition("FULL", "full"),
    "noauto" to Edition("FULL_NO_AUTO_UPDATE", "full-noautoupdate"),
    "standard" to Edition("STANDARD", ""),
)
val edition = editions[editionKey]
    ?: throw GradleException("Unknown edition '$editionKey' — expected one of: ${editions.keys}")

if (editionKey != "full") {
    sourceSets.main {
        java {
            exclude("com/phantomaddons/UpdateInstaller.java")
        }
    }
}

val isLegacyRendering = sc.current.parsed < "26.2"

sourceSets.main {
    java {
        if (isLegacyRendering) {
            exclude("com/phantomaddons/LevelRenderDispatch.java")
            exclude("com/phantomaddons/utils/AlwaysOnTopRenderTypes.java")
            exclude("com/phantomaddons/utils/WorldRenderCollector.java")
            exclude("com/phantomaddons/utils/ImmediateDraw.java")
        } else {
            exclude("com/phantomaddons/mixin/WorldRendererMixin.java")
        }
    }
}

val modVersion = version.toString()
val editionLabel = edition.label
val minecraftCompat = property("mod.mc_compat") as String

tasks.processResources {
    inputs.property("version", modVersion)
    inputs.property("edition", editionLabel)
    inputs.property("legacyRendering", isLegacyRendering)
    inputs.property("minecraftCompat", minecraftCompat)
    filesMatching("fabric.mod.json") {
        expand("version" to modVersion, "minecraft_compat" to minecraftCompat)
    }
    filesMatching("edition.properties") {
        expand("edition" to editionLabel)
    }
    if (isLegacyRendering) {
        filesMatching("phantomaddons.mixins.json") {
            filter { line -> line.replace("\"FontDrawMixin\",", "\"FontDrawMixin\",\n        \"WorldRendererMixin\",") }
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

val archivesBaseName = base.archivesName.get()

tasks.jar {
    archiveClassifier.set(edition.suffix)
    from(rootProject.file("LICENSE")) {
        rename { "${it}_$archivesBaseName" }
    }
}

val rootDir = rootProject.projectDir
val collectedDir = rootProject.layout.buildDirectory.dir("libs/all-editions")

if (project.name == "26.2.x") tasks.register("buildAllEditions") {
    group = "build"
    description = "Builds the full, noauto, and standard edition jars (both MC versions) in one command."
    doLast {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val outDir = File(collectedDir.get().asFile, modVersion)
        outDir.deleteRecursively()
        outDir.mkdirs()

        val wrapper = File(rootDir, if (isWindows) "gradlew.bat" else "gradlew").absolutePath
        fun runGradle(vararg args: String) {
            val command = if (isWindows) listOf("cmd", "/c", wrapper) + args else listOf(wrapper) + args
            val process = ProcessBuilder(command)
                .directory(rootDir)
                .redirectErrorStream(true)
                .start()
            process.inputStream.bufferedReader().forEachLine { println(it) }
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw GradleException("'gradlew ${args.joinToString(" ")}' failed with exit code $exitCode")
            }
        }

        listOf("full", "noauto", "standard").forEach { ed ->
            runGradle("clean")
            runGradle("build", "-Pedition=$ed")

            File(rootDir, "versions").listFiles { f -> f.isDirectory }?.forEach { versionDir ->
                val versionOutDir = File(outDir, versionDir.name)
                versionOutDir.mkdirs()
                File(versionDir, "build/libs").listFiles { f -> f.extension == "jar" }?.forEach { jar ->
                    jar.copyTo(File(versionOutDir, "${versionDir.name}-${jar.name}"), overwrite = true)
                }
            }
        }

        println("All editions collected in: ${outDir.absolutePath}")
    }
}
