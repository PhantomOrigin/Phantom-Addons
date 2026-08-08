plugins {
    // Shared across both Minecraft versions — see stonecutter.properties.toml's top-level loom_version.
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

// Three build variants from one codebase — see Edition.java.
//   ./gradlew build                    -> edition=full (default)
//   ./gradlew build -Pedition=noauto   -> full feature set, no in-mod auto-download
//   ./gradlew build -Pedition=standard -> no AutoGFS/CannonAutoClose/PearlRefill, no auto-download (Modrinth-safe)
data class Edition(val label: String, val suffix: String)

val editionKey = (project.findProperty("edition") as String? ?: "full").lowercase()
val editions = mapOf(
    "full" to Edition("FULL", "full"),
    "noauto" to Edition("FULL_NO_AUTO_UPDATE", "full-noautoupdate"),
    "standard" to Edition("STANDARD", ""),
)
val edition = editions[editionKey]
    ?: throw GradleException("Unknown edition '$editionKey' — expected one of: ${editions.keys}")

// UpdateInstaller.java holds the actual download/jar-swap code. Only the full edition
// compiles it in at all — the other two editions don't just have it disabled, they
// don't contain that code, full stop. UpdateChecker only ever reaches it via
// reflection, so its compilation isn't affected by this exclusion.
if (editionKey != "full") {
    sourceSets.main {
        java {
            exclude("com/phantomaddons/UpdateInstaller.java")
        }
    }
}

// World rendering hooks into two structurally unrelated mechanisms depending on version:
// 26.2+ uses Fabric API's LevelRenderEvents.COLLECT_SUBMITS (LevelRenderDispatch.java, registered
// explicitly from PhantomAddons.java), while pre-26.2 uses a plain @Inject mixin into
// LevelRenderer.renderLevel (mixin/WorldRendererMixin.java, self-registering via mixins.json).
// Both files live in the shared tree; only the one matching the active version compiles in.
val isLegacyRendering = sc.current.parsed < "26.2"

sourceSets.main {
    java {
        if (isLegacyRendering) {
            // Both are 26.2+-only helpers (SubmitNodeCollector dispatch + the RenderPipeline-based
            // always-on-top trick) — legacy rendering paths use plain GL11 depth toggling instead.
            exclude("com/phantomaddons/LevelRenderDispatch.java")
            exclude("com/phantomaddons/utils/AlwaysOnTopRenderTypes.java")
            exclude("com/phantomaddons/utils/WorldRenderCollector.java")
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
    // WorldRendererMixin only exists (and is only needed) on the legacy rendering path — see above.
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

// Builds all three edition jars, for both Minecraft versions, in one command (6 jars total).
// Each edition is a genuinely separate Gradle invocation (project properties like `edition`
// are only read once, at configuration time, so a single `build` run can't produce more than
// one) — this just shells out to the wrapper three times so you don't have to.
//
// Each invocation includes `clean` first: switching `-Pedition` between runs changes
// sourceSets.main.java.exclude (UpdateInstaller.java in/out), and Gradle's incremental
// Java compiler doesn't reliably invalidate previously-compiled classes when only the
// exclude set changes — without a clean, later editions in the sequence can fail or
// silently package stale classes from the previous edition's build. Since `clean` would
// otherwise also delete the previous edition's output, each edition's jars are copied out
// to build/libs/all-editions/ before the next edition's clean runs.
//
// This script is shared across every Stonecutter node (one per Minecraft version), so without
// a guard this task gets registered once per node — running it from root would then fire N
// concurrent copies, each shelling out its own clean/build cycle and clobbering the others'
// files mid-build. Only register it on one node; it already builds every version internally
// via the plain (unqualified) `clean`/`build` invocations below.
val rootDir = rootProject.projectDir
val collectedDir = rootProject.layout.buildDirectory.dir("libs/all-editions")

if (project.name == "26.2.x") tasks.register("buildAllEditions") {
    group = "build"
    description = "Builds the full, noauto, and standard edition jars (both MC versions) in one command."
    doLast {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        // Releases are collected under a folder per mod version (e.g. all-editions/1.6.24/) rather
        // than one flat directory, so past releases stay available across multiple runs of this task
        // instead of being wiped out the next time you bump mod.version and rebuild. Only the current
        // version's own folder is cleared/rebuilt each run.
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
            // clean and build run as two separate invocations, not `clean build` in one: combining
            // them lets compileJava see a task graph configured before clean ran, so it tries to
            // read Stonecutter-generated sources that clean just deleted before they're regenerated.
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
