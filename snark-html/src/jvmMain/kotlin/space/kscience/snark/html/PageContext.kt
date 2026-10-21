package space.kscience.snark.html

import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.ContextAware
import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.hasIndex
import space.kscience.dataforge.names.parseAsName
import space.kscience.dataforge.names.plus
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
public interface PageContext : SnarkContext, ContextAware {

    /**
     * A reference to parent site
     */
    public val site: SiteContext

    override val context: Context get() = site.context

    /**
     * A host site URI prefix added to paths when resolving refs on this page
     */
    public val hostUri: String

    /**
     * A metadata for a page. It should include site metadata
     */
    public val pageMeta: Meta

    /**
     * A route relative to parent site. Includes [SiteContext.siteRoute].
     */
    public val pageRoute: Name

    /**
     * Resolve absolute url for given relative [ref]
     *
     */
    public fun resolveRef(ref: String, targetSite: SiteContext = site): String {
        val pageUrl = hostUri + (targetSite.path + ref).joinToString(separator = "/", prefix = "/")

        return pageUrl.removeSuffix("/")
    }

    /**
     * Resolve absolute url for a page with given [pageName].
     */
    public fun resolvePageRef(pageName: Name, targetSite: SiteContext = site): String

    public fun resolveRelativePageRef(pageName: Name): String = resolvePageRef(pageRoute + pageName)

}

/**
 * Resolves the absolute URL for a page with the given [pageName].
 */
@JvmName("resolvePageRefWithReceiver")
public fun PageContext.resolvePageRef(pageName: String, targetSite: SiteContext = site): String =
    resolvePageRef(pageName.parseAsName())

context(pageContext: PageContext)
public val pageContext: PageContext get() = pageContext

context(pageContext: PageContext)
public fun resolveRef(
    ref: String,
    targetSite: SiteContext = pageContext.site
): String = pageContext.resolveRef(ref, targetSite)

/**
 * Resolve page fragment relative to this page route name.
 */
context(pageContext: PageContextWithData)
public fun resolvePageFragment(
    ref: String
): Data<PageFragment>? = pageContext.data.resolveHtmlOrNull(ref)

context(pageContext: PageContextWithData)
public val pageContextWithData: PageContextWithData get() = pageContext

context(pageContext: PageContextWithData)
public val data: DataTree<*> get() = pageContext.data

context(pageContext: PageContextWithData)
public val pageMeta: Meta get() = pageContext.pageMeta

context(pageContext: PageContext)
public fun resolvePageRef(pageName: Name, targetSite: SiteContext = pageContext.site): String =
    pageContext.resolvePageRef(pageName, targetSite)

context(pageContext: PageContext)
public fun resolvePageRef(pageName: String, targetSite: SiteContext = pageContext.site): String =
    pageContext.resolvePageRef(pageName.parseAsName(), targetSite)

public val PageContext.homeRef: String get() = resolvePageRef(Name.EMPTY)

public val PageContext.name: Name? get() = pageMeta["name"].string?.parseAsName()


public class PageContextWithData(
    private val pageContext: PageContext,
    public val data: DataTree<*>,
) : PageContext by pageContext