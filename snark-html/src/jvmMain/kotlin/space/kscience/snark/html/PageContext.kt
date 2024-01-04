package space.kscience.snark.html

import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.hasIndex
import space.kscience.dataforge.names.parseAsName
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

    public val site: SiteContext

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


public class PageContextWithData(private val pageContext: PageContext, public val data: DataSet<*>): PageContext by pageContext