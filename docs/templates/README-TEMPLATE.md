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

* Get the loaded plugin instance via `context.request(SnarkHtml)`

## SNARK-html

SNARK-HTML module defines tools to work with HTML output format.

${modules}
