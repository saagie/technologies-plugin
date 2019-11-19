package io.saagie.technologies

import io.saagie.technologies.model.Metadata
import io.saagie.technologies.model.MetadataDocker
import io.saagie.technologies.model.MetadataTechno
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File


class SaagieTechnologiesGradlePluginKtTest {


    @Nested
    inner class ProjectKotlinExtension {

        @TempDir
        lateinit var projectdir: File

        @Test
        fun `generateVersionForDocker with simple version`() {

            with(
                ProjectBuilder.builder()
                    .withProjectDir(projectdir)
                    .withName("myproject")
                    .build()
            ) {


                this.version = "1.2.3"
                assertEquals(this.version, this.getVersionForDocker())
            }

        }

        @Test
        fun `generateVersionForDocker with complex version`() {

            with(
                ProjectBuilder.builder()
                    .withProjectDir(projectdir)
                    .withName("myproject")
                    .build()
            ) {


                this.version = "1.2.3+TEST"
                assertEquals(this.version.toString().replace("+", "_"), this.getVersionForDocker())
            }

        }


        @Test
        fun `generateTag`() {

            with(
                ProjectBuilder.builder()
                    .withProjectDir(projectdir)
                    .withName("myproject")
                    .build()
            ) {

                this.version = "1.2.3"
                assertEquals("${this.name}-${this.getVersionForDocker()}", this.generateTag())
            }

        }
    }

    @Nested
    inner class StoreMetadata {


        @TempDir
        lateinit var projectdir: File

        @Test
        fun `generate and create metadata(dot)yml`() {
            with(
                ProjectBuilder.builder()
                    .withProjectDir(projectdir)
                    .withName("myproject")
                    .build()
            ) {
                this.version="1.2.3"
                val plugin = SaagieTechnologiesGradlePlugin()

                File("$projectDir/version.yml").appendText(
                    """
                    version:
                      label: here_is_a_test
                """.trimIndent()
                )

                val techno = MetadataTechno(
                    "technoId", "technoLabel", true, null, "technoIcon", "technoRecommendedVersion",
                    MetadataDocker("image", "1.2.3")
                )
                val metadata = Metadata(techno = techno)
                plugin.storeMetadata(project, projectDir, metadata)
                val metadataFinalFile = File("${project.projectDir.absolutePath}/metadata.yml")
                assertTrue(metadataFinalFile.exists())
                assertEquals(
"""version:
  label: here_is_a_test
techno:
  id: ${techno.id}
  label: ${techno.label}
  icon: ${techno.icon}
  recommendedVersion: ${techno.recommendedVersion}
  docker:
    image: ${techno.docker.image}
    version: ${project.name}-${techno.docker.version}
  available: ${techno.isAvailable}""".trimMargin(),
                    metadataFinalFile.readText().trimIndent())
            }
        }
    }
}
