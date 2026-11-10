# Module snark-ktor

Serving SNARK sites with Ktor and exposing site data via an MCP server

## Usage

## Artifact:

The Maven coordinates of this project are `space.kscience:snark-ktor:0.3.0-dev-3`.

**Gradle Kotlin DSL:**
```kotlin
repositories {
    maven("https://repo.kotlin.link")
    mavenCentral()
}

dependencies {
    implementation("space.kscience:snark-ktor:0.3.0-dev-3")
}
```

## Serving a site

`snarkApplication` reads and parses a data directory and serves an `HtmlSite` built with
[snark-html](../snark-html). Pages are rendered on each request, so links use the request host.

```kotlin
fun Application.module() {
    snarkApplication(
        site = HtmlSite {
            static("assets")
            page("index") { /* ... */ }
        }
    )
}

fun main() {
    embeddedServer(CIO, port = 8080, module = Application::module).start(wait = true)
}
```

`snarkApplication` also:

* serves classpath resources from the `css` directory at `/css`;
* serves an overview of the data tree at `/@data`, which helps to find data names.

Set the data directory in the application config. If it is not set, `data` in the working directory is used:

```yaml
snark:
  dataDirectory: /var/lib/my-site/data
```

Pass `contextBuilder` to load additional plugins, for example custom readers.

### Custom setup

To prepare data in code or mount several sites, use `Route.site` inside Ktor routing:

```kotlin
routing {
    site(context, siteData) {
        page("index") { /* ... */ }
    }
}
```

Use `snarkHtml.parseDirectory(path, observe = true)` to show data changes without restarting the server.

Pages rendered by Ktor have `host`, `path` and `route` values in their metadata.

## MCP server

`Application.snarkMcp(snarkHtml, data)` exposes a data tree to AI assistants over the Model Context Protocol via
streamable HTTP. `snarkHtml.mcpStdIo(data)` does the same over standard input and output. See [MCP](docs/mcp.md).
