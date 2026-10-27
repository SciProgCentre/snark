package space.kscience.snark.html

import kotlinx.coroutines.runBlocking
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.ContextAware
import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.await
import space.kscience.dataforge.data.get
import space.kscience.dataforge.meta.Meta
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

    public fun <T> awaitData(data: Data<T>): T = runBlocking { data.await() }

}

/**
 * Resolves the absolute URL for a page with the given [pageName].
 */
@JvmName("resolvePageRefWithReceiver")
public fun PageContext.resolvePageRef(pageName: String, targetSite: SiteContext = site): String =
    resolvePageRef(pageName.parseAsName())

context(pageContext: PageContext)
public val page: PageContext get() = pageContext

context(pageContext: PageContext)
public val pageMeta: Meta get() = pageContext.pageMeta

context(pageContext: PageContext)
public fun resolveRef(
    ref: String,
    targetSite: SiteContext = pageContext.site
): String = pageContext.resolveRef(ref, targetSite)

/**
 * Resolves a smart reference based on its format and context, returning the appropriate fully resolved URL or reference.
 * The behavior depends on the format of the provided reference string:
 * - If the reference starts with "http" or "mailto:", the original reference is returned.
 * - If the reference starts with "#", it is resolved relative to the current page route.
 * - For all other cases, the reference is resolved using the page context.
 *
 * @param ref The reference string to be resolved. It can be an absolute URL, a fragment identifier, or a relative reference.
 * @return The fully resolved reference string corresponding to the input.
 */
context(pageContext: PageContext)
public fun resolveSmartRef(
    ref: String
): String = when {
    ref.startsWith("http") || ref.startsWith("mailto:") -> {
        ref
    }

    ref.startsWith("#") -> {
        resolvePageRef(pageContext.pageRoute) + ref
    }

    else -> {
        resolvePageRef(ref)
    }
}

/**
 * Resolve page fragment relative to this page route name.
 */
context(pageContext: PageContextWithData)
public fun resolvePageFragment(
    ref: String
): Data<PageFragment>? = pageContext.data.resolveHtmlOrNull(ref)

/**
 * Page data for this page
 */
context(pageContext: PageContextWithData)
public val pageData: DataTree<*> get() = pageContext.data

context(pageContext: PageContext)
public fun resolvePageRef(pageName: Name, targetSite: SiteContext = pageContext.site): String =
    pageContext.resolvePageRef(pageName, targetSite)

context(pageContext: PageContext)
public fun resolvePageRef(pageName: String, targetSite: SiteContext = pageContext.site): String =
    pageContext.resolvePageRef(pageName.parseAsName(), targetSite)

/**
 * Page reference for root page of current site
 */
public val PageContext.homeRef: String get() = resolvePageRef(Name.EMPTY)

/**
 * Page context and data for this page
 */
public class PageContextWithData(
    private val pageContext: PageContext,
    public val data: DataTree<*>,
) : PageContext by pageContext

/**
 * Resolve and await data by path and return it as a specified type if possible
 */
context(pageContext: PageContextWithData)
public inline fun <reified R> awaitDataByType(path: Name): R? = pageContext.data[path]?.let { pageContext.awaitData(it) as? R }

/**
 * Resolve and await data by path and return it as a specified type if possible
 */
context(pageContext: PageContextWithData)
public inline fun <reified R> awaitDataByType(path: String): R? = awaitDataByType(path.parseAsName())

//TODO add implicit type conversions for page content?