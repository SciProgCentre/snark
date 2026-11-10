## MCP server

SNARK can expose site data over the [Model Context Protocol](https://modelcontextprotocol.io), so an AI assistant can
browse the data tree and read design notes while helping to write content or layouts.

### Running

The easiest way is to use the tasks of the [Gradle plugin](../../snark-gradle-plugin):

```shell
./gradlew snarkMcpServer   # streamable HTTP
./gradlew snarkMcpStd      # standard input and output
```

`snarkMcpServer` serves the data directory at `http://localhost:3000/mcp`. To connect Claude Code:

```shell
claude mcp add --transport http snark http://localhost:3000/mcp
```

The stdio variant is started by the assistant itself:

```shell
claude mcp add snark -- ./gradlew -q snarkMcpStd
```

To run the server in your own application:

```kotlin
val snarkHtml = context.request(SnarkHtml)
val data = snarkHtml.io.readDirectory(Path("data"), includeExtensions = true)

// streamable HTTP in Ktor
embeddedServer(CIO, port = 3000) {
    snarkMcp(snarkHtml, data)
}.start(wait = true)

// standard input and output
snarkHtml.mcpStdIo(data).join()
```

`snarkHtml.snarkMcpServer(data)` creates the server without a transport.

### Resources

| URI                       | Content                                                    |
|---------------------------|------------------------------------------------------------|
| `snark://tree`            | The data tree as text, with content types                  |
| `snark://metadata/<name>` | Metadata of a data item as JSON                            |
| `snark://data/<name>`     | Content of a JSON, YAML, Markdown or plain text data item  |

### Tools

| Tool       | Argument | Result                                         |
|------------|----------|------------------------------------------------|
| `readData` | `name`   | Text content of a data item                    |
| `readMeta` | `name`   | Metadata of a data item (also as structured content) |

### Design hints

Put notes for the assistant in `design.md` files anywhere in the data tree:

```
data/
  design.md       # general design rules for the site
  posts/
    design.md     # rules for posts
```

The `design-hint` prompt takes an optional `path` argument and returns hints for that node and all its parents,
starting from the root.
