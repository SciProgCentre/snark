## Links and postprocessing

Absolute links depend on where the site is rendered: the output directory for a static site or the request host for
a Ktor server. So links are resolved while a page is rendered.

### Links in code

Inside a page:

| Function                | Result                                                                                  |
|-------------------------|-----------------------------------------------------------------------------------------|
| `resolveRef(path)`      | Link to a file, relative to the site path                                               |
| `resolvePageRef(name)`  | Link to a page                                                                          |
| `resolveSmartRef(ref)`  | Keeps `http…` and `mailto:` links, resolves `#anchor` on the current page, otherwise a page |
| `resolveAssetRef(path)` | Link to a file shared between [language versions](languages.md)                         |
| `page.homeRef`          | Link to the site root                                                                   |

All functions take an optional target site to link to pages of another site.

### Links in data

Text rendered inside a `postprocess { }` block is processed by `WebPageTextProcessor`, which expands these
expressions:

| Expression                   | Result                         |
|------------------------------|--------------------------------|
| `${homeRef}`                 | Site root link                 |
| `${resolveRef("path")}`      | File link                      |
| `${resolveAssetRef("path")}` | Language-independent file link |
| `${resolvePageRef("name")}`  | Page link                      |
| `${pageMeta.get("key")}`     | Page meta value                |
| `${siteMeta.get("key")}`     | Site meta value                |

In `href` and `src` attributes, `snark://` URIs are resolved as well:

| URI                    | Result          |
|------------------------|-----------------|
| `snark://homeRef`      | Site root link  |
| `snark://ref/<path>`   | File link       |
| `snark://page/<name>`  | Page link       |
| `snark://pageMeta/<key>` | Page meta value |
| `snark://siteMeta/<key>` | Site meta value |

Markdown links using `${homeRef}`, `${resolveRef("...")}` and `${resolvePageRef("...")}` are converted to `snark://`
URIs when the file is parsed:

```markdown
See the [first post](${resolvePageRef("posts.first")}) and ![logo](${resolveRef("assets/logo.png")}).
```

```kotlin
body {
    postprocess {
        fragment(data.resolveHtml("home"))
    }
}
```

Fragments rendered outside `postprocess` are left as is.

### Custom postprocessing

`postprocess` accepts any `TextProcessor`. It is applied to text content and to `href` and `src` attributes:

```kotlin
postprocess(TextProcessor { text -> text.toString().replace("{{year}}", "2026") }) {
    fragment(data.resolveHtml("footer"))
}
```

[snark-documents](../../snark-documents) uses this to number sections and figures.
