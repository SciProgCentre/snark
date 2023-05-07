package documentBuilder

import com.fasterxml.jackson.core.io.BigDecimalParser
import space.kscience.snark.storage.*
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.html.*
import kotlinx.html.dom.createHTMLDocument
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

private val DEFAULT_DOCUMENT_ROOT = "main.md"

public suspend fun buildDocument(documentDirectory: Directory): String {
    val dependencyGraph = buildDependencyGraph(documentDirectory)
    val roooot: MdAstRoot = dependencyGraph.nodes[""]!!.mdAst

    return jacksonObjectMapper().writeValueAsString(roooot)
    // TODO прикрутить html
}

public suspend fun buildDependencyGraph(root: Directory): DependencyGraph {
    val nodes = HashMap<FileName, DependencyGraphNode>()

    buildNodes(root, nodes)

    return DependencyGraph(nodes)
}

private suspend fun buildNodes(folder: Directory, nodes: HashMap<FileName, DependencyGraphNode>) {
    val pathString = folder.path.toString()

    assert(!nodes.containsKey(pathString))

    val rootDcoument = folder.get(DEFAULT_DOCUMENT_ROOT)
    nodes.put(pathString, buildDependencyGraphNode(rootDcoument.readAll(), folder.path))

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