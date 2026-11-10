<#-- LLM generated code: README template for snark-documents -->
# Module ${name}

${description}

<#if published>
## Usage

${artifact}
</#if>

## Overview

A document is a directory in the data tree that contains a descriptor file named `document` (`document.yaml`,
`document.json` or any other format parsed to `Meta`). The descriptor lists fragments in the order they appear.
Each document is rendered as a single HTML page. Other binary files in the directory, such as images, stylesheets
and scripts, are served next to it under the same relative path: `figures/plot.png` is served at
`<route>/figures/plot.png`.

Subdirectories group fragments: `chapter/intro.md` is the data item `chapter.intro`. A `@meta.yaml` file in a
subdirectory holds the metadata of that branch.

The API is in the `space.kscience.snark.html.document` package.

## Descriptor

```yaml
route: lorem.ipsum          # page route, the directory name by default
title: Lorem Ipsum
authors:
  - name: Alexander Nozik
    affiliation: SPC
stylesheets:                # linked in <head>, relative to the document directory
  - assets/style.css
scripts:                    # loaded in <head> with defer
  - assets/script.js
documentMeta:               # available to templates as documentMeta
  metaValue: Hello world!
fragments:
  - type: image
    ref: SPC-logo.png
    meta:
      caption: SPC logo
  - type: data
    name: chapter1          # chapter1.md in the same directory
  - type: data
    name: part2.methods     # part2/methods.md
```

Fragment types:

| Type     | Fields              | Result                                              |
|----------|---------------------|-----------------------------------------------------|
| `data`   | `name`              | A data item from the document directory             |
| `image`  | `ref`, meta `caption`, `alt` | A figure with an optional caption          |
| `list`   | `fragments`         | A nested list of fragments                          |
| `markup` | `text`              | Not rendered yet                                    |
| `layout` | `fragments`         | Not implemented yet                                 |

A `data` item must be a parsed page fragment, for example a Markdown or HTML file.

## Rendering documents

Render all documents found in the site data:

```kotlin
HtmlSite {
    allDocuments(headers = {
        // extra <head> content, for example KaTeX scripts
    })
}
```

Or a single document with an explicit descriptor:

```kotlin
HtmlSite {
    document("papers.first".parseAsName(), descriptor)
}
```

See [examples/document](../examples/document) for a complete Ktor application.

## Templates

<#noparse>
Document text is processed with [FreeMarker](https://freemarker.apache.org/) after rendering. Available values:

| Expression                         | Result                                                          |
|------------------------------------|-----------------------------------------------------------------|
| `${documentName}`                  | Document route                                                  |
| `${section(level)}`, `${section(level, "key")}` | Next number of the section level, continuous through the document |
| `${label("counter")}`, `${label("counter", "key")}` | Next value of a named counter, with anchor     |
| `${ref("key")}`                    | Link to a labelled section or item, showing its number          |
| `${documentRef("path")}`           | Link to a file in the document directory                        |
| `${resolveRef("path")}`, `${resolvePageRef("name")}`, `${homeRef}` | Site links, as in [snark-html](../snark-html/docs/links.md) |
| `${documentMeta.key}`, `${documentMeta.get('key')}` | Value from `documentMeta`                      |

```markdown
## ${section(1, "sec-methods")} Methods

### ${section(2)} Data

**Definition ${label("definition", "def-tree")}.** A content tree is $D = (\delta, \beta)$.

By definition ${ref("def-tree")}, see section ${ref("sec-methods")} and figure ${ref("fig-plot")}.

<figure>
<img src="${documentRef("figures/plot.png")}" alt="Plot">
<figcaption>Figure ${label("figure", "fig-plot")}. A plot.</figcaption>
</figure>
```
</#noparse>

A key becomes the anchor of the item, so `${ref(...)}` can point to items defined later in the document. Numbering is
continuous through the document: each counter (and each section level) starts from `1` and is never reset. A reference
is filled in after the document is rendered, so the document is not rendered twice.
Text inside code blocks and inline code is not processed.

In an equation, put the label into `\tag{}`: `\tag{${label("equation", "eq-key")}}`.

The generated HTML uses `snark-figure`, `snark-image`, `snark-figure-caption`, `snark-label`, `snark-section` and
`snark-ref` CSS classes, as well as `title`, `author`, `author-name` and `author-affiliation`.

## See also

* [snark-html](../snark-html) — parsing data and building sites
