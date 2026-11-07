package space.kscience.snark.mcp

import io.ktor.server.application.Application
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.io.Binary
import space.kscience.snark.html.SnarkHtml

/**
 * Initializes the Snark server's metadata and capabilities, and sets up resources and prompts based on the provided data tree.
 *
 * This function configures the server with metadata interpretation, design hints, and resources mapped from the `dataTree`.
 * It processes the tree structure to expose metadata and data as resources, supports design hint prompts, and serves the resources via URI paths.
 *
 * @param snark The snark context used to request dependencies and configurations for building the Snark server.
 * @param dataTree A data tree containing nodes that supply metadata and content, which are published as resources and prompts on the server.
 */
public fun Application.snarkMcp(
    snark: SnarkHtml,
    dataTree: DataTree<Binary>
) {
    mcpStreamableHttp {
        snark.snarkMcpServer(dataTree)
    }
}