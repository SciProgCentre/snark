package space.kscience.snark.html.document

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.html.*
import space.kscience.dataforge.context.info
import space.kscience.dataforge.context.logger
import space.kscience.dataforge.context.request
import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Laminate
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.cutLast
import space.kscience.dataforge.names.endsWith
import space.kscience.dataforge.names.parseAsName
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

    public val data: DataTree<*>

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

private class PageBasedDocumentBuilder(
    val page: PageContextWithData,
    private val dataRootName: Name,
) : DocumentBuilder {
    override val documentName: Name get() = page.pageRoute
    override val documentMeta: Meta get() = page.pageMeta
    override val data: DataTree<*> = page.data.branch(dataRootName) ?: DataTree.EMPTY

    val fragments = mutableListOf<PageFragment>()

    fun fragment(pageFragment: PageFragment) {
        fragments.add(pageFragment)
    }

    override suspend fun fragment(fragment: DocumentFragment, overrideMeta: Meta?) {
        when (fragment) {

            is ImageDocumentFragment -> fragment {
                figure("snark-figure") {
                    img(classes = "snark-image") {
                        src = fragment.path
                        alt = fragment.meta["alt"].string ?: ""
                    }
                    fragment.meta["caption"].string?.let { caption ->
                        figcaption("snark-figure-caption") { +caption }
                    }
                }
            }

            is MarkupDocumentFragment -> {
                val snarkHtml = page.context.request(SnarkHtml)
                snarkHtml.parseMarkup(Name.EMPTY, fragment.text, fragment.meta)
            }

            is DataDocumentFragment -> {
                val data = data[fragment.name]
                    ?: error("Can't find data with name ${fragment.name} for $fragment")
                fragment(data)
            }

            is ListDocumentFragment -> {
                val meta = Laminate(overrideMeta, fragment.meta)
                fragment.fragments.forEach { fragment(it, meta) }
            }

            is LayoutDocumentFragment -> TODO("Layouts are not implemented")
        }
    }

    override suspend fun fragment(fragment: Data<*>, overrideMeta: Meta?) {
        when (fragment.type) {
            typeOf<PageFragment>() -> fragment(fragment.await() as PageFragment)
            typeOf<DocumentFragment>() -> fragment(
                fragment.await() as DocumentFragment,
                Laminate(overrideMeta, data.meta)
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
    headers: MetaDataContent.() -> Unit = {},
    block: suspend DocumentBuilder.() -> Unit,
): Unit = page(documentName, documentMeta) {
    val documentBuilder = runBlocking { PageBasedDocumentBuilder(page, documentName).apply { block() } }
    head {
        title(documentMeta["title"].string ?: "Snark document")
        headers()
    }
    body {
        postprocess(FtlDocumentProcessor(this@document.context, documentBuilder)) {
            documentBuilder.fragments.forEach {
                fragment(it)
            }
        }
    }
}

public fun SiteContextWithData.document(
    route: Name,
    dataName: Name,
    descriptor: DocumentDescriptor,
    headers: MetaDataContent.() -> Unit = {},
): Unit = page(route, descriptor.documentMeta ?: Meta.EMPTY) {
    val documentBuilder = runBlocking {
        PageBasedDocumentBuilder(page, dataName).apply {
            descriptor.fragments.forEach {
                fragment(it)
            }
        }
    }
    head {
        title(descriptor.title ?: "Snark document")
        headers()
    }
    body {
        h1("title") { +(descriptor.title ?: dataName.toString()) }
        descriptor.authors.forEach {
            div("author") {
                div("author-name") { +it.name }
                it.affiliation?.let { affiliation -> div("author-affiliation") { +affiliation } }
            }
        }
        postprocess(FtlDocumentProcessor(this@document.context, documentBuilder)) {
            documentBuilder.fragments.forEach {
                fragment(it)
            }
        }
    }
}

public fun SiteContextWithData.allDocuments(
    headers: MetaDataContent.() -> Unit = {},
) {
    siteData.forEach { documentData ->
        if (documentData.type == typeOf<Meta>() && documentData.name.endsWith("document")) {
            context.launch {
                val descriptor = DocumentDescriptor.read(documentData.data.await() as Meta)
                val directory = documentData.name.cutLast()
                val route = descriptor.route?.parseAsName(false) ?: directory
                context.logger.info { "Loading document $route" }
                document(
                    route = route,
                    dataName = directory,
                    descriptor = descriptor,
                    headers = headers
                )
            }
        }
    }
}