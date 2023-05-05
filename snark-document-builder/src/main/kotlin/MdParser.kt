package documentBuilder

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

private val MARKDOWN_PARSER = "../nodejs/MarkdownParser.js"

public suspend fun parseMd(mdFile: ByteArray): MdAstRoot {
    return jacksonObjectMapper()
        .readValue<MdAstRoot>(ProcessBuilder("node", MARKDOWN_PARSER, mdFile.toString())
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start().inputStream.bufferedReader().readText())
}

public suspend fun buildDependencyGraphNode(mdFile: ByteArray): DependencyGraphNode {
    val treeRoot = parseMd(mdFile)
    val dependencies = mutableListOf<DependencyGraphEdge>()

    fillDependencies(treeRoot, dependencies)

    return DependencyGraphNode(treeRoot, dependencies)
}

private suspend fun fillDependencies(
        currentNode: MdAstElement,
        dependencies: MutableList<DependencyGraphEdge>) {
    // when (currentNode) {
    //     is MdAstParent -> {
    //         val iterator = currentNode.children.listIterator()

    //         while (iterator.hasNext()) {


    //             iterator.next()
    //         }

    //     }
    //     else -> {}
    // }
    TODO()
}