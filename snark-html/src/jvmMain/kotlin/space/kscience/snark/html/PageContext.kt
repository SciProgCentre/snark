package space.kscience.snark.html

import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import space.kscience.dataforge.data.DataTree
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
public interface PageContext : SnarkContext {

    public val site: SiteContext

    public val host: Url

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
        val pageUrl = URLBuilder(host)
            .appendPathSegments(targetSite.path, true)
            .appendPathSegments(ref)

        return pageUrl.buildString().removeSuffix("/")
    }

    /**
     * Resolve absolute url for a page with given [pageName].
     *
     * @param relative if true, add [SiteContext] route to the absolute page name
     */
    public fun resolvePageRef(pageName: Name): String

    public fun resolveRelativePageRef(pageName: Name): String = resolvePageRef(pageRoute + pageName)

}

context(PageContext)
public val page: PageContext
    get() = this@PageContext

public fun PageContext.resolvePageRef(pageName: String): String =
    resolvePageRef(pageName.parseAsName())

public val PageContext.homeRef: String get() = resolvePageRef(Name.EMPTY)

public val PageContext.name: Name? get() = pageMeta["name"].string?.parseAsName()


public class PageContextWithData(
    private val pageContext: PageContext,
    public val data: DataTree<*>,
) : PageContext by pageContext