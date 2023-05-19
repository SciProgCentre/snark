package documentBuilder

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeBytes

private val MARKDOWN_PARSER = "snark-document-builder/src/main/nodejs/MarkdownParser.js"
private val SNARK_PARSER = "snark-document-builder/src/main/python/SnarkParser.py"

internal fun prepareResources(resourceName: String, expectedPath: Path) {
    val text = object {}.javaClass.getResource(resourceName)?.readBytes()
    if (text != null) {
        expectedPath.parent.createDirectories()
        expectedPath.createFile()
        expectedPath.writeBytes(text)
    }
}

public suspend fun parseMd(mdFile: ByteArray, parserPath: String = MARKDOWN_PARSER): MdAstRoot {
    val process = ProcessBuilder("node", parserPath, String(mdFile))
    val result = process
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start().inputStream.bufferedReader().readText()

    return jacksonObjectMapper().readValue<MdAstRoot>(result)
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
    return jacksonObjectMapper()
        .readValue<List<FileName>>(ProcessBuilder("python3", SNARK_PARSER, string)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start().inputStream.bufferedReader().readText())
}