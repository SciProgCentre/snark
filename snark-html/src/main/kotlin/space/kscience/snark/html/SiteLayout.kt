package space.kscience.snark.html

import space.kscience.dataforge.data.DataTreeItem
import space.kscience.dataforge.misc.Type
import space.kscience.dataforge.names.NameToken

/**
 * An abstraction to render singular data or a data tree.
 */
@Type(SiteLayout.TYPE)
public fun interface SiteLayout {

    context(SiteBuilder)
    public fun render(item: DataTreeItem<*>)

    public companion object {
        public const val TYPE: String = "snark.layout"
        public const val LAYOUT_KEY: String = "layout"
        public const val ASSETS_KEY: String = "assets"
        public val INDEX_PAGE_TOKEN: NameToken = NameToken("index")
    }
}


/**
 * The default [SiteLayout]. It renders all [HtmlData] pages with simple headers via [SiteLayout.defaultDataRenderer]
 */
public object DefaultSiteLayout : SiteLayout {
    context(SiteBuilder) override fun render(item: DataTreeItem<*>) {
        pages(item)
    }
}