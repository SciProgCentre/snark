# Example document

A Ktor application rendering scientific documents from a data directory

## Running

```shell
./gradlew :examples:document:run
```

Then open http://localhost:8080. The root redirects to the `lorem/ipsum` document.

## How it works

1. `snarkApplication` reads and parses the [data](data) directory.
2. `allDocuments` finds every `document.yaml` or `document.json` descriptor. Each descriptor defines one document:
   * [data/loremIpsum](data/loremIpsum) is served at `/lorem/ipsum` (set by `route` in the descriptor);
   * [data/simple](data/simple) is served at `/simple` (the directory name).
3. Fragments listed in the descriptor are rendered in order, and section and figure numbers are generated.
4. KaTeX scripts added to the page header render formulas in the browser.

See [snark-documents](../../snark-documents) for the descriptor format.
