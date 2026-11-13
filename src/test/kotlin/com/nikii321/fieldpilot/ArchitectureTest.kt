package com.nikii321.fieldpilot

import java.nio.file.*
import kotlin.io.path.*
import kotlin.test.*
import org.junit.jupiter.api.Test

class ArchitectureTest {
    @Test
    fun `one public type per file and domain independent from Spring`() {
        Files.walk(Path.of("src/main/kotlin")).use { paths ->
            paths
                .filter { it.extension == "kt" }
                .forEach { p ->
                    val text = p.readText()
                    val types =
                        Regex(
                                "(?m)^(?:@[A-Za-z0-9_.]+[ \t]+)*(?:data |enum |sealed )?(?:class|object|interface) ([A-Za-z0-9_]+)",
                            )
                            .findAll(text)
                            .toList()
                    assertEquals(1, types.size, p.toString())
                    assertEquals(p.nameWithoutExtension, types.single().groupValues[1])
                    if ("/domain/" in p.toString()) assertFalse("org.springframework" in text)
                }
        }
    }
}
