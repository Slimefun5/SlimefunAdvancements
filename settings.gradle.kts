pluginManagement {
    val shadowVersion: String by settings
    val githubGradleVersion: String by settings
    plugins {
        id("com.gradleup.shadow") version shadowVersion
        id("io.github.intisy.github-gradle") version githubGradleVersion
    }
}

rootProject.name = "sfadvancements"
