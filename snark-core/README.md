# Module snark-core

Core module of SNARK providing fundamental abstractions for document processing.

## Overview

snark-core provides the foundational types and interfaces for SNARK's data transformation pipeline:

| File | Description |
| --- | --- |
| `Snark.kt` | Main plugin — entry point for SNARK operations |
| `SnarkReader.kt` | Interface for parsing content formats |
| `SnarkContext.kt` | Marker interface and DSL annotations |
| `TextProcessor.kt` | Interface for text transformations (templating, link replacement) |
| `ReWrapAction.kt` | Action to modify metadata without changing data |
| `DataTreeWithDefault.kt` | DataTree wrapper with fallback values |
| `ImageIOReader.kt` | JVM image reader via ImageIO |
| `snarkWorkspace.kt` | JVM workspace utilities |

## Installation

```kotlin
repositories {
    maven("https://repo.kotlin.link")
    mavenCentral()
}

dependencies {
    implementation("space.kscience:snark-core:0.2.0-dev-1")
}
```

## Snark Plugin

`Snark` is a DataForge `WorkspacePlugin`. It manages readers and text processors through DataForge's context system.

```kotlin
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.request
import space.kscience.snark.Snark

val context = Context {
    plugin(Snark)
}

val snark = context.request(Snark)
```

### Accessing Readers

Readers are gathered from the context by type:

```kotlin
// All registered readers
val readers: Map<Name, SnarkReader<Any>> = snark.readers

// Find reader for specific content type
val markdownReader = readers.values.find { "markdown" in it.inputContentTypes }
```

### Accessing Text Processors

Text processors handle templating and link replacement:

```kotlin
val processors: Map<Name, TextProcessor> = snark.textProcessors

// Get processor from meta
val processor = snark.preprocessor(transformationMeta)
```

## SnarkReader

Interface for parsing binary content into typed objects.

```kotlin
@DfType(DF_TYPE)
interface SnarkReader<out T> : IOReader<T> {
    val outputType: KType
    val inputContentTypes: Set<String>
    val priority: Int
    fun readFrom(source: String): T
}
```

### Creating a Reader

Wrap any `IOReader` with content type metadata:

```kotlin
import space.kscience.snark.SnarkReader
import space.kscience.dataforge.io.IOReader

// From an existing IOReader
val myReader: SnarkReader<MyType> = SnarkReader(
    reader = myIOReader,
    "text/x-myformat", "myformat"  // content types
)

// With explicit type and priority
val priorityReader = SnarkReader<MyType>(
    reader = myIOReader,
    outputType = typeOf<MyType>(),
    "text/x-myformat",
    priority = 20  // higher = preferred (default is 10)
)
```

### Built-in Reader

snark-core includes only `byteArraySnarkParser` for raw binary passthrough. Format-specific readers (Markdown, HTML, YAML) are in `snark-html`.

## TextProcessor

Interface for text transformations like template expansion and link resolution.

```kotlin
@DfType(DF_TYPE)
fun interface TextProcessor {
    fun process(text: CharSequence): String

    companion object {
        const val DF_TYPE: String = "snark.textTransformation"
        val TEXT_TRANSFORMATION_KEY: NameToken = NameToken("transformation")
        val TEXT_PREPROCESSOR_KEY: NameToken = NameToken("preprocessor")
    }
}
```

Implementations handle patterns like `${resolveRef("path")}` or `${pageMeta.get("title")}`.

### Registering a TextProcessor

Register via DataForge's provider system:

```kotlin
class MyPlugin : AbstractPlugin() {
    override fun content(target: String): Map<Name, Any> = when (target) {
        TextProcessor::class.dfType -> mapOf(
            "myProcessor".asName() to TextProcessor { text ->
                text.replace("{{custom}}", "replaced")
            }
        )
        else -> emptyMap()
    }
}
```

## ReWrapAction

An action that modifies document names and metadata without touching the data itself.

```kotlin
import space.kscience.snark.ReWrapAction

// Rename files and add metadata
val action = ReWrapAction<Any>(
    type = typeOf<Any>(),
    newMeta = { name ->
        // Add content type to meta
        set("contentType", guessContentType(name))
    },
    newName = { name, meta, type ->
        // Remove file extension from name
        name.replaceLast { token ->
            NameToken(token.body.substringBeforeLast('.'))
        }
    }
)

// Apply to a DataTree
val processed = action.execute(sourceTree, Meta.EMPTY, context)
```

## DataTreeWithDefault

A `DataTree` wrapper that overlays one tree on another, using the second as fallback.

```kotlin
import space.kscience.snark.DataTreeWithDefault

val primary: DataTree<String> = // ... user content
val defaults: DataTree<String> = // ... default templates

val merged = DataTreeWithDefault(primary, defaults)

// Accessing merged["key"] returns:
// - primary["key"] if it exists
// - defaults["key"] otherwise
```

Used for language fallbacks and template inheritance.

## SnarkContext

Marker interface for SNARK DSL builders:

```kotlin
@SnarkBuilder
interface SnarkContext
```

The `@SnarkBuilder` annotation enables DSL scope control.

### DataTree Observation

Extension functions for observing DataTree updates within a context:

```kotlin
context(contextAware: ContextAware)
fun <T> DataTreeBuilder<T>.observeNode(prefix: Name, tree: DataTree<T>)

context(contextAware: ContextAware)
fun <T> DataTreeBuilder<T>.observeNode(prefix: String, tree: DataTree<T>)
```

## JVM Utilities

### ImageIOReader

Reads images using Java's ImageIO:

```kotlin
import space.kscience.snark.ImageIOReader
import java.awt.image.BufferedImage

val image: BufferedImage = ImageIOReader.readFrom(source)
```

### Resource Reading

Read directories from classpath resources:

```kotlin
import space.kscience.snark.readResourceDirectory

val snark: Snark = context.request(Snark)

// Read resource directory as DataTree<Binary>
val resources: DataTree<Binary> = snark.io.readResourceDirectory(
    "templates",
    classLoader = MyClass::class.java.classLoader
)
```

### Workspace Builder

Create a SNARK workspace with data sources:

```kotlin
import space.kscience.snark.workspace

val workspace = snark.workspace(
    meta = Meta {
        "directory" put Meta {
            "path" put "/path/to/data"
            "name" put "content"
        }
        "resource" put Meta {
            "path" put listOf("templates", "assets")
        }
    }
) {
    // Additional workspace configuration
}
```



## See Also

* [snark-html](../snark-html/README.md) — HTML rendering and site generation
* [snark-ktor](../snark-ktor/README.md) — Ktor server integration
* [DataForge documentation](https://github.com/SciProgCentre/dataforge-core)