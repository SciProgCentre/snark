# Changelog

## Unreleased

### Added

- `snark-documents`: named labels `${label("counter", "key")}` and `${section(level, "key")}`, and cross-references
  `${ref("key")}` that also resolve forward references (filled in after the document is rendered).
- `snark-documents`: `stylesheets` and `scripts` descriptor fields, and
  `${documentRef("path")}`, `${resolveRef("path")}`, `${resolvePageRef("name")}`, `${homeRef}` in document templates.
- `snark-html`: `postprocess` takes a separate `linkProcessor` for `snark:` links in `href` and `src`.
- `examples/document`: a scientific article (`data/article`, Sections 4–5 of the SNARK paper) written in Markdown with
  LaTeX formulas, one file per subsection, generated numbering, a research-blog design and interactive figures.

### Changed

- `snark-documents`: binary files in subdirectories of a document are served under their relative path
  (`figures/plot.png` at `<route>/figures/plot.png`) instead of the file name only.
- `snark-html`: postprocessing escapes text before processing, so a text processor may return HTML. Text inside
  `<code>` is not processed.
- `snark-html`: `${...}` expressions in Markdown are kept as literal text, so math or emphasis syntax does not break them.

### Fixed

- `MarkdownReader` did not register its Markdown extensions, so math, tables and `snark:` links were not parsed.
- `MarkdownReader` used the link text as the link destination, so every Markdown link pointed to its own text.
- `MarkdownReader` did not rewrite `${resolveRef(...)}` in image destinations.
- `${section()}` and `${label()}` in Markdown text were rendered as escaped HTML.
- `examples/document`: KaTeX now renders Markdown math, which is emitted without `$` delimiters.

### Known issues

- `allDocuments` registers Ktor routes from parallel coroutines; `KtorSiteContext.page` can throw
  `ConcurrentModificationException`, and a random document is missing after start.
- Front matter values with `:` must be quoted (the YAML parser rejects them otherwise).
