## Site design process

Site design consists of several stages:

### 1. Site data layout
Site data is loaded from a directory or other source as `DataTree<Binary>`. Then it is parsed to typed nodes like assets, html pages and other site data.
The data could be transformed before or after parsing to adjust what is available to site builder. In multi-site setup, data could be adjusted for each site separately.

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
It loads resources from common, assets, images and files branches into corresponding branches of site trees but keeps site content different.

### 2. Site creation and layout

Site is created by calling `site` method in a corresponding context (Ktor route for dynamic sites or `snark.staticSite` for static).

Full sequence of data loading and site creation:
```kotlin
    val snark = context.request(SnarkHtml)
    val siteData = snark.parseDirectory(dataDirectory, true)
    site(context, siteData, content = spcSite)
```
Specific pages in the site are defined by `content` block. It could contain pages, static resources and sub-sites. See `HtmlSite` builder for reference.
Sub-sites and pages could inherit the site data tree or modify it.

### 3. Page data layout

Page is created by calling `page` function in `HtmlSite` block. It requires three parameters:
* Page route (affects its reference and link)
* Page data tree. Defaults to the site content tree, but could be adjusted. The data not included in the tree is not "visible" to page layout.
* Page content block that defines page layout.

The separation of data tree management and layout is important. It allows separating responsibility. Page layout does not care how data is delivered, and the data tree does not know how it will be rendered. So it is possible to change design without changing data and change the data (and its source) without changing design.

### 4. Page layout

