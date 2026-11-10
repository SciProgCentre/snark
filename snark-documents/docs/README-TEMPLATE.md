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
Each document is rendered as a single HTML page. Other binary files in the directory, such as images, are served
next to it.

The API is in the `space.kscience.snark.html.document` package.

## Descriptor

```yaml
route: lorem.ipsum          # page route, the directory name by default
title: Lorem Ipsum
authors:
  - name: Alexander Nozik
    affiliation: SPC
documentMeta:               # available to templates as documentMeta
  metaValue: Hello world!
fragments:
  - type: image
    ref: SPC-logo.png
    meta:
      caption: SPC logo
  - type: data
    name: chapter1          # chapter1.md in the same directory
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

| Expression                    | Result                                              |
|-------------------------------|-----------------------------------------------------|
| `${documentName}`             | Document route                                      |
| `${section(level)}`           | Next section number of the given level, with anchor |
| `${label("counter")}`         | Next value of a named counter, with anchor          |
| `${documentMeta.key}`, `${documentMeta.get('key')}` | Value from `documentMeta`     |

```markdown
## Chapter ${section(1)}

See figure ${label("figure")}.
```
</#noparse>

Counters start from `1`.

The generated HTML uses `snark-figure`, `snark-image`, `snark-figure-caption`, `snark-label` and `snark-section`
CSS classes, as well as `title`, `author`, `author-name` and `author-affiliation`.

## See also

* [snark-html](../snark-html) — parsing data and building sites
