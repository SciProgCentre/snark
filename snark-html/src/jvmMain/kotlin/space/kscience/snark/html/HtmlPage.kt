package space.kscience.snark.html

import kotlinx.html.HTML
import kotlinx.html.stream.createHTML
import kotlinx.html.visitTagAndFinalize
import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.data.DataSetBuilder
import space.kscience.dataforge.data.static
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name

public fun interface HtmlPage {

    context(PageContextWithData)
    public fun HTML.renderPage()

    public companion object {
        public fun createHtmlString(
            pageContext: PageContext,
            dataSet: DataSet<*>,
            page: HtmlPage,
        ): String = createHTML().run {
            HTML(kotlinx.html.emptyMap, this, null).visitTagAndFinalize(this) {
                with(PageContextWithData(pageContext, dataSet)) {
                    with(page) {
                        renderPage()
                    }
                }
            }
        }
    }
}


// data builders

public fun DataSetBuilder<Any>.page(
    name: Name,
    pageMeta: Meta = Meta.EMPTY,
    block: context(PageContextWithData) HTML.() -> Unit,
) {
    val page = HtmlPage(block)
    static<HtmlPage>(name, page, pageMeta)
}


//                if (data.type == typeOf<HtmlData>()) {
//                    val languageMeta: Meta = Language.forName(name)
//
//                    val dataMeta: Meta = if (languageMeta.isEmpty()) {
//                        data.meta
//                    } else {
//                        data.meta.toMutableMeta().apply {
//                            "languages" put languageMeta
//                        }
//                    }
//
//                    page(name, dataMeta) { pageContext->
//                        head {
//                            title = dataMeta["title"].string ?: "Untitled page"
//                        }
//                        body {
//                            @Suppress("UNCHECKED_CAST")
//                            htmlData(pageContext, data as HtmlData)
//                        }
//                    }
//                }