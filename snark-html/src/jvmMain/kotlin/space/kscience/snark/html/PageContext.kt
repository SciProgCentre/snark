package space.kscience.snark.html

import kotlinx.html.HTML
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.ContextAware
import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.*
import space.kscience.snark.SnarkBuilder
import space.kscience.snark.SnarkContext

public fun Name.toWebPath(): String = tokens.joinToString(separator = "/") {
    if (it.hasIndex()) {
        "${it.body}[${it.index}]"
    } else {
        it.body
    }
}

/**
 *  A context for building a single page
 */
@SnarkBuilder
public interface PageContext :  SnarkContext {

    /**
     * A metadata for a page. It should include site metadata
     */
    public val pageMeta: Meta

    /**
     * Resolve absolute url for given [ref]
     *
     */
    public fun resolveRef(ref: String): String

    /**
     * Resolve absolute url for a page with given [pageName].
     *
     * @param relative if true, add [SiteContext] route to the absolute page name
     */
    public fun resolvePageRef(pageName: Name, relative: Boolean = false): String
}

context(PageContext)
public val page: PageContext
    get() = this@PageContext

public fun PageContext.resolvePageRef(pageName: String): String = resolvePageRef(pageName.parseAsName())

public val PageContext.homeRef: String get() = resolvePageRef(SiteContext.INDEX_PAGE_TOKEN.asName())

public val PageContext.name: Name? get() = pageMeta["name"].string?.parseAsName()

/**
 * Resolve a Html builder by its full name
 */
context(SnarkContext)
public fun DataTree<*>.resolveHtmlOrNull(name: Name): HtmlData? {
    val resolved = (getByType<HtmlFragment>(name) ?: getByType<HtmlFragment>(name + SiteContext.INDEX_PAGE_TOKEN))

    return resolved?.takeIf {
        it.published //TODO add language confirmation
    }
}

context(SnarkContext)
public fun DataTree<*>.resolveHtmlOrNull(name: String): HtmlData? = resolveHtmlOrNull(name.parseAsName())

context(SnarkContext)
public fun DataTree<*>.resolveHtml(name: String): HtmlData = resolveHtmlOrNull(name)
    ?: error("Html fragment with name $name is not resolved")

/**
 * Find all Html blocks using given name/meta filter
 */
context(SnarkContext)
public fun DataTree<*>.resolveAllHtml(predicate: (name: Name, meta: Meta) -> Boolean): Map<Name, HtmlData> =
    filterByType<HtmlFragment> { name, meta ->
        predicate(name, meta)
                && meta["published"].string != "false"
        //TODO add language confirmation
    }.asSequence().associate { it.name to it.data }


context(SnarkContext)
public fun DataTree<*>.findByContentType(
    contentType: String,
    baseName: Name = Name.EMPTY,
): Map<Name, Data<HtmlFragment>> = resolveAllHtml { name, meta ->
    name.startsWith(baseName) && meta["content_type"].string == contentType
}