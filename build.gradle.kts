plugins {
    java
    id("com.gradleup.shadow")
    id("io.github.intisy.github-gradle")
    id("jacoco")
}

group = "me.char321"
description = "Slimefun Advancements is a Slimefun addon adding an advancement system."

// Shared Slimefun-addon build conventions (Java 8, spigot-api baseline, core dep, publish, shadow, version).
apply(from = "https://raw.githubusercontent.com/Slimefun5/workflows/stable/slimefun-addon.gradle")

repositories {
    maven("https://jitpack.io")
}

dependencies {
    githubImplementation("Slimefun5:SlimefunMetrics:v1.0.0")
    implementation("org.bstats:bstats-bukkit:2.2.1")
    githubImplementation("Slimefun5:AdvancementAPI:v1.0.0")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.15.2")
    testImplementation("org.slf4j:slf4j-simple:2.0.16")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.107.0") {
        exclude(group = "org.jetbrains", module = "annotations")
    }
}

configurations.testImplementation {
    extendsFrom(configurations.compileOnly.get())
}

tasks {
    shadowJar {
        relocate("org.bstats", "slimefunadvancements.libs.bstats")
        relocate("net.roxeez.advancement", "me.char321.sfadvancements.libs.advancementapi")
    }
    compileTestJava { enabled = false }
    test { enabled = false }
}
