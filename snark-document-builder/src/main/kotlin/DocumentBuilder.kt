package documentBuilder

import com.fasterxml.jackson.core.io.BigDecimalParser
import space.kscience.snark.storage.*
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.html.*
import kotlinx.html.dom.createHTMLDocument
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.serialization.json.Json
import kotlin.io.path.*

private val SNARK_HTML_RENDER = "snark-document-builder/src/main/nodejs/HtmlRenderer.js"
fun getHtml(ast_string: String): String
{
    return ProcessBuilder("node", SNARK_HTML_RENDER, ast_string)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start().inputStream.bufferedReader().readText()
}

private val DEFAULT_DOCUMENT_ROOT = "main.md"

public suspend fun buildDocument(root: Directory, path: Path): String {
    val dependencyGraph = buildDependencyGraph(root, path)

    println("dependency graph nodes (size: ${dependencyGraph.nodes.size}):\n${dependencyGraph.nodes}")

    val root: MdAstRoot = dependencyGraph.nodes[path.toString()]!!.mdAst

    return getHtml(jacksonObjectMapper().writeValueAsString(root))
}

public suspend fun buildDependencyGraph(root: Directory, path: Path): DependencyGraph {
    val nodes = HashMap<FileName, DependencyGraphNode>()

    buildNodes(root, path, nodes)

    return DependencyGraph(nodes)
}

private suspend fun buildNodes(root: Directory, path: Path, nodes: HashMap<FileName, DependencyGraphNode>) {
    val pathString = path.toString()

    assert(!nodes.containsKey(pathString))

    val rootDcoument = (root / path).get(DEFAULT_DOCUMENT_ROOT)
    nodes.put(pathString, buildDependencyGraphNode(rootDcoument.readAll(), path))

    val dependencies = getDependencies(nodes.getValue(pathString))

    for (dependency in dependencies) {
        if (!nodes.containsKey(dependency))
            buildNodes(root, Path(dependency), nodes)
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