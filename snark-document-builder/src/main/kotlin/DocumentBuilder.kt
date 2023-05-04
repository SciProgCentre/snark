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

    buildNodes(root, nodes)

    return DependencyGraph(nodes)
}

private suspend fun buildNodes(folder: Directory, nodes: HashMap<FileName, DependencyGraphNode>) {
    assert(!nodes.containsKey(folder.getPath()))

    val path = folder.getPath()
    val rootDcoument = folder.get(DEFAULT_DOCUMENT_ROOT)
    nodes.put(path, buildDependencyGraphNode(rootDcoument.readAll()))

    val dependencies = getDependencies(nodes.getValue(path))

    for (dependency in dependencies) {
        if (!nodes.containsKey(dependency))
            buildNodes(folder.getSubdir(dependency), nodes)
    }
}

public suspend fun getDependencies(node: DependencyGraphNode): Set<FileName> {
    TODO()
}