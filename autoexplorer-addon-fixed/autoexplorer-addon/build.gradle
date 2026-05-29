// ─────────────────────────────────────────────────────────────────────────────
// AutoExplorer Addon — build.gradle
//
// Targets:
//   Minecraft  1.21.4
//   Fabric API 0.x (latest stable for 1.21.4)
//   Java       21  (minimum required by Fabric for 1.21.x)
//   Meteor     0.5.x  (latest release compatible with 1.21.4)
//
// Run `./gradlew build` to compile.
// The output JAR lands in build/libs/ — drop it in .minecraft/mods/.
// ─────────────────────────────────────────────────────────────────────────────

plugins {
    id 'fabric-loom'             version '1.9-SNAPSHOT'
    id 'maven-publish'
}

// ── Project metadata ─────────────────────────────────────────────────────────
version      = project.mod_version
group        = project.maven_group
base.archivesName = project.archives_base_name

// ── Java toolchain ────────────────────────────────────────────────────────────
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
}

// ── Compile options ───────────────────────────────────────────────────────────
tasks.withType(JavaCompile).configureEach {
    options.release = 21
    options.compilerArgs += ['-Xlint:all', '-Xlint:-processing']
}

// ── Repositories ──────────────────────────────────────────────────────────────
repositories {
    mavenCentral()

    // Fabric official maven — needed for yarn mappings and fabric-loader
    maven {
        name = "Fabric"
        url  = "https://maven.fabricmc.net/"
    }

    // Meteor Client releases
    maven {
        name = "Meteor Client"
        url  = "https://maven.meteordev.org/releases"
    }

    // Meteor Client snapshots
    maven {
        name = "Meteor Client Snapshots"
        url  = "https://maven.meteordev.org/snapshots"
    }

    // JitPack — Baritone API lives here (MeteorDevelopment fork)
    maven {
        name = "JitPack"
        url  = "https://jitpack.io"
    }
}

// ── Dependencies ──────────────────────────────────────────────────────────────
dependencies {

    // ── Minecraft + mappings ────────────────────────────────────────────────
    minecraft         "com.mojang:minecraft:${project.minecraft_version}"
    mappings          "net.fabricmc:yarn:${project.yarn_mappings}:v2"
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"

    // ── Fabric API ───────────────────────────────────────────────────────────
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"

    // ── Meteor Client ────────────────────────────────────────────────────────
    // compileOnly because Meteor is present at runtime (player installs it).
    modCompileOnly("meteordevelopment:meteor-client:${project.meteor_version}") {
        transitive = false
    }

    // ── Baritone API ─────────────────────────────────────────────────────────
    // Meteor bundles its own Baritone fork. We pull the API JAR from JitPack
    // so the compiler can resolve IBaritone, IExploreProcess, etc.
    // The group on JitPack is com.github.MeteorDevelopment, repo is baritone.
    modCompileOnly "com.github.MeteorDevelopment:baritone:${project.baritone_version}"
}

// ── Loom configuration ────────────────────────────────────────────────────────
loom {
    // Uncomment if you add Mixins later:
    // runs.configureEach { vmArg "-Dmixin.debug.export=true" }
}

// ── Publishing (optional) ─────────────────────────────────────────────────────
publishing {
    publications {
        create("mavenJava", MavenPublication) {
            from components.java
        }
    }
    repositories {
        // maven { url = layout.buildDirectory.dir("repo") }
    }
}
