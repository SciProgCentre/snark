package space.kscience.snark.html

import space.kscience.dataforge.context.ContextAware
import space.kscience.dataforge.data.*
import space.kscience.dataforge.io.Binary
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.parseAsName
import space.kscience.dataforge.names.plus
import space.kscience.dataforge.workspace.filterByType
import space.kscience.snark.SnarkBuilder
import space.kscience.snark.SnarkContext


/**
 * An abstraction, which is used to render sites to the different rendering engines
 */
@SnarkBuilder
public interface SiteContext : SnarkContext, ContextAware {

    public val parent: SiteContext?

    /**
     * A context path segments for this site
     */
    public val path: List<String>

    /**
     * Route name of this [SiteContext] relative to the site root
     */
    public val siteRoute: Name

    /**
     * Site configuration
     */
    public val siteMeta: Meta

    /**
     * Renders a static file or resource for the given route and data.
     *
     * @param route The route name of the static file relative to the site root.
     * @param data The data object containing the binary data for the static file.
     */
    public fun static(route: Name, data: Data<Binary>)


    /**
     * Create a single page at given [route]. If the route is empty, create an index page the current route.
     *
     * @param pageMeta additional page meta. [PageContext] will use both it and [siteMeta]
     */
    public fun page(
        route: Name,
        data: DataTree<*>?,
        pageMeta: Meta = Meta.EMPTY,
        content: HtmlPage,
    )

    /**
     * Create a route block with its own data. Does not change the context path
     */
    public fun route(
        route: Name,
        data: DataTree<*>?,
        siteMeta: Meta = Meta.EMPTY,
        content: HtmlSite,
    )

    /**
     * Creates a sub-site and changes context path to match [route]
     * @param route mount site at [rootName]
     */
    public fun site(
        route: Name,
        data: DataTree<*>?,
        siteMeta: Meta = Meta.EMPTY,
        content: HtmlSite,
    )


    public companion object {
        public val SITE_META_KEY: Name = Name.of("site")
        public val INDEX_PAGE_TOKEN: NameToken = NameToken("index")
        public val UP_PAGE_TOKEN: NameToken = NameToken("..")
    }
}

context(site: SiteContext)
public fun static(dataSet: DataTree<Binary>, prefix: Name = Name.EMPTY) {
    dataSet.forEach {
        site.static(prefix + it.name, it)
    }
}

/**
 * Copies binary data from the specified data set branch and renders it as static files in the given site context.
 *
 * @param dataSet The data tree containing data that could be rendered as static files.
 * @param branch The branch name of the dataset to filter binary data from.
 * @param prefix The prefix to prepend to the route for the static files. Defaults to the branch name.
 */
context(site: SiteContext)
public fun static(dataSet: DataTree<*>, branch: String, prefix: String = branch) {
    val branchName = branch.parseAsName()
    val prefixName = prefix.parseAsName()
    dataSet.branch(branchName)?.filterByType<Binary>()?.forEach {
        site.static(prefixName + it.name, it)
    }
}

/**
 * Provides access to the current `SiteContext` within the specific rendering scope.
 * Acts as an abstraction used to render sites to different rendering engines.
 *
 * The `siteContext` can be used to access various properties and methods for managing
 * site paths, routes, metadata, and content rendering. It encapsulates the rendering
 * environment and contextual data necessary for dynamically generating and structuring
 * web content.
 */
context(site: SiteContext)
public val siteContext: SiteContext get() = site

context(site: SiteContextWithData)
public val siteContextWithData: SiteContextWithData get() = site

context(site: SiteContextWithData)
public val siteData: DataTree<*> get() = site.siteData

/**
 * A wrapper for site context that allows convenient site building experience
 */
public class SiteContextWithData(public val site: SiteContext, public val siteData: DataTree<*>) : SiteContext by site

context(site: SiteContextWithData)
public fun page(
    route: Name,
    data: DataTree<*>?,
    pageMeta: Meta = Meta.EMPTY,
    content: HtmlPage,
): Unit = siteContext.page(route, data, pageMeta, content)

context(site: SiteContextWithData)
public fun static(
    branch: String,
    prefix: String = branch
): Unit = static(site.siteData, branch, prefix)


context(siteContext: SiteContextWithData)
public fun page(
    route: Name = Name.EMPTY,
    pageMeta: Meta = Meta.EMPTY,
    content: HtmlPage,
): Unit = siteContext.page(route, siteData, pageMeta, content)

context(siteContext: SiteContextWithData)
public fun page(
    route: String,
    pageMeta: Meta = Meta.EMPTY,
    content: HtmlPage,
): Unit = siteContext.page(route.parseAsName(), siteData, pageMeta, content)

context(siteContext: SiteContextWithData)
public fun route(
    route: Name,
    data: DataTree<*>? = siteData,
    siteMeta: Meta = Meta.EMPTY,
    content: HtmlSite,
): Unit = siteContext.route(route, data, siteMeta, content)

context(siteContext: SiteContextWithData)
public fun route(
    route: String,
    data: DataTree<*>? = siteData,
    siteMeta: Meta = Meta.EMPTY,
    content: HtmlSite,
): Unit = siteContext.route(route.parseAsName(), data, siteMeta, content)

context(siteContext: SiteContextWithData)
public fun site(
    route: String,
    data: DataTree<*>? = siteData,
    siteMeta: Meta = Meta.EMPTY,
    content: HtmlSite,
): Unit = siteContext.site(route.parseAsName(), data, siteMeta, content)

/**
 * Render all pages and sites found in the data
 */
context(siteContext: SiteContextWithData)
public suspend fun renderPages(data: DataTree<*>): Unit {

    // Render all sub-sites
    data.filterByType<HtmlSite>().forEach { siteData: NamedData<HtmlSite> ->
        // generate a sub-site context and render the data in sub-site context
        val dataPrefix = siteData.meta["site.dataPath"].string?.let { Name.of(it) } ?: Name.EMPTY
        siteContext.site(
            route = siteData.meta["site.route"].string?.let { Name.of(it) } ?: siteData.name,
            data.branch(dataPrefix) ?: DataTree.EMPTY,
            siteMeta = siteData.meta,
            siteData.await()
        )
    }

    // Render all stand-alone pages in default site
    data.filterByType<HtmlPage>().forEach { pageData: NamedData<HtmlPage> ->
        val dataPrefix = pageData.meta["page.dataPath"].string?.let { Name.of(it) } ?: Name.EMPTY
        siteContext.page(
            route = pageData.meta["page.route"].string?.let { Name.of(it) } ?: pageData.name,
            data.branch(dataPrefix) ?: DataTree.EMPTY,
            pageMeta = pageData.meta,
            pageData.await()
        )
    }
}


//
///**
// * Recursively renders the data items in [data]. If [LAYOUT_KEY] is defined in an item, use it to load
// * layout from the context, otherwise render children nodes as name segments and individual data items using [dataRenderer].
// */
//public fun SiteContext.pages(
//    dataRenderer: DataRenderer = DataRenderer.DEFAULT,
//) {
//    val layoutMeta = siteData().meta[LAYOUT_KEY]
//    if (layoutMeta != null) {
//        //use layout if it is defined
//        snark.siteLayout(layoutMeta).render(siteData())
//    } else {
//        when (siteData()) {
//            is DataTreeItem.Node -> {
//                siteData().tree.items.forEach { (token, item) ->
//                    //Don't apply index token
//                    if (token == SiteLayout.INDEX_PAGE_TOKEN) {
//                        pages(item, dataRenderer)
//                    } else if (item is DataTreeItem.Leaf) {
//                        dataRenderer(token.asName(), item.data)
//                    } else {
//                        route(token.asName()) {
//                            pages(item, dataRenderer)
//                        }
//                    }
//                }
//            }
//
//            is DataTreeItem.Leaf -> {
//                dataRenderer(Name.EMPTY, siteData().data)
//            }
//        }
//        siteData().meta[SiteLayout.ASSETS_KEY]?.let {
//            assetsFrom(it)
//        }
//    }
//    //TODO watch for changes
//}
//
///**
// * Render all pages in a node with given name
// */
//public fun SiteContext.pages(
//    dataPath: Name,
//    remotePath: Name = dataPath,
//    dataRenderer: DataRenderer = DataRenderer.DEFAULT,
//) {
//    val item = resolveData.getItem(dataPath) ?: error("No data found by name $dataPath")
//    route(remotePath) {
//        pages(item, dataRenderer)
//    }
//}
//
//public fun SiteContext.pages(
//    dataPath: String,
//    remotePath: Name = dataPath.parseAsName(),
//    dataRenderer: DataRenderer = DataRenderer.DEFAULT,
//) {
//    pages(dataPath.parseAsName(), remotePath, dataRenderer = dataRenderer)
//}