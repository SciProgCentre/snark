package documentBuilder

import com.fasterxml.jackson.core.io.BigDecimalParser
import space.kscience.snark.storage.*
import java.nio.file.Path

val DEFAULT_DOCUMENT_ROOT = "main.md"

public suspend fun buildDocument(documentDirectory: Directory) {
    val documentRoot = documentDirectory.get(DEFAULT_DOCUMENT_ROOT)

    TODO()
}