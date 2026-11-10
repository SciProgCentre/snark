# Module snark-pandoc

Document conversion with Pandoc

## Usage

Run [Pandoc](https://pandoc.org) with options defined by `PandocCommandBuilder`:

```kotlin
Pandoc.execute {
    addInputFile(Path("paper.md"))
    outputFile(Path("paper.pdf"))
    standalone()
    numberSections()
    bibliography(Path("references.bib"))
    citationProcessing()
}
```

The builder has a function for most Pandoc command line options, named after the option: `formatFrom`, `formatTo`,
`setTemplate`, `metadata`, `luaFilter`, `pdfEngine`, `referenceDoc`, `kaTex`, and others. Options without a function
can be added to `options` directly:

```kotlin
Pandoc.execute {
    addInputFile(Path("paper.md"))
    outputFile(Path("paper.docx"))
    options += "--toc"
}
```

`execute` throws an exception if Pandoc exits with an error. Use `redirectOutput` and `redirectError` to save Pandoc
output to files, for example for `getVersion()` or `getOutputFormats()`.

## Pandoc installation

If `pandoc` is not available on the system, the latest release is downloaded from GitHub to `./pandoc` on first use.
Set `pandocExecutablePath` to change the location. Windows, macOS and Linux (x86-64 and ARM) are supported.
