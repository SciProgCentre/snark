# SNARK

In Lewis Carroll "The hunting of the Snark", the Snark itself is something everybody want to get, but nobody know what it is. It is the same in case of this project, but it also has narrower scope. SNARK could be read as "Scientific Notation And Research works in Kotlin" because it could be used for automatic creation of research papers. But it has other purposes as well.

To sum it up, **SNARK is an automated data transformation tool with the main focus on document and web page generation**. It is based on [DataForge framework](https://github.com/SciProgCentre/dataforge-core).

SNARK **is not a typesetting system** itself, but it could utilize typesetting systems such as Markdown, Latex or Typst to do data transformations.

## Concepts

The SNARK process it the transformation of a data tree. Initial data could include texts, images, static binary or textual data or even active external data subscriptions. The result is usually a tree of documents or a directly served web-site.

**Data** is any kind of content, generated lazily with additional metadata (DataForge Meta).

## Using DataForge context

DataForge module management is based on **Contexts** and **Plugins**. Context is used both as dependency injection system, lifecycle object and API discoverability root for all executions. To use some subsystem, one needs to:

* Create a Context with a Plugin like this:

```kotlin
Context("Optional context name"){
    plugin(SnarkHtml) 
    // Here SnarkHtml is a Plugin factory declared as a companion object to a Plugin itself
}
```

* Get the loaded plugin instance via `val snarkHtml = context.request(SnarkHtml)`

* Use plugin like

```kotlin
val siteData = snarkHtml.readSiteData(context) {
    directory(snark.io, Name.EMPTY, dataDirectory)
}

```

## SNARK-html

SNARK-HTML module defines tools to work with HTML output format. The API root for it is `SnarkHtml` plugin. Its primary function (`parse` action) is to parse raw binary DataTree with objects specific for HTML rendering, assets and metadata. It uses `SnarkReader` and more specifically `SnarkHtmlReader` to parse binary data into formats like `Meta` and `PageFragment`. If `parse` could not recognize the format of the input, it leaves it as (lazy) binary.

### Preprocessing and postprocessing

Snark uses DataForge data tree transformation ideology so there could be any number of data transformation steps both before parsing and after parsing, but there is a key difference: before parsing we work with binaries that could be transformed directly (yet lazily because this is how DataForge works), after parsing we have not a hard data, but a rendering function that could only be transformed by wrapping it in another function (which could be complicated). The raw data transformation before parsing is called preprocessing. It could include both raw binary transformation and metadata transformation. The postprocessing is usually done inside the rendering function produced by parser or created directly from code.

The interface for `PageFragment` looks like this: 

```kotlin
public fun interface PageFragment {
    context(PageContextWithData, FlowContent) public fun renderFragment()
}
```

It takes a reference to parsed data tree and rendering context of the page as well as HTML mounting root and provides action to render HTML. The reason for such complication is that some things are not known before the actual page rendering happens. For example, absolute links in HTML could be resolved only when the page is rendered on specific REST request that contain information about host and port. Another example is providing automatic counters for chapters, formulas and images in document rendering. The order is not known until all fragments are placed in correct order.

Postprocessors are functions that transform fragments of HTML wrapped in them according to data tree and page rendering context.

Other details on HTML rendering could be found in [snark-html](./snark-html) module



### [examples](examples)
>
> **Maturity**: EXPERIMENTAL

### [snark-core](snark-core)
>
> **Maturity**: EXPERIMENTAL

### [snark-gradle-plugin](snark-gradle-plugin)
>
> **Maturity**: EXPERIMENTAL

### [snark-html](snark-html)
>
> **Maturity**: EXPERIMENTAL
>
> **Features:**
> - [data](snark-html/#) : Data-based processing. Instead of traditional layout-based
> - [layouts](snark-html/#) : Use custom layouts to represent a data tree
> - [parsers](snark-html/#) : Add custom file formats and parsers using DataForge dependency injection
> - [preprocessor](snark-html/#) : Preprocessing text files using templates
> - [metadata](snark-html/#) : Trademark DataForge metadata layering and transformations
> - [dynamic](snark-html/#) : Generating dynamic site using KTor server
> - [static](snark-html/#) : Generating static site


### [snark-ktor](snark-ktor)
>
> **Maturity**: EXPERIMENTAL

### [snark-pandoc](snark-pandoc)
>
> **Maturity**: EXPERIMENTAL

### [examples/document](examples/document)
>
> **Maturity**: EXPERIMENTAL

