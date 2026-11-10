package space.kscience.snark.html.document

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import space.kscience.dataforge.context.info
import space.kscience.dataforge.context.logger
import space.kscience.dataforge.context.request
import space.kscience.dataforge.data.*
import space.kscience.dataforge.io.Binary
import space.kscience.dataforge.meta.Laminate
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.*
import space.kscience.dataforge.workspace.filterByType
import space.kscience.snark.SnarkContext
import space.kscience.snark.TextProcessor
import space.kscience.snark.html.*
import kotlin.reflect.typeOf

/**
 *  A context for building a single document
 */
public interface DocumentBuilder : SnarkContext {

    public val route: Name

    public val documentMeta: Meta

    public val data: DataTree<*>

    public suspend fun fragment(fragment: Data<*>, overrideMeta: Meta? = null)

    public suspend fun fragment(fragment: DocumentFragment, overrideMeta: Meta? = null)
}

context(site: SiteContextWithData)
public suspend fun DocumentBuilder.fragment(fragmentName: Name) {
    this.fragment(site.siteData[fragmentName] ?: error("Can't find data fragment for $fragmentName in site data."))
}

context(site: SiteContextWithData)
public suspend fun DocumentBuilder.fragment(fragmentName: String) {
    this.fragment(site.siteData[fragmentName] ?: error("Can't find data fragment for $fragmentName in site data."))
}

private class PageBasedDocumentBuilder(
    val page: PageContextWithData,
    private val dataRootName: Name,
) : DocumentBuilder {
    override val route: Name get() = page.pageRoute
    override val documentMeta: Meta get() = page.pageMeta
    override val data: DataTree<*> = page.data.branch(dataRootName) ?: DataTree.EMPTY

    val fragments = mutableListOf<PageFragment>()

    fun fragment(pageFragment: PageFragment) {
        fragments.add(pageFragment)
    }

    override suspend fun fragment(fragment: DocumentFragment, overrideMeta: Meta?) {
        when (fragment) {
            is ImageDocumentFragment -> fragment {
                val page =
                figure("snark-figure") {
                    img(classes = "snark-image") {
                        src = page.resolveRef(this@PageBasedDocumentBuilder.route.toWebPath() + "/" + fragment.ref)
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

context(site: SiteContextWithData)
public fun document(
    dataName: Name,
    descriptor: DocumentDescriptor = DocumentDescriptor.empty(),
    route: Name = dataName,
    headers: MetaDataContent.() -> Unit = {},
    documentBlock: DocumentBuilder.() -> Unit = {},
): Unit {
    // files keep their path inside the document directory: figures/plot.png is served at <route>/figures/plot.png
    siteData.branch(dataName)?.filterByType<Binary>()?.forEach {
        site.static(route + it.name, it)
    }
    page(route, descriptor.documentMeta ?: Meta.EMPTY) {
        //TODO think about avoiding blocking
        val documentBuilder = runBlocking {
            PageBasedDocumentBuilder(contextOf<PageContextWithData>(), dataName).apply {
                descriptor.fragments.forEach {
                    fragment(it)
                }
                documentBlock()
            }
        }
        val pageContext = contextOf<PageContextWithData>()
        val documentPath = route.toWebPath()

        val templates = FtlDocumentProcessor(site.context, documentBuilder, page = pageContext)
        val links = WebPageTextProcessor(pageContext)

        head {
            title(descriptor.title ?: "Snark document")
            headers()
            descriptor.stylesheets?.forEach {
                link(rel = "stylesheet", href = pageContext.resolveRef("$documentPath/$it"))
            }
            descriptor.scripts?.forEach {
                script(src = pageContext.resolveRef("$documentPath/$it")) { defer = true }
            }
        }
        body {
            h1("title") { +(descriptor.title ?: dataName.toString()) }
            descriptor.authors.forEach {
                div("author") {
                    div("author-name") { +it.name }
                    it.affiliation?.let { affiliation -> div("author-affiliation") { +affiliation } }
                }
            }

            // text is processed as a template; snark:// links in href and src attributes are resolved as well.
            // Fragments are rendered once; ${ref(...)} numbers are filled in afterwards, so they can point forward.
            val content = createHTML(prettyPrint = false).div {
                postprocess(templates, TextProcessor { text -> links.process(templates.process(text)) }) {
                    documentBuilder.fragments.forEach {
                        fragment(it)
                    }
                }
            }.removePrefix("<div>").removeSuffix("</div>")
            unsafe { +templates.resolveReferences(content) }
        }
    }
}

context(site: SiteContextWithData)
public fun allDocuments(
    headers: MetaDataContent.() -> Unit = {},
) {
    siteData.forEach { documentData ->
        if (documentData.type == typeOf<Meta>() && documentData.name.endsWith("document")) {
            site.context.launch {
                val descriptor = DocumentDescriptor.read(documentData.await() as Meta)
                val directory = documentData.name.cutLast()
                val route = descriptor.route?.parseAsName(false) ?: directory
                site.context.logger.info { "Loading document $route" }
                document(
                    dataName = directory,
                    descriptor = descriptor,
                    route = route,
                    headers = headers
                )
            }
        }
    }
}