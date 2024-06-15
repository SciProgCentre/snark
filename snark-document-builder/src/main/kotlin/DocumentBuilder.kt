package documentBuilder

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import space.kscience.snark.pandoc.PandocCommandBuilder
import space.kscience.snark.pandoc.PandocWrapper
import space.kscience.snark.storage.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

private val SNARK_HTML_RENDER = "snark-document-builder/src/main/nodejs/HtmlRenderer.js"
private val SNARK_MD_RENDERER = "snark-document-builder/src/main/nodejs/MdRenderer.js"
fun getHtml(ast_string: String): String
{
    return ProcessBuilder("node", SNARK_HTML_RENDER, ast_string)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start().inputStream.bufferedReader().readText()
}

fun getLatex(ast_string: String) : Path
{
    val outputMd = Files.createTempFile(Path.of("./data/"), "output", ".md")

    val output = ProcessBuilder("node", SNARK_MD_RENDERER, ast_string)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start().inputStream.bufferedReader().readText()
    outputMd.writeText(output)

    val outputTex = Files.createTempFile(Path.of("./data/"), "output", ".tex")

    val pandocWrapper = PandocWrapper()
    pandocWrapper.use { p: PandocWrapper? ->
        val command = PandocCommandBuilder(
            listOf<Path>(outputMd),
            outputTex
        )
        PandocWrapper.execute(command)
    }

    return outputTex
}

private val DEFAULT_DOCUMENT_ROOT = "main.md"

public suspend fun buildDocument(root: Directory, path: Path): String {
    val dependencyGraph = buildDependencyGraph(root, path)

    val graphManage = GraphManager(dependencyGraph)

    graphManage.buildDocument(path.toString())

//    for ((key, value) in dependencyGraph.nodes) {
//        println("Key ${key}")
//        println("Value.mdAst ${value.mdAst}")
//        println("Value.dependencies ${value.dependencies}")
//    }

    val root: MdAstRoot = dependencyGraph.nodes[path.toString()]!!.mdAst

    return getHtml(jacksonObjectMapper().writeValueAsString(root))
}

public suspend fun buildLatex(root: Directory, path: Path) : Path {
    val dependencyGraph = buildDependencyGraph(root, path)

    val graphManage = GraphManager(dependencyGraph)

    graphManage.buildDocument(path.toString())

    val root: MdAstRoot = dependencyGraph.nodes[path.toString()]!!.mdAst

    return getLatex(jacksonObjectMapper().writeValueAsString(root))
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