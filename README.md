# SNARK

In Lewis Carroll's "The Hunting of the Snark", the Snark is something everybody wants to get, but nobody knows what
it is. The same is true for this project, but it has a narrower scope. SNARK could be read as "Scientific Notation And
Research works in Kotlin" because it can be used for automatic creation of research papers. But it has other purposes
as well.

To sum it up, **SNARK is an automated data transformation tool with the main focus on document and web page
generation**. It is based on the [DataForge framework](https://github.com/SciProgCentre/dataforge-core).

SNARK **is not a typesetting system** itself, but it can use typesetting systems such as Markdown, LaTeX or Typst
to do data transformations.

## Concepts

A SNARK process is a transformation of a data tree. Initial data can include texts, images, static binary or textual
data, or even active external data subscriptions. The result is usually a tree of documents or a site served directly.

**Data** is any kind of content, computed lazily, with additional metadata (DataForge `Meta`).

A typical pipeline:

1. **Read** raw data into a `DataTree<Binary>`, for example from a directory.
2. **Preprocess** (optional): transform raw text or metadata, for example with templates.
3. **Parse** known formats into page fragments (Markdown, HTML) or metadata (YAML, JSON).
4. **Render** a site layout that places fragments on pages.
5. **Postprocess**: resolve links and numbering that are known only during rendering.

Data layout and page layout are independent: the same layout works with different data and vice versa.

## Quick start

```kotlin
suspend fun main() {
    val context = Context { plugin(SnarkHtml) }
    val snarkHtml = context.request(SnarkHtml)

    val siteData = snarkHtml.parseDirectory(Path("data"))

    snarkHtml.staticSite(siteData, Path("build/site")) {
        static("assets")
        page("index") {
            head { title("My site") }
            body { fragment(data.resolveHtml("home")) }
        }
    }
}
```

This renders `data/home.md` into `build/site/index.html` and copies `data/assets`.
To serve the same site with Ktor, see [snark-ktor](snark-ktor).

SNARK subsystems are DataForge plugins. A `Context` is used for dependency injection, lifecycle and discovery of
readers and processors: load a plugin with `plugin(SnarkHtml)` and get it with `context.request(SnarkHtml)`.

## Guides

* [Site design](snark-html/docs/site-design.md) — data layout, page layout, static and dynamic rendering
* [Data](snark-html/docs/data.md) — reading and parsing data, metadata, custom formats
* [Links](snark-html/docs/links.md) — link resolution and postprocessing
* [Languages](snark-html/docs/languages.md) — multi-language sites
* [Documents](snark-documents) — scientific documents with automatic numbering
* [Serving with Ktor](snark-ktor) and the [MCP server](snark-ktor/docs/mcp.md)
* [Gradle plugin](snark-gradle-plugin) — distribution, MCP task and deployment

## Examples

* [examples/document](examples/document) — a Ktor server that renders scientific documents from a data directory.

## Modules

### [snark-core](snark-core)
> Core SNARK abstractions: readers, text processors and data tree utilities
>
> **Maturity**: EXPERIMENTAL

### [snark-documents](snark-documents)
> Scientific documents built from data trees with automatic numbering
>
> **Maturity**: EXPERIMENTAL

### [snark-gradle-plugin](snark-gradle-plugin)
> Gradle plugin for SNARK-based sites
>
> **Maturity**: EXPERIMENTAL

### [snark-html](snark-html)
> HTML rendering and static site generation from data trees
>
> **Maturity**: EXPERIMENTAL
>
> **Features:**
> - [data](snark-html/docs/data.md) : Data-based processing instead of traditional layout-based
> - [layouts](snark-html/docs/site-design.md) : Use custom layouts to represent a data tree
> - [parsers](snark-html/docs/data.md#custom-formats) : Add custom file formats and parsers using DataForge dependency injection
> - [preprocessor](snark-html/docs/data.md#preprocessing) : Preprocessing text files using templates
> - [metadata](snark-html/docs/data.md#metadata) : DataForge metadata layering and transformations
> - [links](snark-html/docs/links.md) : Link resolution at render time
> - [languages](snark-html/docs/languages.md) : Multi-language sites
> - [dynamic](snark-html/../snark-ktor) : Generating dynamic site using Ktor server
> - [static](snark-html/docs/site-design.md#5-rendering-targets) : Generating static site


### [snark-ktor](snark-ktor)
> Serving SNARK sites with Ktor and exposing site data via an MCP server
>
> **Maturity**: EXPERIMENTAL

### [snark-pandoc](snark-pandoc)
> Document conversion with Pandoc
>
> **Maturity**: EXPERIMENTAL

### [examples/document](examples/document)
> A Ktor application rendering scientific documents from a data directory
>
> **Maturity**: EXPERIMENTAL

