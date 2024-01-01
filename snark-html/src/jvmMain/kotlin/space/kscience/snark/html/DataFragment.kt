package space.kscience.snark.html

import kotlinx.coroutines.Dispatchers
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

public fun interface DataFragment {
    public suspend fun FlowContent.renderFragment(page: PageContext, data: DataSet<*>)
}


context(PageContext)
public fun FlowContent.htmlData(data: DataSet<*>, fragment: Data<DataFragment>): Unit = runBlocking(Dispatchers.IO) {
    with(fragment.await()) { renderFragment(page, data) }
}

context(SnarkContext)
public val Data<*>.id: String
    get() = meta["id"]?.string ?: "block[${hashCode()}]"

context(SnarkContext)
public val Data<*>.language: String?
    get() = meta["language"].string?.lowercase()

context(SnarkContext)
public val Data<*>.order: Int?
    get() = meta["order"]?.int

context(SnarkContext)
public val Data<*>.published: Boolean
    get() = meta["published"].string != "false"


/**
 * Resolve a Html builder by its full name
 */
context(SnarkContext)
public fun DataSet<*>.resolveHtmlOrNull(name: Name): Data<DataFragment>? {
    val resolved = (getByType<DataFragment>(name) ?: getByType<DataFragment>(name + SiteContext.INDEX_PAGE_TOKEN))

    return resolved?.takeIf {
        it.published //TODO add language confirmation
    }
}

context(SnarkContext)
public fun DataSet<*>.resolveHtmlOrNull(name: String): Data<DataFragment>? = resolveHtmlOrNull(name.parseAsName())

context(SnarkContext)
public fun DataSet<*>.resolveHtml(name: String): Data<DataFragment> = resolveHtmlOrNull(name)
    ?: error("Html fragment with name $name is not resolved")

/**
 * Find all Html blocks using given name/meta filter
 */
context(SnarkContext)
public fun DataSet<*>.resolveAllHtml(
    predicate: (name: Name, meta: Meta) -> Boolean,
): Map<Name, Data<DataFragment>> = filterByType<DataFragment> { name, meta ->
    predicate(name, meta)
            && meta["published"].string != "false"
    //TODO add language confirmation
}.asSequence().associate { it.name to it.data }

context(SnarkContext)
public fun DataSet<*>.findHtmlByContentType(
    contentType: String,
    baseName: Name = Name.EMPTY,
): Map<Name, Data<DataFragment>> = resolveAllHtml { name, meta ->
    name.startsWith(baseName) && meta["content_type"].string == contentType
}