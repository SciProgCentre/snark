package space.kscience.snark.html

import kotlinx.html.HTML
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.ContextAware
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.DataTreeItem
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.parseAsName
import space.kscience.snark.SnarkContext
import java.nio.file.Path
import kotlin.reflect.KType
import kotlin.reflect.typeOf


/**
 * An abstraction, which is used to render sites to the different rendering engines
 */
public interface SiteBuilder : ContextAware, SnarkContext {

    public val data: DataTree<*>

    public val snark: SnarkPlugin

    override val context: Context get() = snark.context

    public val siteMeta: Meta

    public fun assetFile(remotePath: String, file: Path)

    public fun assetDirectory(remotePath: String, directory: Path)

    public fun assetResourceFile(remotePath: String, resourcesPath: String)

    public fun assetResourceDirectory(resourcesPath: String)

    public fun page(route: Name = Name.EMPTY, content: context(Page, HTML) () -> Unit)

    /**
     * Create a route with optional data tree override. For example one could use a subtree of the initial tree.
     * By default, the same data tree is used for route
     */
    public fun route(
        routeName: Name,
        dataOverride: DataTree<*>? = null,
        metaOverride: Meta? = null,
        setAsRoot: Boolean = false,
    ): SiteBuilder

    public companion object {
        public val INDEX_PAGE_TOKEN: NameToken = NameToken("index")
        public val UP_PAGE_TOKEN: NameToken = NameToken("..")
    }
}

context(SiteBuilder) public val siteBuilder: SiteBuilder get() = this@SiteBuilder

public inline fun SiteBuilder.route(
    route: Name,
    dataOverride: DataTree<*>? = null,
    metaOverride: Meta? = null,
    setAsRoot: Boolean = false,
    block: SiteBuilder.() -> Unit,
) {
    route(route, dataOverride, metaOverride, setAsRoot).apply(block)
}

public inline fun SiteBuilder.route(
    route: String,
    dataOverride: DataTree<*>? = null,
    metaOverride: Meta? = null,
    setAsRoot: Boolean = false,
    block: SiteBuilder.() -> Unit,
) {
    route(route.parseAsName(), dataOverride, metaOverride, setAsRoot).apply(block)
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

//TODO move to DF
public fun DataTree.Companion.empty(meta: Meta = Meta.EMPTY): DataTree<Any> = object : DataTree<Any> {
    override val items: Map<NameToken, DataTreeItem<Any>> get() = emptyMap()
    override val dataType: KType get() = typeOf<Any>()
    override val meta: Meta get() = meta
}