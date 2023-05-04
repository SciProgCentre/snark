package documentBuilder

import com.fasterxml.jackson.core.io.BigDecimalParser
import space.kscience.snark.storage.*
import java.nio.file.Path
import java.nio.file.Paths

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
    val pathString = folder.getPath().toString()
    
    assert(!nodes.containsKey(pathString))

    val rootDcoument = folder.get(DEFAULT_DOCUMENT_ROOT)
    nodes.put(pathString, buildDependencyGraphNode(rootDcoument.readAll()))

    val dependencies = getDependencies(nodes.getValue(pathString))

    for (dependency in dependencies) {
        if (!nodes.containsKey(dependency))
            buildNodes(folder.getSubdir(Paths.get(dependency)), nodes)
    }
}

public suspend fun getDependencies(node: DependencyGraphNode): Set<FileName> {
    val dependencies = mutableListOf<FileName>()
    
    for (dependency in node.dependencies) {
        when (dependency) {
            is IncludeDependency -> dependencies.addAll(dependency.includeList)
        }
    }

    return dependencies.toSet()
}