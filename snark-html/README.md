# Module snark-html

HTML rendering and site generation module for SNARK.

## Overview

snark-html provides tools for transforming data trees into HTML output — either as static sites or dynamic Ktor-served pages. It builds on snark-core and adds HTML-specific readers, page/site contexts, and rendering infrastructure.

### Source Structure


| File / Package                          | Description |
|-----------------------------------------|-------------|
| **SnarkHtml.kt**                        | Main plugin, parse action, content type detection |
| **SnarkHtmlReader.kt**                  | Interface for readers that output PageFragment |
| **MarkdownReader.kt**                   | Markdown → PageFragment via intellij-markdown |
| **PageFragment.kt**                     | Renderable HTML fragment **interface** |
| **PageContext.kt**                      | Context for building a single page |
| **SiteContext.kt**                      | Context for building sites and routes |
| **HtmlPage.kt**                         | Full HTML page **interface** |
| **HtmlSite.kt**                         | Site rendering **interface** |
| **Postprocessor.kt**                    | WebPageTextProcessor, template variable expansion |
| **MetaMaskData.kt**                     | Data wrapper with overridden meta |
| **document/**                           | **Scientific document building** |
| DocumentBuilder.kt                      | - |
| DocumentDescriptor.kt                   | - |
| DocumentFragment.kt                     | - |
| FtlDocumentProcessor.kt                 | - |
| RegexDocumentProcessor.kt               | - |
| **language/**                           | **Multi-language support** |
| Language.kt                             | - |
| **static/**                             | **Static site generation** |
| StaticSiteContext.kt                    | - |


## Installation

```kotlin
repositories {
    maven("https://repo.kotlin.link")
    mavenCentral()
}

dependencies {
    implementation("space.kscience:snark-html:0.2.0-dev-1")
}
```

## SnarkHtml Plugin

`SnarkHtml` is the main entry point. It extends `WorkspacePlugin` and provides parsing actions.

```kotlin
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.request
import space.kscience.snark.html.SnarkHtml

val context = Context {
    plugin(SnarkHtml)
}

val snarkHtml = context.request(SnarkHtml)
```

### Built-in Readers

SnarkHtml registers these readers:

| Name | Content Types | Output |
| --- | --- | --- |
| `html` | `text/html`, `html` | `PageFragment` |
| `markdown` | `text/markdown`, `md`, `markdown` | `PageFragment` |
| `json` | `application/json` | `Meta` |
| `yaml` | `text/yaml`, `yaml` | `Meta` |

### Parse Action

The `parseAction` transforms binary data into typed objects based on content type:

```kotlin
val binaries: DataTree<Binary> = snarkHtml.io.readDirectory(path)

// Parse binaries into PageFragment, Meta, or leave as Binary
val parsed: DataTree<Any> = with(snarkHtml) {
    parseDataTree(binaries)
}
```

Content type is determined by:

1. `contentType` metadata field
2. File path extension
3. `URLConnection.guessContentTypeFromName()`

## PageFragment

A renderable HTML fragment. This is the core output type for content.

```kotlin
fun interface PageFragment {
    context(page: PageContextWithData) 
    fun FlowContent.renderFragment()
}
```

### Rendering Fragments

```kotlin
context(page: PageContextWithData)
fun FlowContent.fragment(fragment: PageFragment)

context(page: PageContextWithData)
fun FlowContent.fragment(data: Data<PageFragment>)
```

### Fragment Utilities

```kotlin
context(snark: SnarkContext)
val Data<*>.id: String           // meta["id"] or hash-based
val Data<*>.order: Int?          // meta["order"]
val Data<*>.published: Boolean   // meta["published"] != "false"

// Resolve fragments from DataTree
fun DataTree<*>.resolveHtmlOrNull(name: Name): Data<PageFragment>?
fun DataTree<*>.resolveHtml(name: String): Data<PageFragment>
fun DataTree<*>.resolveAllHtml(predicate: (Name, Meta?) -> Boolean): Map<Name, Data<PageFragment>>
fun DataTree<*>.findHtmlByContentType(contentType: String, baseName: Name = Name.EMPTY): Map<Name, Data<PageFragment>>
```

## PageContext

Context for rendering a single page. Provides URL resolution and metadata access.

```kotlin
interface PageContext : SnarkContext, ContextAware {
    val site: SiteContext
    val host: Url
    val pageMeta: Meta
    val pageRoute: Name
    
    fun resolveRef(ref: String, targetSite: SiteContext = site): String
    fun resolvePageRef(pageName: Name, targetSite: SiteContext = site): String
}
```

### PageContextWithData

Wraps PageContext with access to the page's data tree:

```kotlin
class PageContextWithData(
    private val pageContext: PageContext,
    val data: DataTree<*>
) : PageContext by pageContext
```

### Context Accessors

Use context receivers for clean access:

```kotlin
context(pageContext: PageContext)
val homeRef: String get() = resolvePageRef(Name.EMPTY)

context(pageContext: PageContext)
fun resolveRef(ref: String): String = pageContext.resolveRef(ref)

context(pageContext: PageContextWithData)
val pageMeta: Meta get() = pageContext.pageMeta

context(pageContext: PageContextWithData)
val data: DataTree<*> get() = pageContext.data
```

## SiteContext

Abstraction for rendering sites to different backends (static files, Ktor server).

```kotlin
interface SiteContext : SnarkContext, ContextAware {
    val parent: SiteContext?
    val path: List<String>
    val siteRoute: Name
    val siteMeta: Meta
    
    fun static(route: Name, data: Data<Binary>)
    fun page(route: Name, data: DataTree<*>?, pageMeta: Meta, content: HtmlPage)
    fun route(route: Name, data: DataTree<*>?, siteMeta: Meta, content: HtmlSite)
    fun site(route: Name, data: DataTree<*>?, siteMeta: Meta, content: HtmlSite)
}
```

### SiteContextWithData

```kotlin
class SiteContextWithData(
    val site: SiteContext, 
    val siteData: DataTree<*>
) : SiteContext by site
```

### Site Building DSL

```kotlin
context(siteContext: SiteContextWithData)
fun page(route: Name = Name.EMPTY, pageMeta: Meta = Meta.EMPTY, content: HtmlPage)

context(siteContext: SiteContextWithData)
fun route(route: String, data: DataTree<*>? = siteData, siteMeta: Meta = Meta.EMPTY, content: HtmlSite)

context(siteContext: SiteContextWithData)
fun site(route: String, data: DataTree<*>? = siteData, siteMeta: Meta = Meta.EMPTY, content: HtmlSite)

context(site: SiteContext)
fun static(dataSet: DataTree<Binary>, prefix: Name = Name.EMPTY)

context(site: SiteContext)
fun static(dataSet: DataTree<*>, branch: String, prefix: String = branch)
```

### Rendering Pages from Data

```kotlin
context(siteContext: SiteContextWithData)
suspend fun renderPages(data: DataTree<*>)
```

Automatically renders `HtmlSite` and `HtmlPage` items found in the data tree.

## HtmlPage

Interface for full HTML page rendering:

```kotlin
fun interface HtmlPage {
    context(page: PageContextWithData)
    fun HTML.renderPage()
}
```

### Creating Pages

```kotlin
suspend fun DataSink<Any>.page(
    name: Name,
    pageMeta: Meta = Meta.EMPTY,
    block: context(PageContextWithData) HTML.() -> Unit
)
```

### Rendering to String

```kotlin
val html: String = HtmlPage.createHtmlString(pageContext, dataSet, page)
```

## HtmlSite

Interface for site rendering:

```kotlin
fun interface HtmlSite {
    context(siteContext: SiteContextWithData)
    fun renderSite()
}
```

## MarkdownReader

Parses Markdown to PageFragment using JetBrains' intellij-markdown library.

```kotlin
object MarkdownReader : SnarkHtmlReader {
    override val inputContentTypes = setOf("text/markdown", "md", "markdown")
    
    override fun readFrom(source: String): PageFragment
    override fun readFrom(source: Source): PageFragment
}
```

### Snark-aware Link Processing

MarkdownReader handles special link formats:

| Pattern | Resolved to |
| --- | --- |
| `${homeRef}` | Site home URL |
| `${resolveRef("path")}` | Resolved asset URL |
| `${resolvePageRef("name")}` | Resolved page URL |
| `${pageMeta.get("key")}` | Page metadata value |

Links like `snark://ref/path` and `snark://page/name` are also processed.

## Postprocessor (WebPageTextProcessor)

Processes template expressions in content:

```kotlin
class WebPageTextProcessor(private val page: PageContext) : TextProcessor {
    override fun process(text: CharSequence): String
}
```

### Supported Expressions

| Expression | Result |
| --- | --- |
| `${homeRef}` | Home page URL |
| `${resolveRef("path")}` | Asset URL |
| `${resolvePageRef("name")}` | Page URL |
| `${pageMeta.get("key")}` | Page meta value |
| `${siteMeta.get("key")}` | Site meta value |

### Using Postprocessor

Wrap content in postprocessed block:

```kotlin
context(page: PageContext)
fun FlowContent.postprocess(
    processor: TextProcessor = WebPageTextProcessor(page),
    block: context(PageContext) FlowContent.() -> Unit
)
```

## Static Site Generation

Generate static HTML files:

```kotlin
import space.kscience.snark.html.static.staticSite

suspend fun SnarkHtml.staticSite(
    data: DataTree<*>?,
    outputPath: Path,
    siteUrl: Url = Url(outputPath.absolutePathString()),
    siteMeta: Meta = data?.meta ?: Meta.EMPTY,
    content: HtmlSite
)
```

### Example

```kotlin
val snarkHtml = context.request(SnarkHtml)

with(snarkHtml) {
    val data = parseDataTree(io.readDirectory(Path("content")))
    
    staticSite(data, Path("build/site")) {
        // Static assets
        static("assets")
        
        // Pages
        page("index") {
            head { title("My Site") }
            body {
                h1 { +"Welcome" }
                data.resolveAllHtml { name, _ -> true }.forEach { (name, fragment) ->
                    fragment(fragment)
                }
            }
        }
    }
}
```

## Document Building

For structured scientific documents with sections, figures, and cross-references.

### DocumentFragment Types

```kotlin
sealed interface DocumentFragment {
    val meta: Meta
}

class MarkupDocumentFragment(val text: String, override val meta: Meta)
class ImageDocumentFragment(val ref: String, override val meta: Meta)
class DataDocumentFragment(val name: Name, override val meta: Meta)
class ListDocumentFragment(val fragments: List<DocumentFragment>, override val meta: Meta)
class LayoutDocumentFragment(val fragments: Map<String, DocumentFragment>, override val meta: Meta)
```

### DocumentDescriptor

Describes document structure via YAML/JSON:

```kotlin
class DocumentDescriptor : Scheme() {
    var route: String?
    var title: String?
    var documentMeta: Meta?
    var authors: List<Author>
    var fragments: List<DocumentFragment>
}
```

Example `document.yaml`:

```yaml
route: lorem.ipsum
title: Lorem Ipsum
authors:
  - name: Alexander Nozik
    affiliation: SPC
fragments:
  - type: image
    ref: SPC-logo.png
    meta:
      caption: SPC logo
  - type: data
    name: chapter1
  - type: data
    name: chapter2
```

### Building Documents

```kotlin
context(site: SiteContextWithData)
fun document(
    dataName: Name,
    descriptor: DocumentDescriptor = DocumentDescriptor.empty(),
    route: Name = dataName,
    headers: MetaDataContent.() -> Unit = {},
    documentBlock: DocumentBuilder.() -> Unit = {}
)

// Auto-discover all documents
context(site: SiteContextWithData)
fun allDocuments(headers: MetaDataContent.() -> Unit = {})
```

### FreeMarker Processing

`FtlDocumentProcessor` uses FreeMarker templates for document content:

```markdown
## Chapter ${section(1)}

This is document ${documentName}.

Figure ${label(image)} shows the result.
```

Available template functions:

* `${documentName}` — Document route
* `${section(level)}` — Auto-numbered section
* `${label(counter)}` — Auto-numbered label with anchor
* `${documentMeta.get("key")}` — Document metadata

## Multi-Language Support

### Language Configuration

```kotlin
class Language : Scheme() {
    var key: String           // "en", "ru", etc.
    var dataPath: Name        // Where language-specific content lives
    var route: Name           // URL prefix for this language
}
```

### Multi-Language Sites

```kotlin
context(site: SiteContextWithData)
fun multiLanguageSite(
    defaultLanguage: Language,
    vararg languages: Language,
    content: HtmlSite
)
```

Example:

```kotlin
multiLanguageSite(
    Language("en"),
    Language("ru", route = "ru".parseAsName())
) {
    page("index") {
        // Content automatically selected by language
    }
}
```

### Localized Strings

```kotlin
interface LocalizedString {
    context(page: PageContext)
    fun value(): String
}

// Fixed string
LocalizedString.fixed("Hello")

// By language map
LocalizedString.byLanguage("en" to "Hello", "ru" to "Привет")

// From page resources
LocalizedString.byResource("greeting".parseAsName(), defaultValue = "Hello")
```

### Language Context

```kotlin
context(page: PageContext)
val language: String                    // Current page language
val languageMap: Map<String, Meta>      // Available translations

fun PageContext.localisedPageRef(pageName: Name): String
```


## See Also

* [snark-core](../snark-core/README.md) — Core interfaces and plugin
* [snark-ktor](../snark-ktor/README.md) — Ktor server integration
* [snark-pandoc](../snark-pandoc/README.md) — PDF/DOCX via Pandoc
* [examples/document](../examples/document) — Document builder example