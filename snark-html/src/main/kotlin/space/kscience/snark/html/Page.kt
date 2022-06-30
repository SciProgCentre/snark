package space.kscience.snark.html

import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.ContextAware
import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.*
import space.kscience.snark.SnarkContext

context(SnarkContext) public fun Name.toWebPath(): String = tokens.joinToString(separator = "/") {
    if (it.hasIndex()) {
        "${it.body}[${it.index}]"
    } else {
        it.body
    }
}

public interface Page : ContextAware, SnarkContext {

    public val snark: SnarkPlugin

    override val context: Context get() = snark.context

    public val data: DataTree<*>

    public val pageMeta: Meta

    public fun resolveRef(ref: String): String

    public fun resolvePageRef(pageName: Name): String
}

context(Page) public val page: Page get() = this@Page

public fun Page.resolvePageRef(pageName: String): String = resolvePageRef(pageName.parseAsName())

public val Page.homeRef: String get() = resolvePageRef(SiteBuilder.INDEX_PAGE_TOKEN.asName())

/**
 * Resolve a Html builder by its full name
 */
context(SnarkContext) public fun DataTree<*>.resolveHtml(name: Name): HtmlData? {
    val resolved = (getByType<HtmlFragment>(name) ?: getByType<HtmlFragment>(name + SiteBuilder.INDEX_PAGE_TOKEN))

    return resolved?.takeIf {
        it.published //TODO add language confirmation
    }
}

/**
 * Find all Html blocks using given name/meta filter
 */
context(SnarkContext) public fun DataTree<*>.resolveAllHtml(predicate: (name: Name, meta: Meta) -> Boolean): Map<Name, HtmlData> =
    filterByType<HtmlFragment> { name, meta ->
        predicate(name, meta)
                && meta["published"].string != "false"
        //TODO add language confirmation
    }.asSequence().associate { it.name to it.data }


context(SnarkContext) public fun DataTree<*>.findByContentType(
    contentType: String,
    baseName: Name = Name.EMPTY,
): Map<Name, Data<HtmlFragment>> = resolveAllHtml { name, meta ->
    name.startsWith(baseName) && meta["content_type"].string == contentType
}