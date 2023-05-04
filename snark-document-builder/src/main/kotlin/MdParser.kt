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
    TODO()
}