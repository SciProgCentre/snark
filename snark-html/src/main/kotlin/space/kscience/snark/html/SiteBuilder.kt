package space.kscience.snark.html

import kotlinx.html.HTML
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.ContextAware
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.DataTreeItem
import space.kscience.dataforge.data.branch
import space.kscience.dataforge.data.getItem
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.getIndexed
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.parseAsName
import space.kscience.snark.SnarkBuilder
import space.kscience.snark.SnarkContext
import space.kscience.snark.html.SiteLayout.Companion.LAYOUT_KEY


/**
 * An abstraction, which is used to render sites to the different rendering engines
 */
@SnarkBuilder
public interface SiteBuilder : ContextAware, SnarkContext {

    /**
     * Route name of this [SiteBuilder] relative to the site root
     */
    public val route: Name

    /**
     * Data used for site construction. The type of the data is not limited
     */
    public val data: DataTree<*>

    /**
     * Snark plugin and context used for layout resolution, preprocessors, etc
     */
    public val snark: SnarkHtmlPlugin

    override val context: Context get() = snark.context

    /**
     * Site configuration
     */
    public val siteMeta: Meta

    /**
     * Serve a static data as a file from [data] with given [dataName] at given [routeName].
     */
    public fun static(dataName: Name, routeName: Name = dataName)


    /**
     * Create a single page at given [route]. If route is empty, create an index page at current route.
     *
     * @param pageMeta additional page meta. [WebPage] will use both it and [siteMeta]
     */
    @SnarkBuilder
    public fun page(route: Name = Name.EMPTY, pageMeta: Meta = Meta.EMPTY, content: context(HTML, WebPage) () -> Unit)

    /**
     * Create a route with optional data tree override. For example one could use a subtree of the initial tree.
     * By default, the same data tree is used for route.
     */
    public fun route(
        routeName: Name,
        dataOverride: DataTree<*>? = null,
        routeMeta: Meta = Meta.EMPTY,
    ): SiteBuilder

    /**
     * Creates a route and sets it as site base url
     */
    public fun site(
        routeName: Name,
        dataOverride: DataTree<*>? = null,
        routeMeta: Meta = Meta.EMPTY,
    ): SiteBuilder


    public companion object {
        public val SITE_META_KEY: Name = "site".asName()
        public val INDEX_PAGE_TOKEN: NameToken = NameToken("index")
        public val UP_PAGE_TOKEN: NameToken = NameToken("..")
    }
}

context(SiteBuilder)
public val site: SiteBuilder
    get() = this@SiteBuilder

@SnarkBuilder
public inline fun SiteBuilder.route(
    route: Name,
    dataOverride: DataTree<*>? = null,
    routeMeta: Meta = Meta.EMPTY,
    block: SiteBuilder.() -> Unit,
) {
    route(route, dataOverride, routeMeta).apply(block)
}

@SnarkBuilder
public inline fun SiteBuilder.route(
    route: String,
    dataOverride: DataTree<*>? = null,
    routeMeta: Meta = Meta.EMPTY,
    block: SiteBuilder.() -> Unit,
) {
    route(route.parseAsName(), dataOverride, routeMeta).apply(block)
}

@SnarkBuilder
public inline fun SiteBuilder.site(
    route: Name,
    dataOverride: DataTree<*>? = null,
    routeMeta: Meta = Meta.EMPTY,
    block: SiteBuilder.() -> Unit,
) {
    site(route, dataOverride, routeMeta).apply(block)
}

@SnarkBuilder
public inline fun SiteBuilder.site(
    route: String,
    dataOverride: DataTree<*>? = null,
    routeMeta: Meta = Meta.EMPTY,
    block: SiteBuilder.() -> Unit,
) {
    site(route.parseAsName(), dataOverride, routeMeta).apply(block)
}

public inline fun SiteBuilder.withData(
    data: DataTree<*>,
    block: SiteBuilder.() -> Unit
){
    route(Name.EMPTY, data).apply(block)
}

public inline fun SiteBuilder.withDataBranch(
    name: Name,
    block: SiteBuilder.() -> Unit
){
    route(Name.EMPTY, data.branch(name)).apply(block)
}

public inline fun SiteBuilder.withDataBranch(
    name: String,
    block: SiteBuilder.() -> Unit
){
    route(Name.EMPTY, data.branch(name)).apply(block)
}

///**
// * Create a stand-alone site at a given node
// */
//public fun SiteBuilder.site(route: Name, dataRoot: DataTree<*>, block: SiteBuilder.() -> Unit) {
//    val mountedData = data.copy(
//        data = dataRoot,
//        baseUrlPath = data.resolveRef(route.tokens.joinToString(separator = "/")),
//        meta = Laminate(dataRoot.meta, data.meta) //layering dataRoot meta over existing data
//    )
//    route(route) {
//        withData(mountedData).block()
//    }
//}

public fun SiteBuilder.static(dataName: String): Unit = static(dataName.parseAsName())

public fun SiteBuilder.static(dataName: String, routeName: String): Unit = static(
    dataName.parseAsName(),
    routeName.parseAsName()
)

internal fun SiteBuilder.assetsFrom(rootMeta: Meta) {
    rootMeta.getIndexed("file".asName()).forEach { (_, meta) ->
        val webName: String? by meta.string()
        val name by meta.string { error("File path is not provided") }
        val fileName = name.parseAsName()
        static(fileName, webName?.parseAsName() ?: fileName)
    }
}


/**
 * Recursively renders the data items in [data]. If [LAYOUT_KEY] is defined in an item, use it to load
 * layout from the context, otherwise render children nodes as name segments and individual data items using [dataRenderer].
 */
public fun SiteBuilder.pages(
    data: DataTreeItem<*>,
    dataRenderer: DataRenderer = DataRenderer.DEFAULT,
) {
    val layoutMeta = data.meta[LAYOUT_KEY]
    if (layoutMeta != null) {
        //use layout if it is defined
        snark.siteLayout(layoutMeta).render(data)
    } else {
        when (data) {
            is DataTreeItem.Node -> {
                data.tree.items.forEach { (token, item) ->
                    //Don't apply index token
                    if (token == SiteLayout.INDEX_PAGE_TOKEN) {
                        pages(item, dataRenderer)
                    } else if (item is DataTreeItem.Leaf) {
                        dataRenderer(token.asName(), item.data)
                    } else {
                        route(token.asName()) {
                            pages(item, dataRenderer)
                        }
                    }
                }
            }

            is DataTreeItem.Leaf -> {
                dataRenderer(Name.EMPTY, data.data)
            }
        }
        data.meta[SiteLayout.ASSETS_KEY]?.let {
            assetsFrom(it)
        }
    }
    //TODO watch for changes
}

/**
 * Render all pages in a node with given name
 */
public fun SiteBuilder.pages(
    dataPath: Name,
    remotePath: Name = dataPath,
    dataRenderer: DataRenderer = DataRenderer.DEFAULT,
) {
    val item = data.getItem(dataPath) ?: error("No data found by name $dataPath")
    route(remotePath) {
        pages(item, dataRenderer)
    }
}

public fun SiteBuilder.pages(
    dataPath: String,
    remotePath: Name = dataPath.parseAsName(),
    dataRenderer: DataRenderer = DataRenderer.DEFAULT,
) {
    pages(dataPath.parseAsName(), remotePath, dataRenderer = dataRenderer)
}