package documentBuilder

import com.fasterxml.jackson.core.io.BigDecimalParser
import space.kscience.snark.storage.*
import space.kscience.snark.storage.local.LocalDirectory
import java.nio.file.Path

val DEFAULT_DOCUMENT_ROOT = "main.md"

public suspend fun buildDocument(documentPath: String) {
    val documentDirectory: Directory = LocalDirectory(documentPath)

    val dependencyGraph = buildDependencyGraph(documentDirectory)

    TODO() /*resolving of dependencies*/
}

public suspend fun buildDependencyGraph(root: Directory) : DependencyGraph {
    // val rootDcoument = root.get(DEFAULT_DOCUMENT_ROOT)

    // val filesToParse: Queue<FileReader> = LinkedList<FileReader>(listOf(root))
    // var documentName = "."

    // val nodes = HashMap<FileName, DependencyGraphNode>()

    // while (!filesToParse.isEmpty())
    // {
    //     dependencyGraphNode = buildDependencyNode(filesToParse.remove())

    //     nodes.put()
    // }
    TODO()
}