package io.saagie.technologies

import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerLogsContainer
import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import com.bmuschko.gradle.docker.tasks.container.DockerWaitContainer
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPullImage
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.saagie.technologies.model.Metadata
import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

class SaagieTechnologiesGradlePlugin : Plugin<Project> {
    companion object {
        @JvmField val TIMEOUT_TEST_CONTAINER = 10
    }

    override fun apply(project: Project) {

        val metadata = readMetadata(project.projectDir)
        val imageName = generateDockerTag(project, metadata)

        val imageTestNameDetails = Pair("gcr.io/gcp-runtimes/container-structure-test", "latest")
        val imageTestName = "${imageTestNameDetails.first}:${imageTestNameDetails.second}"
        var logs = ""

        val buildImage = project.tasks.create<DockerBuildImage>("buildImage") {
            this.inputDir.set(File("."))
            this.tags.add(imageName)
        }

        val pullDockerImage = project.tasks.create<DockerPullImage>("pullDockerImage") {
            repository.set(imageTestNameDetails.first)
            tag.set(imageTestNameDetails.second)
        }

        val createContainer = project.tasks.create<DockerCreateContainer>("createContainer") {
            dependsOn(pullDockerImage)
            targetImageId(imageTestName)
            autoRemove.set(false)
            binds.put("${project.projectDir.absolutePath}/image_test.yml", "/workdir/image_test.yml")
            binds.put("/var/run/docker.sock", "/var/run/docker.sock")
            workingDir.set("/workdir")
            cmd.addAll("test", "--image", imageName, "--config", "/workdir/image_test.yml")
        }

        val startContainer = project.tasks.create<DockerStartContainer>("startContainer") {
            dependsOn(createContainer)
            targetContainerId(createContainer.containerId)
        }

        val removeContainer = project.tasks.create<DockerRemoveContainer>("removeContainer") {
            force.set(true)
            targetContainerId(createContainer.containerId)
        }

        val logContainer = project.tasks.create<DockerLogsContainer>("logContainer") {
            dependsOn(startContainer)
            targetContainerId(createContainer.containerId)
            follow.set(true)
            tailAll.set(true)
            onNext {
                logs += "$this \n"
            }
        }

        val buildWaitContainer = project.tasks.create<DockerWaitContainer>("buildWaitContainer") {
            dependsOn(logContainer)
            targetContainerId(createContainer.containerId)
            awaitStatusTimeout.set(TIMEOUT_TEST_CONTAINER)
            doLast {
                if (exitCode != 0) {
                    logger.error(logs)
                    throw GradleException("Tests on ${project.name} failed")
                }
            }
            finalizedBy(removeContainer)
        }

        val testImage = project.tasks.create("testImage") {
            dependsOn(buildImage, buildWaitContainer)
            startContainer.mustRunAfter(buildImage)
        }

        val generateMetadata = project.tasks.create("generateMetadata") {
            dependsOn(testImage)
            storeMetadata(project, project.projectDir, metadata)
        }

        val buildDockerImage = project.tasks.create("buildDockerImage") {
            group = "technologies"
            description = "Build techno"
            dependsOn("generateMetadata")
        }
    }

    fun readMetadata(projectDir: File): Metadata =
        getJacksonObjectMapper().readValue(
            File("${projectDir.parentFile.absoluteFile}/techno.yml").inputStream(),
            Metadata::class.java
        )

    fun getJacksonObjectMapper(): ObjectMapper = ObjectMapper(
        YAMLFactory()
            .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
            .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
    )
        .registerModule(KotlinModule()).setSerializationInclusion(JsonInclude.Include.NON_NULL)

    fun generateDockerTag(project: Project, metadata: Metadata) =
        "${metadata.techno.docker.image}:${project.generateTag()}"

    fun storeMetadata(project: Project, projectDir: File, metadata: Metadata) {
        val targetMetadata = File("${projectDir.absolutePath}/metadata.yml")
        targetMetadata.delete()
        File("${projectDir.absolutePath}/version.yml").copyTo(targetMetadata)
        targetMetadata.appendText("\n" +
            getJacksonObjectMapper().writeValueAsString(
                metadata.copy(
                    metadata.techno.copy(
                        docker = metadata.techno.docker.copy(version = project.generateTag())
                    )
                )
            )
        )
    }
}

fun Project.generateTag(): String = "${this.name}-${this.getVersionForDocker()}"
fun Project.getVersionForDocker(): String = "${this.rootProject.version}".replace("+", "_")
