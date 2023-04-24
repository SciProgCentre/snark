package documentBuilder

import com.fasterxml.jackson.core.io.BigDecimalParser
import space.kscience.snark.storage.*
import space.kscience.snark.storage.local.LocalDirectory
import java.nio.file.Path

val DEFAULT_DOCUMENT_ROOT = "main.md"

public suspend fun buildDocument(documentPath: String) {
    val documentDirectory: Directory = LocalDirectory(documentPath)
    val documentRoot = documentDirectory.get(DEFAULT_DOCUMENT_ROOT)

    val (dependencies, vertex) = parseMd(documentRoot.readAll())

    val documentDependencies: HashMap<String, Dependencies> = HashMap<String, Dependencies>()
    val dependencyGraph: HashMap<String, DependencyGraphVertex> = 
            HashMap<String, DependencyGraphVertex>()

    documentDependencies.put(documentPath, dependencies)
    dependencyGraph.put(documentPath, vertex)
}