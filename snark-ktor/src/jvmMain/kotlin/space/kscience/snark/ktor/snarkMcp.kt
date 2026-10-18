package space.kscience.snark.ktor

import io.ktor.server.application.Application
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.*
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.request
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.await
import space.kscience.dataforge.data.forEach
import space.kscience.dataforge.data.get
import space.kscience.dataforge.io.Binary
import space.kscience.dataforge.io.toByteArray
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.*
import space.kscience.snark.Snark
import space.kscience.snark.html.SnarkHtml

/**
 * Initializes the Snark server's metadata and capabilities, and sets up resources and prompts based on the provided data tree.
 *
 * This function configures the server with metadata interpretation, design hints, and resources mapped from the `dataTree`.
 * It processes the tree structure to expose metadata and data as resources, supports design hint prompts, and serves the resources via URI paths.
 *
 * @param context The context used to request dependencies and configurations for building the Snark server.
 * @param dataTree A data tree containing nodes that supply metadata and content, which are published as resources and prompts on the server.
 */
public fun Application.snarkMcp(
    context: Context,
    dataTree: DataTree<Binary>
) {

    val snark = context.request(SnarkHtml)

    val mcp = Server(
        serverInfo = Implementation(
            name = "snark-mcp",
            version = "1.0.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                prompts = ServerCapabilities.Prompts(),
                resources = ServerCapabilities.Resources()
            ),
        )
    )

    val pathArgument = PromptArgument(
        name = "path",
        description = "Path in the tree for which to generate a design hint",
        required = false
    )

    // generate design prompt
    mcp.addPrompt(
        "design-hint",
        description = "Design and metadata interpretation hint for a node with a specific name",
        arguments = listOf(pathArgument),
    ) { request ->

        val path = request.arguments?.get("path")?.parseAsName() ?: Name.EMPTY

        val designHintPrompts = buildList {
            suspend fun addHint(currentPath: Name) {
                val currentDesignPromptPath = currentPath + Snark.SNARK_HINT_KEY + NameToken("design.md")
                val designHintData = dataTree[currentDesignPromptPath]?.await()
                if (designHintData != null) {
                    add(
                        PromptMessage(
                            role = Role.User,
                            content = TextContent(text = designHintData.toByteArray().decodeToString()),
                        )
                    )
                }
            }

            var currentPath = Name.EMPTY
            for (token in path.tokens) {

                addHint(currentPath)
                currentPath += token
            }
            addHint(currentPath)
        }

        GetPromptResult(
            description = "A design hint for node with specific path. It includes design hints for all parent nodes if present",
            messages = designHintPrompts
        )
    }

    mcp.addResource(
        uri = "snark://tree",
        name = "tree",
        description = "A text tree showing data hierarchy",
        mimeType = "text/plain"
    ) { request ->
        val treeView = buildString {
            fun renderNode(prefix: String, token: NameToken, node: DataTree<*>) {
                val suffix = node.data?.let {
                    " (${it.meta["type"].string ?: snark.getContentType(token.asName(), it.meta)})"
                } ?: ""
                appendLine("$prefix- ${token.toStringUnescaped()}$suffix")
                node.items.forEach { (childToken, childData) ->
                    renderNode("$prefix  ", childToken, childData)
                }
            }

            renderNode("", NameToken("tree"), dataTree)
        }
        ReadResourceResult(
            contents = listOf(
                TextResourceContents(
                    text = treeView,
                    uri = request.uri,
                    mimeType = "text/plain",
                )
            )
        )
    }

    dataTree.forEach { namedData ->
        //serve metadata
        mcp.addResource(
            uri = "snark://metadata/${namedData.name}",
            name = namedData.name.toStringUnescaped() + "/meta",
            description = "Metadata for data tree node with name ${namedData.name}",
            mimeType = "application/json",
        ) { request ->
            ReadResourceResult(
                contents = listOf(
                    TextResourceContents(
                        text = namedData.meta.toString(),
                        uri = request.uri,
                        mimeType = "application/json",
                    )
                )
            )
        }

        //serve text content
        val contentType = snark.getContentType(namedData.name, namedData.meta)

        if (contentType in listOf("application/json", "application/yaml", "text/markdown", "text/plain")) {

            mcp.addResource(
                uri = "snark://data/${namedData.name}",
                name = namedData.name.toStringUnescaped() + "/data",
                description = "Data content for tree node with name ${namedData.name}",
                mimeType = contentType,
            ) { request ->
                ReadResourceResult(
                    contents = listOf(
                        TextResourceContents(
                            text = namedData.await().toByteArray().decodeToString(),
                            uri = request.uri,
                            mimeType = contentType,
                        )
                    )
                )
            }


        }
    }

    mcpStreamableHttp { mcp }
}