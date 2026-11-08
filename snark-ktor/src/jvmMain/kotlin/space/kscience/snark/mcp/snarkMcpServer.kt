package space.kscience.snark.mcp

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import space.kscience.dataforge.data.*
import space.kscience.dataforge.io.Binary
import space.kscience.dataforge.io.toByteArray
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.meta.toJson
import space.kscience.dataforge.names.*
import space.kscience.snark.html.SnarkHtml

/**
 * Creates an MCP server for reading Snark data as prompts and resources.
 *
 * The returned server exposes a design hint prompt that resolves design metadata from
 * `design.md` files in the requested node path and its ancestors. It also exposes a
 * text representation of the data tree, metadata resources for each data node, and
 * text data resources for supported textual content types.
 *
 * Resource registrations are kept in sync with updates from the provided data tree.
 *
 * @param dataTree the binary data tree to expose through the MCP server.
 * @return a configured MCP server backed by the provided data tree.
 */
public fun SnarkHtml.snarkMcpServer(
    dataTree: DataTree<Binary>
): Server = Server(
    serverInfo = Implementation(
        name = "snark-read-mcp",
        version = "1.0.0"
    ),
    options = ServerOptions(
        capabilities = ServerCapabilities(
            prompts = ServerCapabilities.Prompts(),
            resources = ServerCapabilities.Resources(listChanged = true, subscribe = true),
            tools = ServerCapabilities.Tools(),
        ),
    )
).apply {

    val pathArgument = PromptArgument(
        name = "path",
        description = "Path in the tree for which to generate a design hint",
        required = false
    )
    // generate design prompt
    addPrompt(
        "design-hint",
        description = "Design and metadata interpretation hint for a node with a specific name",
        arguments = listOf(pathArgument),
    ) { request ->

        val path = request.arguments?.get("path")?.parseAsName() ?: Name.EMPTY

        val designHintPrompts = buildList {
            suspend fun addHint(currentPath: Name) {
                val currentDesignPromptPath = currentPath + NameToken("design.md")
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

    // full tree of provided data
    addResource(
        uri = "snark://tree",
        name = "tree",
        description = "A text tree showing data hierarchy",
        mimeType = "text/plain"
    ) { request ->
        val treeView = buildString {
            fun renderNode(prefix: String, token: NameToken, node: DataTree<*>) {
                val suffix = node.data?.let {
                    " (${it.meta["type"].string ?: getContentType(token.asName(), it.meta)})"
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

    fun addData(name: Name, data: Data<Binary>) {

        //skip service data
        if (data.meta["type"].string?.startsWith("snark") == true) return

        //serve metadata
        addResource(
            uri = "snark://metadata/${name}",
            name = name.toStringUnescaped() + "/meta",
            description = "Metadata for data tree node with name $name",
            mimeType = "application/json",
        ) { request ->
            ReadResourceResult(
                contents = listOf(
                    TextResourceContents(
                        text = data.meta.toString(),
                        uri = request.uri,
                        mimeType = "application/json",
                    )
                )
            )
        }

        //serve text content
        val contentType = getContentType(name, data.meta)

        if (contentType in listOf("application/json", "application/yaml", "text/markdown", "text/plain")) {

            addResource(
                uri = "snark://data/${name}",
                name = name.toStringUnescaped() + "/data",
                description = "Data content for tree node with name $name",
                mimeType = contentType,
            ) { request ->
                ReadResourceResult(
                    contents = listOf(
                        TextResourceContents(
                            text = data.await().toByteArray().decodeToString(),
                            uri = request.uri,
                            mimeType = contentType,
                        )
                    )
                )
            }
        }
    }

    dataTree.forEach { namedData ->
        addData(namedData.name, namedData)
    }

    dataTree.updates.onEach { name ->
        val data = dataTree[name]
        if (data == null) {
            removeResource("snark://data/${name}")
            removeResource("snark://metadata/${name}")
        } else {
            addData(name, data)
        }
    }.launchIn(context)

    addTool(
        name = "readData",
        description = "Read textual or binary data from specific tree node",
    ) { request ->
        val name = request.arguments?.get("name")?.jsonPrimitive?.content?.parseAsName()
            ?: return@addTool CallToolResult(content = listOf(TextContent(text = "No name provided")))

        val data = dataTree[name]
        if (data == null) {
            CallToolResult(content = listOf(TextContent(text = "No data found for name $name")))
        } else {
            val contentType = getContentType(name, data.meta)
            if (contentType in listOf("application/json", "application/yaml", "text/markdown", "text/plain")) {
                CallToolResult(content = listOf(TextContent(text = data.await().toByteArray().decodeToString())))
            } else {
                CallToolResult(content = listOf(TextContent(text = "Non-textual content type: $contentType")))
            }
        }
    }

    addTool(
        name = "readMeta",
        description = "Read metadata associated with a specific tree node",
    ) { request ->
        val name = request.arguments?.get("name")?.jsonPrimitive?.content?.parseAsName()
            ?: return@addTool CallToolResult(content = listOf(TextContent(text = "No name provided")))

        val data = dataTree[name]
        if (data == null) {
            CallToolResult(content = listOf(TextContent(text = "No data found for name $name")))
        } else {
            CallToolResult(
                content = listOf(TextContent(text = data.meta.toString())),
                structuredContent = data.meta.toJson().let { jsonElement ->
                    jsonElement as? JsonObject ?: buildJsonObject { put("value", jsonElement) }
                }
            )
        }
    }
}