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

plugins {
    `kotlin-dsl`
    id("org.kordamp.gradle.base") version "0.54.0"
    id("org.kordamp.gradle.testing") version "0.54.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
    id("org.jmailen.kotlinter") version "4.4.1"
    id("com.gradle.plugin-publish") version "1.2.1"
}

version = "1.3.17"
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
            // disabled because it seems impossible to make the licence check understand who is the copyright owner...
            enabled = false
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
    const val jackson = "2.17.2"
    const val kordamp = "0.54.0"
    const val junit = "5.11.0"
    const val fuel = "2.3.1"
    const val gradledocker = "9.4.0"
}

val github = "https://github.com/saagie/technologies-plugin"
val packageName = "com.saagie.technologies"

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib"))
    implementation("com.bmuschko:gradle-docker-plugin:${VersionInfo.gradledocker}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${VersionInfo.jackson}")
    implementation("com.fasterxml.jackson.core:jackson-databind:${VersionInfo.jackson}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${VersionInfo.jackson}")
    implementation("com.github.kittinunf.fuel:fuel:${VersionInfo.fuel}")
    implementation("org.kordamp.gradle:project-gradle-plugin:${VersionInfo.kordamp}")

    testImplementation("org.junit.jupiter:junit-jupiter:${VersionInfo.junit}")
    testImplementation(kotlin("test"))

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-libraries:1.23.6")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.6")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}


detekt {
    toolVersion = "1.23.6"
    source.setFrom("src/test/kotlin")
    baseline = project.rootDir.resolve("detekt-baseline.xml")
}

kotlinter {
    ignoreFailures = false
    reporters = arrayOf("html")
}

gradlePlugin {
    plugins {
        create(project.name) {
            id = packageName
            displayName = "Saagie Technologies Plugin"
            description = "Saagie Technologies Plugin for Gradle"
            implementationClass = "$packageName.SaagieTechnologiesGradlePlugin"
            tags = listOf("saagie", "technologies")
        }
    }
    website = github
    vcsUrl = github
}
