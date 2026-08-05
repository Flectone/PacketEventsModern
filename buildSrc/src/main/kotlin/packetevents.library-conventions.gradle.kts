import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import groovy.util.Node
import kotlin.math.sign

plugins {
    `java-library`
    signing
    id("com.vanniktech.maven.publish")
}

group = rootProject.group
version = rootProject.version
description = rootProject.description

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/groups/public/")
}

val isShadow = project.pluginManager.hasPlugin("com.gradleup.shadow")

dependencies {
    compileOnly("org.jetbrains:annotations:23.0.0")
}

java {
    disableAutoTargetJvm()
}

tasks {
    withType<JavaCompile> {
        options.compilerArgs.add("-parameters")
        options.compilerArgs.add("-g")

        sequenceOf("unchecked", "deprecation", "removal")
            .forEach { options.compilerArgs.add("-Xlint:$it") }

        options.encoding = Charsets.UTF_8.name()
        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release = 8
    }

    processResources {
        inputs.property("version", project.version)
        filesMatching(listOf("plugin.yml", "bungee.yml", "velocity-plugin.json", "fabric.mod.json", "META-INF/neoforge.mods.toml")) {
            expand("version" to project.version)
        }
    }

    jar {
        if (isShadow) {
            archiveClassifier = "default"
        } else {
            destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
        }
    }

    val writeVersionFile by tasks.registering {
        val outFile = layout.buildDirectory.file("generated/${rootProject.name}_${project.name}_version.txt")
        outputs.file(outFile)

        val projectVersion = project.version.toString()

        doLast {
            outFile.map { it.asFile }.get().apply {
                parentFile.mkdirs()
                writeText(projectVersion)
            }
        }
    }

    // write version file to each jar; this solves our issue of modrinth not accepting
    // uploads of the same file twice, caused by the sources jar of some modules not changing for some versions
    withType<Jar> {
        dependsOn(writeVersionFile)
        metaInf {
            from(writeVersionFile)
        }
    }

    tasks.findByName("javadoc")?.enabled = false
    tasks.findByName("javadocJar")?.enabled = false

    defaultTasks("build")
}

val publishVersion: String = rootProject.ext["versionNoHash"] as String

mavenPublishing {
    publishToMavenCentral(true)
    signAllPublications()

    coordinates("net.flectone", "packeteventsmodern-${project.name}", publishVersion)

    pom {
        name.set("${rootProject.name}-${project.name}")
        description.set(rootProject.description.toString())
        url.set("https://github.com/retrooper/packetevents")

        licenses {
            license {
                name.set("GPL-3.0")
                url.set("https://www.gnu.org/licenses/gpl-3.0.html")
            }
        }

        developers {
            developer {
                id.set("retrooper")
                name.set("Retrooper")
                email.set("retrooperdev@gmail.com")
            }
        }

        scm {
            connection.set("scm:git:https://github.com/retrooper/packetevents.git")
            developerConnection.set("scm:git:https://github.com/retrooper/packetevents.git")
            url.set("https://github.com/retrooper/packetevents/tree/2.0")
        }
    }
}

afterEvaluate {
    tasks.findByName("javadoc")?.enabled = false
    tasks.findByName("javadocJar")?.enabled = false
}

// So that SNAPSHOT is always the latest SNAPSHOT
configurations.all {
    resolutionStrategy.cacheDynamicVersionsFor(0, TimeUnit.SECONDS)
}

val taskNames = gradle.startParameter.taskNames
if (taskNames.any { it.contains("build") }
    && taskNames.any { it.contains("publish") }) {
    throw IllegalStateException("Cannot build and publish at the same time.")
}
