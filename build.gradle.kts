plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.2.20"
    id("org.jetbrains.intellij.platform") version "2.16.0"
}

group = "com.kiishi.ite"
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")

    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion").get())
        bundledPlugin("org.jetbrains.plugins.terminal")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
        }
        changeNotes = """
            Initial IntelliJ Platform plugin for iTE.
        """.trimIndent()
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.gradleProperty("intellijPlatformPublishingToken")
        channels = providers.gradleProperty("pluginChannels")
            .map { value -> value.split(",").map { it.trim() }.filter { it.isNotEmpty() } }
            .getOrElse(emptyList())
    }
}

tasks {
    patchPluginXml {
        version = project.version.toString()
        sinceBuild = providers.gradleProperty("pluginSinceBuild")
    }

    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
