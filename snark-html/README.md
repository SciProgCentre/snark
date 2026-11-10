# Module snark-html

HTML rendering and static site generation from data trees

## Features

 - [data](docs/data.md) : Data-based processing instead of traditional layout-based
 - [layouts](docs/site-design.md) : Use custom layouts to represent a data tree
 - [parsers](docs/data.md#custom-formats) : Add custom file formats and parsers using DataForge dependency injection
 - [preprocessor](docs/data.md#preprocessing) : Preprocessing text files using templates
 - [metadata](docs/data.md#metadata) : DataForge metadata layering and transformations
 - [links](docs/links.md) : Link resolution at render time
 - [languages](docs/languages.md) : Multi-language sites
 - [dynamic](../snark-ktor) : Generating dynamic site using Ktor server
 - [static](docs/site-design.md#5-rendering-targets) : Generating static site


## Installation

```kotlin
repositories {
    maven("https://repo.kotlin.link")
    mavenCentral()
}

dependencies {
    implementation("space.kscience:snark-html:0.3.0-dev-3")
}
```

## Overview

snark-html turns a data tree into HTML pages:

1. **Read** raw data as `DataTree<Binary>`, for example from a directory.
2. **Parse** it with `SnarkHtml`: Markdown and HTML become `PageFragment`, YAML and JSON become `Meta`, other files
   stay binary.
3. **Render** a site: an `HtmlSite` block declares pages, sub-sites and static files. The same block can be written
   to a static directory or served by [snark-ktor](../snark-ktor).

```kotlin
suspend fun main() {
    val context = Context { plugin(SnarkHtml) }
    val snarkHtml = context.request(SnarkHtml)
    val siteData = snarkHtml.parseDirectory(Path("data"))

    snarkHtml.staticSite(siteData, Path("build/site")) {
        static("assets")                             // copy data/assets
        page("index") {                              // write index.html
            head { title("My site") }
            body {
                postprocess {                        // resolve links written in data
                    fragment(data.resolveHtml("home"))   // render data/home.md
                }
            }
        }
    }
}
```

Inside an `HtmlSite` block, `page`, `route`, `site` and `static` declare pages, route prefixes, sub-sites and files.
Inside a page, `data` and `pageMeta` give access to the page data, and functions like `resolveHtml`, `resolveRef`
and `resolvePageRef` find fragments and build links.

## Guides

* [Site design](docs/site-design.md) — data layout, page layout, static and dynamic rendering
* [Data](docs/data.md) — reading and parsing data, names, metadata, preprocessing, custom formats
* [Links](docs/links.md) — link resolution and postprocessing
* [Languages](docs/languages.md) — multi-language sites

## See also

* [snark-core](../snark-core) — readers and text processors
* [snark-documents](../snark-documents) — scientific documents built from data
* [snark-ktor](../snark-ktor) — serving sites with Ktor
