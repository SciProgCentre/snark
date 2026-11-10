<#-- LLM generated code: README template for snark-core -->
# Module ${name}

${description}

## Installation
```kotlin
repositories {
    maven("https://repo.kotlin.link")
    mavenCentral()
}

dependencies {
    implementation("${group}:${name}:${version}")
}
```

## Snark plugin

`Snark` is a DataForge `WorkspacePlugin`. It collects readers and text processors registered in the context.

```kotlin
val context = Context {
    plugin(Snark)
}

val snark = context.request(Snark)

val readers = snark.readers               // all SnarkReader instances in the context
val processors = snark.textProcessors     // all TextProcessor instances in the context
val processor = snark.preprocessor(meta)  // find a processor by name (string meta or `name` field)
```

Most users do not use `Snark` directly, but rather `SnarkHtml` from [snark-html](../snark-html), which depends on it.

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

snark-core includes only a raw binary passthrough reader. Format-specific readers (Markdown, HTML, YAML) are in
[snark-html](../snark-html).

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

<#noparse>
Implementations handle patterns like `${resolveRef("path")}` or `${pageMeta.get("title")}`.
</#noparse>

### Registering a TextProcessor

Register via DataForge's provider system:

```kotlin
class MyPlugin : AbstractPlugin() {
    override fun content(target: String): Map<Name, Any> = when (target) {
        SnarkReader::class.dfType -> mapOf("myFormat".asName() to myReader)
        TextProcessor::class.dfType -> mapOf(
            "myProcessor".asName() to TextProcessor { text -> text.toString().replace("{{custom}}", "replaced") }
        )
        else -> emptyMap()
    }
}
```

## Data tree utilities

* `ReWrapAction` changes names and meta of data items without touching the data or its computation state:

  ```kotlin
  val dropExtensions = ReWrapAction<Any> { name, _, _ ->
      name.replaceLast { NameToken(it.body.substringBeforeLast('.')) }
  }
  ```

* `DataTreeWithDefault(primary, defaults)` overlays two trees: items missing in `primary` are taken from `defaults`.
  It is used for language fallbacks.

* `observeNode(prefix, tree)` mounts a tree into a `DataTreeBuilder` and keeps following its updates.
  It takes the coroutine context from the surrounding `ContextAware`.

* `Snark.SNARK_NODE_KEY` (`snark`) marks data nodes with hints and other service information that should not be
  rendered. `Snark.DESIGN_NODE_KEY` (`design`) is the key for design hint data.

## JVM utilities

* `ImageIOReader` reads images into `BufferedImage`.
* `IOPlugin.readResourceDirectory("templates")` reads a classpath resource directory as `DataTree<Binary>`.
* `Snark.workspace(meta)` creates a workspace reading data from directories and resources described in meta:

  ```kotlin
  val workspace = snark.workspace(
      Meta {
          "directory" put Meta {
              "path" put "/path/to/data"
              "name" put "content"
          }
      }
  )
  ```

## See also

* [snark-html](../snark-html) — HTML rendering and site generation
* [DataForge](https://github.com/SciProgCentre/dataforge-core)
