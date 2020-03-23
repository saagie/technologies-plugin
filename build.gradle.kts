/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2020 Pierre Leresteux.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.10.1"
    id("io.gitlab.arturbosch.detekt") version "1.3.1"
    id("org.jmailen.kotlinter") version "2.1.3"
    id("org.kordamp.gradle.project") version "0.32.0"
    kotlin("jvm") version "1.3.70"
}

version = "1.1.3"
group = "com.saagie"

config {
    info {
        name = "Technologies"
        description = "Saagie gradle plugin for technologies"
        inceptionYear = "2019"
        vendor = "Saagie"

        links {
            website = "https://www.saagie.com"
            scm = "https://github.com/saagie/technologies-plugin"
        }

        licensing {
            licenses {
                license {
                    id = "Apache-2.0"
                }
            }
        }

        organization {
            name = "Saagie"
            url = "http://www.saagie.com"
        }

        people {
            person {
                id = "pierre"
                name = "Pierre Leresteux"
                email = "pierre@saagie.com"
                roles = listOf("author", "developer")
            }
            person {
                id = "guillaume"
                name = "Guillaume Naimi"
                email = "guillaume.naimi@saagie.com"
                roles = listOf("developer")
            }
        }
    }
}


object VersionInfo {
    const val kotlin = "1.3.70"
    const val jackson = "2.10.3"
    const val kordamp = "0.32.0"
    const val junit = "5.5.2"
    const val fuel = "2.2.1"
    const val gradledocker="6.3.0"
}

val versions: VersionInfo by extra { VersionInfo }
val github = "https://github.com/saagie/technologies-plugin"
val packageName = "com.saagie.technologies"

repositories {
    jcenter()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib-jdk8", version = versions.kotlin))
    implementation("com.bmuschko:gradle-docker-plugin:${VersionInfo.gradledocker}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${VersionInfo.jackson}")
    implementation("com.fasterxml.jackson.core:jackson-databind:${VersionInfo.jackson}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${VersionInfo.jackson}")
    implementation("com.github.kittinunf.fuel:fuel:${VersionInfo.fuel}")
    implementation("org.kordamp.gradle:project-gradle-plugin:${VersionInfo.kordamp}")

    testImplementation("org.junit.jupiter:junit-jupiter:${VersionInfo.junit}")
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
    baseline = project.rootDir.resolve("detekt-baseline.xml")
}

kotlinter {
    ignoreFailures = false
    reporters = arrayOf("html")
    experimentalRules = false
    disabledRules = arrayOf("import-ordering","no-wildcard-imports")
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
