package space.kscience.snark.html

import kotlinx.html.HTML
import space.kscience.dataforge.data.*
import space.kscience.dataforge.io.Binary
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.workspace.Workspace
import space.kscience.snark.SnarkBuilder
import space.kscience.snark.SnarkContext


/**
 * An abstraction, which is used to render sites to the different rendering engines
 */
@SnarkBuilder
public interface SiteContext : SnarkContext {

    /**
     * Route name of this [SiteContext] relative to the site root
     */
    public val route: Name

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
    public suspend fun static(route: Name, data: Data<Binary>)


    /**
     * Create a single page at given [route]. If route is empty, create an index page at current route.
     *
     * @param pageMeta additional page meta. [PageContext] will use both it and [siteMeta]
     */
    @SnarkBuilder
    public suspend fun page(
        route: Name,
        data: DataSet<Any>,
        pageMeta: Meta = Meta.EMPTY,
        htmlPage: HtmlPage,
    )


    /**
     * Creates a sub-site and sets it as site base url
     * @param route mount site at [rootName]
     * @param dataPrefix prefix path for data used in this site
     */
    @SnarkBuilder
    public suspend fun site(
        route: Name,
        data: DataSet<Any>,
        siteMeta: Meta = Meta.EMPTY,
        htmlSite: HtmlSite,
    )


    public companion object {
        public val SITE_META_KEY: Name = "site".asName()
        public val INDEX_PAGE_TOKEN: NameToken = NameToken("index")
        public val UP_PAGE_TOKEN: NameToken = NameToken("..")
    }
}

@SnarkBuilder
public suspend fun SiteContext.page(
    route: Name,
    data: DataSet<Any>,
    pageMeta: Meta = Meta.EMPTY,
    htmlPage: HTML.(page: PageContext, data: DataSet<Any>) -> Unit,
): Unit = page(route, data, pageMeta, HtmlPage(htmlPage))

context(SiteContext)
public val site: SiteContext
    get() = this@SiteContext


public suspend fun SiteContext.renderPages(data: DataSet<Any>): Unit {

    // Render all sub-sites
    data.filterByType<HtmlSite>().forEach { siteData: NamedData<HtmlSite> ->
        // generate a sub-site context and render the data in sub-site context
        val dataPrefix = siteData.meta["site.dataPath"].string?.asName() ?: Name.EMPTY
        site(
            route = siteData.meta["site.route"].string?.asName() ?: siteData.name,
            data.branch(dataPrefix),
            siteMeta = siteData.meta,
            siteData.await()
        )
    }

    // Render all stand-alone pages in default site
    data.filterByType<HtmlPage>().forEach { pageData: NamedData<HtmlPage> ->
        val dataPrefix = pageData.meta["page.dataPath"].string?.asName() ?: Name.EMPTY
        page(
            route = pageData.meta["page.route"].string?.asName() ?: pageData.name,
            data.branch(dataPrefix),
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