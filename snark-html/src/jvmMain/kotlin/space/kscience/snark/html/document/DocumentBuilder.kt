package space.kscience.snark.html.document

import kotlinx.coroutines.runBlocking
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.title
import space.kscience.dataforge.context.request
import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Laminate
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.Name
import space.kscience.snark.SnarkBuilder
import space.kscience.snark.SnarkContext
import space.kscience.snark.html.*
import kotlin.reflect.typeOf

/**
 *  A context for building a single document
 */
@SnarkBuilder
public interface DocumentBuilder : SnarkContext {

    public val documentName: Name

    public val documentMeta: Meta

    public val documentData: DataTree<*>

    public suspend fun fragment(fragment: Data<*>, overrideMeta: Meta? = null)

    public suspend fun fragment(fragment: DocumentFragment, overrideMeta: Meta? = null)
}

context(SiteContextWithData)
public suspend fun DocumentBuilder.fragment(fragmentName: Name) {
    fragment(site.siteData[fragmentName] ?: error("Can't find data fragment for $fragmentName in site data."))
}

context(SiteContextWithData)
public suspend fun DocumentBuilder.fragment(fragmentName: String) {
    fragment(site.siteData[fragmentName] ?: error("Can't find data fragment for $fragmentName in site data."))
}

private class PageBasedDocumentBuilder(val page: PageContextWithData) : DocumentBuilder {
    override val documentName: Name get() = page.pageRoute
    override val documentMeta: Meta get() = page.pageMeta
    override val documentData: DataTree<*> get() = page.data

    val fragments = mutableListOf<PageFragment>()

    fun fragment(pageFragment: PageFragment) {
        fragments.add(pageFragment)
    }

    override suspend fun fragment(fragment: DocumentFragment, overrideMeta: Meta?) {
        when (fragment) {
            is ListDocumentFragment -> {
                val meta = Laminate(overrideMeta, fragment.meta)
                fragment.fragments.forEach { fragment(it, meta) }
            }

            is ImageDocumentFragment -> TODO()
            is MarkupDocumentFragment -> {
                val snarkHtml = page.context.request(SnarkHtml)
                TODO()
            }

            is DataDocumentFragment -> {
                val data = documentData[fragment.dataName]
                    ?: error("Can't find data with name ${fragment.dataName} for $fragment")
                fragment(data)
            }

            is LayoutDocumentFragment -> TODO()
        }
    }

    override suspend fun fragment(fragment: Data<*>, overrideMeta: Meta?) {
        when (fragment.type) {
            typeOf<PageFragment>() -> fragment(fragment.await() as PageFragment)
            typeOf<DocumentFragment>() -> fragment(
                fragment.await() as DocumentFragment,
                Laminate(overrideMeta, documentData.meta)
            )

            typeOf<String>() -> fragment(
                MarkupDocumentFragment(fragment.await() as String, fragment.meta),
                overrideMeta
            )

            else -> error("Unsupported data type: ${fragment.type}")
        }
    }
}

public fun SiteContextWithData.document(
    documentName: Name,
    documentMeta: Meta = Meta.EMPTY,
    block: suspend DocumentBuilder.() -> Unit,
): Unit = page(documentName, documentMeta) {
    val documentBuilder = runBlocking { PageBasedDocumentBuilder(page).apply { block() } }
    head {
        title(documentMeta["title"].string ?: "Snark document")
    }
    body {
        postprocess(FtlDocumentProcessor(this@document.context, documentBuilder)) {
            documentBuilder.fragments.forEach {
                fragment(it)
            }
        }
    }
}