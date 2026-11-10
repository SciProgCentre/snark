## Site design process

Site design consists of several stages. Data layout and page layout are separated: page layout does not care how data
is delivered, and the data tree does not know how it will be rendered. So it is possible to change the design without
changing the data, and change the data (or its source) without changing the design.

### 1. Site data layout

Site data is loaded from a directory or other source as `DataTree<Binary>`. Then it is parsed to typed nodes like
assets, HTML fragments and metadata (see [data](data.md)).
The data could be transformed before or after parsing to adjust what is available to the site builder. In a multi-site
setup, data could be adjusted for each site separately.

An example of data transformation for a specific site:

```kotlin
context(contextAware: ContextAware)
internal inline fun <reified T> DataTree<T>.contentFor(branchName: String): DataTree<T> = DataTree {
    observeNode(Name.EMPTY, branch(Name.of(branchName, "content")) ?: branch(branchName) ?: DataTree.EMPTY)
    observeNode("common", branch("common") ?: DataTree.EMPTY)
    observeNode("assets", branch(Name.of(branchName, "assets")) ?: DataTree.EMPTY)
    observeNode("images", branch(Name.of(branchName, "images")) ?: DataTree.EMPTY)
    observeNode("files", branch(Name.of(branchName, "files")) ?: DataTree.EMPTY)
}
```

It loads resources from `common`, `assets`, `images` and `files` branches into corresponding branches of the site tree,
but keeps site content different.

### 2. Site creation and layout

A site is created by calling `site` in a corresponding context: `Route.site` for dynamic sites served by
[Ktor](../../snark-ktor) or `snarkHtml.staticSite` for static ones.

```kotlin
val snarkHtml = context.request(SnarkHtml)
val siteData = snarkHtml.parseDirectory(dataDirectory)

// static
snarkHtml.staticSite(siteData, outputPath, content = mySite)

// dynamic, inside Ktor routing
site(context, siteData, content = mySite)
```

The pages of the site are defined by the `content` block (`HtmlSite`). It can contain pages, static resources and
sub-sites. Sub-sites and pages inherit the site data tree or replace it.

### 3. Page data layout

A page is created by calling `page` in an `HtmlSite` block. It takes:

* a page route (affects its reference and link);
* a page data tree. It defaults to the site data tree, but could be adjusted. Data not included in the tree is not
  visible to the page layout;
* a page content block that defines the page layout.

### 4. Page layout

A page layout is a [kotlinx.html](https://github.com/Kotlin/kotlinx.html) builder that has access to the page context:
data, metadata and link resolution. Common parts of a layout are ordinary Kotlin functions:

```kotlin
context(page: PageContextWithData)
fun HTML.defaultLayout(pageTitle: String, content: FlowContent.() -> Unit) {
    head {
        title(pageTitle)
        link(rel = "stylesheet", href = resolveRef("assets/main.css"))
    }
    body {
        nav { a(href = page.homeRef) { +"Home" } }
        main {
            postprocess { content() }
        }
    }
}
```

Pages can be generated from data. For example, a page for each Markdown file in the `posts` branch:

```kotlin
val mySite = HtmlSite {
    static("assets")

    page("index") {
        defaultLayout("Home") { fragment(data.resolveHtml("home")) }
    }

    siteData.resolveAllHtml { name, _ -> name.startsWith("posts".parseAsName()) }.forEach { (name, post) ->
        page(name) {
            defaultLayout(post.meta["title"].string ?: name.toString()) { fragment(post) }
        }
    }
}
```

Fragments are rendered with `fragment(...)`. Wrapping them in `postprocess` resolves links written in the data
(see [links](links.md)).

### 5. Rendering targets

The same `HtmlSite` can be rendered in two ways:

| Target  | Function               | Links                                    | Data updates            |
|---------|------------------------|------------------------------------------|-------------------------|
| Static  | `SnarkHtml.staticSite` | Based on `siteUri` (output directory URI by default) | Rendered once |
| Dynamic | `Route.site` in [snark-ktor](../../snark-ktor) | Based on the request host | Rendered on each request |

Static pages are written as `<route>.html`; the page with an empty route or `index` is written as `index.html`.
Snark uses kotlinx-html as a template engine for page rendering. The page template is defined by `HtmlPage` functional interface:

```kotlin
public fun interface HtmlPage {
    context(page: PageContextWithData)
    public fun HTML.renderPage()
}
```

It uses HTML tag as a root element and has contextual access to page data and page context. Page context allows the following actions:
* Page metadata with `pageMeta` property.
* Page route name with `pageRoute` property.
* Resolve link reference on page site: `resolveRef(ref: String, targetSite: SiteContext = site)`. Could reference other sites on the same server if `targetSite` is provided.
* Resolve page reference by its name: `resolvePageRef(pageName: Name, targetSite: SiteContext = site)`. Uses absolute page name to resolve a link path for this page.
* Resolve a relative page reference by its name: `resolveRelativePageRef(pageName: Name)`. Uses relative page name to resolve a link path for this page.
* Extract actual data from lazy `Data`: **TODO**
