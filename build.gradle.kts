plugins {
    java
    id("com.gradleup.shadow")
    id("io.github.intisy.github-gradle")
    id("jacoco")
}

group = "me.char321"
description = "Slimefun Advancements is a Slimefun addon adding an advancement system."

apply(from = "https://raw.githubusercontent.com/Slimefun5/gradle/stable/slimefun-addon.gradle")

dependencies {
    githubImplementation("Slimefun5:SlimefunMetrics:v1.0.0")
    implementation("org.bstats:bstats-bukkit:2.2.1")
    githubImplementation("Slimefun5:AdvancementAPI:v1.0.0")
}

tasks {
    shadowJar {
        relocate("org.bstats", "slimefunadvancements.libs.bstats")
        relocate("net.roxeez.advancement", "me.char321.sfadvancements.libs.advancementapi")
        archiveFileName.set("SlimefunAdvancements-${project.extra["displayVersion"]}.jar")
    }
}
