package space.kscience.snark.mcp

import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.*
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import space.kscience.dataforge.context.error
import space.kscience.dataforge.context.logger
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.io.Binary
import space.kscience.snark.html.SnarkHtml

/**
 * Starts an MCP server for this [SnarkHtml] plugin using standard input and standard output as the transport.
 *
 * The server exposes the provided binary data tree through a stdio-based MCP session and keeps the launched coroutine active
 * until the server is closed.
 *
 * @param data The binary data tree to expose through the MCP server.
 * @return The [Job] representing the launched MCP stdio server coroutine.
 */
public fun SnarkHtml.mcpStdIo(
    data: DataTree<Binary>
): Job = context.launch {
    try {

        val server = snarkMcpServer(data)

        val transport = StdioServerTransport(
            input = System.`in`.asSource().buffered(),
            output = System.out.asSink().buffered(),
        )

        val session = server.createSession(transport)

        session.onClose {
            cancel()
        }

        awaitCancellation()
    } catch (e: Exception) {
        if (e is CancellationException) {
            ensureActive()
        }
        logger.error(e){"Failed to start MCP server"}
    }
}