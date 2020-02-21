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
    id("org.kordamp.gradle.project") version "0.31.2"
    kotlin("jvm") version "1.3.61"
}

version = "1.0.23"
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
    const val kotlin = "1.3.61"
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
    implementation("com.bmuschko:gradle-docker-plugin:6.1.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.10.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.10.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.0")
    implementation("com.github.kittinunf.fuel:fuel:2.2.1")
    implementation("org.kordamp.gradle:project-gradle-plugin:0.30.3")

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
