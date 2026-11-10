<#-- LLM generated code: README template for examples/document -->
# Example document

${description}

## Running

```shell
./gradlew :examples:document:run
```

Then open http://localhost:8080. The root redirects to the `lorem/ipsum` document.

## Article

[data/article](data/article) at http://localhost:8080/article is a scientific article (in Russian) written in
Markdown with LaTeX formulas. Use it as a template for a paper:

```
article/
├── document.yaml            # descriptor: title, authors, design files, counters, order of fragments
├── summary.md               # header data and a summary
├── model/                   # Section 4 = data branch "model"
│   ├── @meta.yaml           # metadata of the branch
│   ├── intro.md             # model.intro: chapter heading and introduction
│   ├── names.md             # model.names: subsection 4.1
│   └── ...
├── architecture/            # Section 5
├── references.md            # numbered reference list
├── figures/                 # images, served at /article/figures/...
└── assets/                  # style.css and script.js, linked by the descriptor
```

<#noparse>
Each Markdown file starts with a front matter block (`title: "..."`) and contains one subsection. Numbers are
generated: `${section(2, "sec-names")}` in headings, `${label("definition", "def-tree")}` for definitions,
figures, tables, listings and equations, `${ref("def-tree")}` for cross-references. Citations use keys of the
reference list: `[${ref("ref-spivey1992")}]`, where `references.md` has `1. ${label("reference", "ref-spivey1992")} …`. Figures show an interactive diagram (Mermaid or a script) and the
original image.
</#noparse>

## How it works

1. `snarkApplication` reads and parses the [data](data) directory.
2. `allDocuments` finds every `document.yaml` or `document.json` descriptor. Each descriptor defines one document:
   * [data/loremIpsum](data/loremIpsum) is served at `/lorem/ipsum` (set by `route` in the descriptor);
   * [data/simple](data/simple) is served at `/simple` (the directory name);
   * [data/article](data/article) is served at `/article`.
3. Fragments listed in the descriptor are rendered in order, and section and figure numbers are generated.
4. KaTeX scripts added to the page header render formulas (`$...$` and `$$` blocks) in the browser.
5. The Mermaid script turns ```` ```mermaid ```` code blocks into diagrams. `click` directives in a diagram add links and tooltips.

See [snark-documents](../../snark-documents) for the descriptor format.
