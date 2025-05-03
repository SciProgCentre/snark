package space.kscience.snark.html

import kotlinx.coroutines.runBlocking
import kotlinx.html.FlowContent
import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.int
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.parseAsName
import space.kscience.dataforge.names.plus
import space.kscience.dataforge.names.startsWith
import space.kscience.snark.SnarkContext

public fun interface PageFragment {
    context(page: PageContextWithData) public fun FlowContent.renderFragment()
}

context(page: PageContextWithData)
public fun FlowContent.fragment(fragment: PageFragment): Unit {
    with(fragment) {
        renderFragment()
    }
}


context(page: PageContextWithData)
public fun FlowContent.fragment(data: Data<PageFragment>): Unit = runBlocking {
    fragment(data.await())
}


context(snark: SnarkContext)
public val Data<*>.id: String
    get() = meta["id"]?.string ?: "block[${hashCode()}]"

context(snark: SnarkContext)
public val Data<*>.order: Int?
    get() = meta["order"]?.int

context(snark: SnarkContext)
public val Data<*>.published: Boolean
    get() = meta["published"].string != "false"


/**
 * Resolve a Html builder by its full name
 */
context(snark: SnarkContext)
public fun DataTree<*>.resolveHtmlOrNull(name: Name): Data<PageFragment>? {
    val resolved = (getByType<PageFragment>(name) ?: getByType<PageFragment>(name + SiteContext.INDEX_PAGE_TOKEN))

    return resolved?.takeIf {
        it.published //TODO add language confirmation
    }
}

context(snark: SnarkContext)
public fun DataTree<*>.resolveHtmlOrNull(name: String): Data<PageFragment>? = resolveHtmlOrNull(name.parseAsName())

context(snark: SnarkContext)
public fun DataTree<*>.resolveHtml(name: String): Data<PageFragment> = resolveHtmlOrNull(name)
    ?: error("Html fragment with name $name is not resolved")

/**
 * Find all Html blocks using given name/meta filter
 */
context(snark: SnarkContext)
public fun DataTree<*>.resolveAllHtml(
    predicate: (name: Name, meta: Meta) -> Boolean,
): Map<Name, Data<PageFragment>> = filterByType<PageFragment> { name, meta, _ ->
    predicate(name, meta) && meta["published"].string != "false"
    //TODO add language confirmation
}.asSequence().associate { it.name to it.data }

context(snark: SnarkContext)
public fun DataTree<*>.findHtmlByContentType(
    contentType: String,
    baseName: Name = Name.EMPTY,
): Map<Name, Data<PageFragment>> = resolveAllHtml { name, meta ->
    name.startsWith(baseName) && meta["content_type"].string == contentType
}