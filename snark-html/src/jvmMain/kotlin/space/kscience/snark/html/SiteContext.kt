package space.kscience.snark.html

import space.kscience.dataforge.data.*
import space.kscience.dataforge.io.Binary
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.*
import space.kscience.snark.SnarkBuilder
import space.kscience.snark.SnarkContext
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf


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
    public fun static(route: Name, data: Data<Binary>)


    /**
     * Create a single page at given [route]. If the route is empty, create an index page the current route.
     *
     * @param pageMeta additional page meta. [PageContext] will use both it and [siteMeta]
     */
    @SnarkBuilder
    public fun page(
        route: Name,
        data: DataSet<*>,
        pageMeta: Meta = Meta.EMPTY,
        content: HtmlPage,
    )

    /**
     * Create a route block with its own data. Does not change base url
     */
    @SnarkBuilder
    public fun route(
        route: Name,
        data: DataSet<*>,
        siteMeta: Meta = Meta.EMPTY,
        content: HtmlSite,
    )

    /**
     * Creates a sub-site and sets it as site base url
     * @param route mount site at [rootName]
     */
    @SnarkBuilder
    public fun site(
        route: Name,
        data: DataSet<*>,
        siteMeta: Meta = Meta.EMPTY,
        content: HtmlSite,
    )


    public companion object {
        public val SITE_META_KEY: Name = "site".asName()
        public val INDEX_PAGE_TOKEN: NameToken = NameToken("index")
        public val UP_PAGE_TOKEN: NameToken = NameToken("..")
    }
}

public fun SiteContext.static(dataSet: DataSet<Binary>, prefix: Name = Name.EMPTY) {
    dataSet.forEach { (name, data) ->
        static(prefix + name, data)
    }
}


public fun SiteContext.static(dataSet: DataSet<*>, branch: String, prefix: String = branch) {
    val branchName = branch.parseAsName()
    val prefixName = prefix.parseAsName()
    val binaryType = typeOf<Binary>()
    dataSet.forEach { (name, data) ->
        @Suppress("UNCHECKED_CAST")
        if (name.startsWith(branchName) && data.type.isSubtypeOf(binaryType)) {
            static(prefixName + name, data as Data<Binary>)
        }
    }
}



context(SiteContext)
public val site: SiteContext
    get() = this@SiteContext


/**
 * A wrapper for site context that allows convenient site building experience
 */
public class SiteContextWithData(private val site: SiteContext, public val siteData: DataSet<*>) : SiteContext by site


@SnarkBuilder
public fun SiteContextWithData.static(branch: String, prefix: String = branch): Unit = static(siteData, branch, prefix)


@SnarkBuilder
public fun SiteContextWithData.page(
    route: Name = Name.EMPTY,
    pageMeta: Meta = Meta.EMPTY,
    content: HtmlPage,
): Unit = page(route, siteData, pageMeta, content)

@SnarkBuilder
public fun SiteContextWithData.route(
    route: String,
    data: DataSet<*> = siteData,
    siteMeta: Meta = Meta.EMPTY,
    content: HtmlSite,
): Unit = route(route.parseAsName(), data, siteMeta,content)

@SnarkBuilder
public fun SiteContextWithData.site(
    route: String,
    data: DataSet<*> = siteData,
    siteMeta: Meta = Meta.EMPTY,
    content: HtmlSite,
): Unit = site(route.parseAsName(), data, siteMeta,content)

/**
 * Render all pages and sites found in the data
 */
public suspend fun SiteContext.renderPages(data: DataSet<*>): Unit {

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