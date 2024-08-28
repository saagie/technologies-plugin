package com.saagie.technologies

import com.fasterxml.jackson.databind.JsonMappingException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

data class DataTest(
    val toto: String,
    val titi: String,
    val float: String,
)

class SaagieTechnologiesPackageGradlePluginKtTest {
    @Test
    fun `Verify float value throw exception in a YAML`() {
        // Given
        val jacksonObject = yamlMapper()
        // When
        val thrown: JsonMappingException =
            assertThrows(JsonMappingException::class.java) {
                jacksonObject.readValue(File("src/test/resources/assets/yamlWithFloat.yaml"), DataTest::class.java)
            }

        thrown.message?.let { assertTrue(it.contains("this float value is ambiguous : ")) }
    }

    @Test
    fun `Verify float as String value is OK in a YAML`() {
        // Given
        val jacksonObject = yamlMapper()

        // When
        val result = jacksonObject.readValue(File("src/test/resources/assets/yamlWithString.yaml"), DataTest::class.java)

        assertTrue(result != null)
        assertTrue(result == DataTest("2", "3", "1.02"))
    }

    @Test
    fun `Verify float value throw exception in a JSON`() {
        // Given
        val jacksonObject = jsonMapper()

        // When
        val thrown: JsonMappingException =
            assertThrows(JsonMappingException::class.java) {
                jacksonObject.readValue(File("src/test/resources/assets/jsonWithFloat.json"), DataTest::class.java)
            }

        thrown.message?.let { assertTrue(it.contains("this float value is ambiguous : ")) }
    }

    @Test
    fun `Verify float as String value is OK in a JSON`() {
        // Given
        val jacksonObject = jsonMapper()

        // When
        val result = jacksonObject.readValue(File("src/test/resources/assets/jsonWithString.json"), DataTest::class.java)

        assertTrue(result != null)
        assertTrue(result == DataTest("2", "3", "1.02"))
    }
}
