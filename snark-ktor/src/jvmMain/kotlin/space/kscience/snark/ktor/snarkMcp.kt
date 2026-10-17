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
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.parseAsName
import space.kscience.dataforge.names.plus
import space.kscience.snark.Snark
import space.kscience.snark.html.SnarkHtml.Companion.CONTENT_TYPE_KEY
import kotlin.reflect.typeOf

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
    dataTree: DataTree<*>
) {

    val snarkPlugin = context.request(Snark)

    val mcp = Server(
        serverInfo = Implementation(
            name = "snark-server",
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
                val currentDesignPromptPath = currentPath + Snark.SNARK_NAME_TOKEN + "design"
                val designHintData = dataTree[currentDesignPromptPath]?.await()
                if (designHintData != null && designHintData is String) {
                    add(
                        PromptMessage(
                            role = Role.User,
                            content = TextContent(text = designHintData),
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

    dataTree.forEach { namedData ->

        mcp.addResource(
            uri = "snark://metadata/${namedData.name}",
            name = namedData.toString(),
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

        val contentType = namedData.meta[CONTENT_TYPE_KEY].string

        when (namedData.type) {
            typeOf<String>() -> mcp.addResource(
                uri = "snark://data/${namedData.name}",
                name = namedData.toString(),
                description = "Markdown data content for data tree node with name ${namedData.name}",
                mimeType = contentType ?: "text/plain",
            ) { request ->
                ReadResourceResult(
                    contents = listOf(
                        TextResourceContents(
                            text = namedData.await().toString(),
                            uri = request.uri,
                            mimeType = "text/plain",
                        )
                    )
                )
            }

            typeOf<Meta>() -> mcp.addResource(
                uri = "snark://data/${namedData.name}",
                name = namedData.toString(),
                description = "Json data content for data tree node with name ${namedData.name}",
                mimeType = "application/json",
            ) { request ->
                ReadResourceResult(
                    contents = listOf(
                        TextResourceContents(
                            text = namedData.await().toString(),
                            uri = request.uri,
                            mimeType = "application/json",
                        )
                    )
                )
            }
        }


    }

    mcpStreamableHttp { mcp }
}