## Site data

### Reading data

```kotlin
val snarkHtml = context.request(SnarkHtml)

// pass observe = true to follow file changes
val siteData = snarkHtml.parseDirectory(Path("data"))
```

`parseDataTree` parses any existing `DataTree<Binary>`, and `buildDataTree { ... }` parses a tree built in code.

### Names

Directory paths become data names: `data/posts/first.md` becomes `posts.first`.

* Parsed items lose their file extension (`first.md` becomes `first`). Unparsed binary items keep it
  (`logo.png` stays `logo.png`), so they can be served as files.
* An `index` item represents its parent node: `posts/index.md` is available as `posts`.

### Metadata

Each data item has metadata. For files, it can be set in a front matter block:

```markdown
---
title: First post
order: 1
---
# Hello
```

Metadata fields used by SNARK:

| Field          | Meaning                                                                     |
|----------------|-----------------------------------------------------------------------------|
| `contentType`  | Overrides the content type guessed from the file name                       |
| `preprocessor` | Name of a text processor to apply before parsing                            |
| `published`    | `false` hides a fragment from `resolveHtml` and `resolveAllHtml`             |
| `order`, `id`  | Available as `Data.order` and `Data.id` for sorting and anchors             |
| `type`         | Used by `findHtmlByContentType` to select fragments                         |
| `language`     | Language of the item in [multi-language sites](languages.md)                |

Pages and sites stored in data (see `renderPages`) also use `page.route`, `page.dataPath`, `site.route` and
`site.dataPath`.

### Parsing

The content type of each item is taken from `contentType` metadata or guessed from the file name. The reader with
the highest priority for that type parses the item. Built-in readers:

| Content type                      | Result         |
|-----------------------------------|----------------|
| `text/markdown`, `md`, `markdown` | `PageFragment` |
| `text/html`, `html`               | `PageFragment` |
| `application/json`                | `Meta`         |
| `text/yaml`, `yaml`               | `Meta`         |

Items without a reader stay `Binary`.

### Preprocessing

Raw text can be transformed before parsing. Register a `TextProcessor` in the context and set its name in the
`preprocessor` field:

```markdown
---
preprocessor: myProcessor
---
```

### Custom formats

Readers and text processors are discovered through DataForge plugins. Readers that produce HTML implement
`SnarkHtmlReader`:

```kotlin
object PlainTextReader : SnarkHtmlReader {
    override val inputContentTypes: Set<String> = setOf("text/plain", "txt")
    override fun readFrom(source: String): PageFragment = PageFragment { pre { +source } }
    override fun readFrom(source: Source): PageFragment = readFrom(source.readString())
}

class MyFormats : AbstractPlugin() {
    override val tag: PluginTag get() = Companion.tag

    override fun content(target: String): Map<Name, Any> = when (target) {
        SnarkReader::class.dfType -> mapOf("text".asName() to PlainTextReader)
        else -> emptyMap()
    }

    companion object : PluginFactory<MyFormats> {
        override val tag: PluginTag = PluginTag("my.formats")
        override fun build(context: Context, meta: Meta): MyFormats = MyFormats()
    }
}

val context = Context {
    plugin(SnarkHtml)
    plugin(MyFormats)
}
```

Readers producing other types are created with `SnarkReader(ioReader, "content/type")`
(see [snark-core](../../snark-core)).
