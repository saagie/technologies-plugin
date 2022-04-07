/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2022 Creative Data.
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
    id("com.gradle.plugin-publish") version "0.13.0"
    id("io.gitlab.arturbosch.detekt") version "1.15.0"
    id("org.jmailen.kotlinter") version "3.3.0"
    id("org.kordamp.gradle.project") version "0.46.0"
    kotlin("jvm") version "1.4.20"
}

version = "1.3.5"
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
            enabled = false // disabled because it seems impossible to make the licence check understand who is the copyright owner...
            licenses {
                license {
                    id = "Apache-2.0"
                }
            }
        }

        people {
            person {
                id = "Creative Data"
                name = "Creative Data"
            }
        }

        organization {
            name = "Saagie"
            url = "http://www.saagie.com"
        }
    }
}


object VersionInfo {
    const val kotlin = "1.4.20"
    const val jackson = "2.12.1"
    const val kordamp = "0.43.0"
    const val junit = "5.7.1"
    const val fuel = "2.3.1"
    const val gradledocker = "6.7.0"
    const val gradleNode = "3.2.1"
}

val versions: VersionInfo by extra { VersionInfo }
val github = "https://github.com/saagie/technologies-plugin"
val packageName = "com.saagie.technologies"

repositories {
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
    implementation("com.github.node-gradle:gradle-node-plugin:${VersionInfo.gradleNode}")

    testImplementation("org.junit.jupiter:junit-jupiter:${VersionInfo.junit}")
    testImplementation(kotlin("test", version = versions.kotlin))
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.apiVersion = "1.4"
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
