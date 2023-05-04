package documentBuilder

import com.fasterxml.jackson.core.io.BigDecimalParser
import space.kscience.snark.storage.*
import java.nio.file.Path

private val DEFAULT_DOCUMENT_ROOT = "main.md"

public suspend fun buildDocument(documentDirectory: Directory) {
    val dependencyGraph = buildDependencyGraph(documentDirectory)

    TODO() /*resolving of dependencies*/
}

public suspend fun buildDependencyGraph(root: Directory): DependencyGraph {  
    val nodes = HashMap<FileName, DependencyGraphNode>()

    // buildNodes(root, nodes)

    val rootDcoument = root.get(DEFAULT_DOCUMENT_ROOT)

    nodes.put(".", buildDependencyGraphNode(rootDcoument.readAll()))

    val filesToParse = getDependencies(nodes.getValue("."))  

    while (!filesToParse.isEmpty())
    {
        val currentFile = filesToParse.remove()
        val currentDocument = Directory(currentFile).get(DEFAULT_DOCUMENT_ROOT)

        nodes.put(currentFile, buildDependencyGraphNode(currentDocument.readAll()))

        val currentDependencies = getDependencies(nodes.getValue(currentFile))

        for (fileName in currentDependencies) {
            if (!nodes.containsKey(fileName) && !filesToParse.contains(fileName))
                filesToParse.add(fileName)    
        }
    }

    return DependencyGraph(nodes)
}

// private suspend fun buildNodes(folder: Directory, nodes: HashMap<FileName, DependencyGraphNode>) {
//     assert(!nodes.containsKey(folder.getPath()))
// }

public suspend fun getDependencies(node: DependencyGraphNode): Set<FileName> {
    TODO()
}