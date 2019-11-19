import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.10.1"
    id("io.gitlab.arturbosch.detekt") version "1.1.1" // don't update until kotlin 1.3.41 is supported by Gradle
    id("org.jmailen.kotlinter") version "2.1.3" // don't update until kotlin 1.3.41 supported by Gradle
    kotlin("jvm") version "1.3.60"
}

version = "1.0.0"
group = "com.saagie"

object VersionInfo {
    const val kotlin = "1.3.60"
}

val versions: VersionInfo by extra { VersionInfo }
val github = "https://github.com/saagie/technologies-plugin"
val packageName = "com.saagie.technologies"

repositories {
    jcenter()
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib-jdk8", version = versions.kotlin))
    implementation("com.bmuschko:gradle-docker-plugin:5.3.0")
    compile("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.10.0")
    compile("com.fasterxml.jackson.core:jackson-databind:2.10.0")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")
    testImplementation(kotlin("test", version = versions.kotlin))
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.apiVersion = "1.3"
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}


detekt {
    input = files("src/main/kotlin", "src/test/kotlin")
    filters = ".*/resources/.*,.*/build/.*"
    baseline = project.rootDir.resolve("detekt-baseline.xml")
}

kotlinter {
    ignoreFailures = false
    reporters = arrayOf("html")
    experimentalRules = false
    disabledRules = arrayOf("import-ordering")
}

gradlePlugin {
    plugins {
        create(project.name) {
            id = packageName
            displayName = "Saagie Technologies Plugin"
            description = "Saagie Technologies Plugin for Gradle"
            implementationClass = "$packageName.SaagieTechnologiesGradlePlugin"
        }
    }
}

pluginBundle {
    website = github
    vcsUrl = github
    tags = listOf("saagie", "technologies")
    mavenCoordinates {
        groupId = project.group.toString()
        artifactId = project.name
    }
}