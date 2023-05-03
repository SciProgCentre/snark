package documentBuilder

import com.fasterxml.jackson.core.io.BigDecimalParser
import space.kscience.snark.storage.*
import java.nio.file.Path

private val DEFAULT_DOCUMENT_ROOT = "main.md"

public suspend fun buildDocument(documentDirectory: Directory) {

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