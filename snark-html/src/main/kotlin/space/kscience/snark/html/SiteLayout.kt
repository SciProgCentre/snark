package space.kscience.snark.html

import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.title
import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.DataTreeItem
import space.kscience.dataforge.data.getItem
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.getIndexed
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.misc.Type
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.parseAsName
import space.kscience.snark.html.SiteLayout.Companion.ASSETS_KEY
import space.kscience.snark.html.SiteLayout.Companion.INDEX_PAGE_TOKEN
import space.kscience.snark.html.SiteLayout.Companion.LAYOUT_KEY
import java.nio.file.Path
import kotlin.reflect.typeOf

internal fun SiteBuilder.assetsFrom(rootMeta: Meta) {
    rootMeta.getIndexed("resource".asName()).forEach { (_, meta) ->

        val path by meta.string()
        val remotePath by meta.string()

        path?.let { resourcePath ->
            //If remote path provided, use a single resource
            remotePath?.let {
                resourceFile(it, resourcePath)
                return@forEach
            }

            //otherwise use package resources
            resourceDirectory(resourcePath)
        }
    }

    rootMeta.getIndexed("file".asName()).forEach { (_, meta) ->
        val remotePath by meta.string { error("File remote path is not provided") }
        val path by meta.string { error("File path is not provided") }
        file(Path.of(path), remotePath)
    }

    rootMeta.getIndexed("directory".asName()).forEach { (_, meta) ->
        val path by meta.string { error("Directory path is not provided") }
        file(Path.of(path), "")
    }
}

/**
 * Render (or don't) given data piece
 */
public typealias DataRenderer = SiteBuilder.(name: Name, data: Data<Any>) -> Unit

/**
 * Recursively renders the data items in [data]. If [LAYOUT_KEY] is defined in an item, use it to load
 * layout from the context, otherwise render children nodes as name segments and individual data items using [dataRenderer].
 */
public fun SiteBuilder.pages(
    data: DataTreeItem<*>,
    dataRenderer: DataRenderer = SiteLayout.defaultDataRenderer,
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
                    if (token == INDEX_PAGE_TOKEN) {
                        pages(item, dataRenderer)
                    } else if (item is DataTreeItem.Leaf) {
                        dataRenderer(this, token.asName(), item.data)
                    } else {
                        route(token.asName()) {
                            pages(item, dataRenderer)
                        }
                    }
                }
            }
            is DataTreeItem.Leaf -> {
                dataRenderer.invoke(this, Name.EMPTY, data.data)
            }
        }
        data.meta[ASSETS_KEY]?.let {
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
    dataRenderer: DataRenderer = SiteLayout.defaultDataRenderer,
) {
    val item = data.getItem(dataPath) ?: error("No data found by name $dataPath")
    route(remotePath) {
        pages(item, dataRenderer)
    }
}

public fun SiteBuilder.pages(
    dataPath: String,
    remotePath: Name = dataPath.parseAsName(),
    dataRenderer: DataRenderer = SiteLayout.defaultDataRenderer,
) {
    pages(dataPath.parseAsName(), remotePath, dataRenderer = dataRenderer)
}

/**
 * An abstraction to render singular data or a data tree.
 */
@Type(SiteLayout.TYPE)
public fun interface SiteLayout {

    context(SiteBuilder) public fun render(item: DataTreeItem<*>)

    public companion object {
        public const val TYPE: String = "snark.layout"
        public const val LAYOUT_KEY: String = "layout"
        public const val ASSETS_KEY: String = "assets"
        public val INDEX_PAGE_TOKEN: NameToken = NameToken("index")

        public val defaultDataRenderer: SiteBuilder.(name: Name, data: Data<*>) -> Unit = { name: Name, data: Data<*> ->
            if (data.type == typeOf<HtmlData>()) {
                page(name) {
                    head {
                        title = data.meta["title"].string ?: "Untitled page"
                    }
                    body {
                        @Suppress("UNCHECKED_CAST")
                        htmlData(data as HtmlData)
                    }
                }
            }
        }
    }
}

/**
 * The default [SiteLayout]. It renders all [HtmlData] pages as t with simple headers via [SiteLayout.defaultDataRenderer]
 */
public object DefaultSiteLayout : SiteLayout {
    context(SiteBuilder) override fun render(item: DataTreeItem<*>) {
        pages(item)
    }
}