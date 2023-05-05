package documentBuilder

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Path

private val MARKDOWN_PARSER = "../nodejs/MarkdownParser.js"

public suspend fun parseMd(mdFile: ByteArray): MdAstRoot {
    return jacksonObjectMapper()
        .readValue<MdAstRoot>(ProcessBuilder("node", MARKDOWN_PARSER, mdFile.toString())
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start().inputStream.bufferedReader().readText())
}

public suspend fun buildDependencyGraphNode(mdFile: ByteArray, path: Path): DependencyGraphNode {
    val treeRoot = parseMd(mdFile)
    val dependencies = mutableListOf<DependencyGraphEdge>()

    fillDependencies(treeRoot, dependencies, path)

    return DependencyGraphNode(treeRoot, dependencies)
}

internal suspend fun fillDependencies(
        currentNode: MdAstElement,
        dependencies: MutableList<DependencyGraphEdge>,
        path: Path) {
    when (currentNode) {
        is MdAstParent -> {
            for (child in currentNode.children) {
                if (child is MdAstText) {
                    val includeList = getIncludeFiles(child.value).toMutableList()

                    if (includeList.size > 0) {
                        includeList.replaceAll { path.toString() + "/" + it }

                        dependencies += IncludeDependency(currentNode, child, includeList)
                    }
                } else {
                    fillDependencies(child, dependencies, path)
                }
            }
        }
        else -> {}
    }
}

public suspend fun getIncludeFiles(string: String): List<FileName> {
    TODO()
}