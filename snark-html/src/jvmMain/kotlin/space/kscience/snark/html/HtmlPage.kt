package space.kscience.snark.html

import kotlinx.css.html
import kotlinx.html.HTML
import kotlinx.html.dom.append
import kotlinx.html.dom.document
import kotlinx.html.dom.serialize
import kotlinx.html.html
import space.kscience.dataforge.data.DataSink
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.wrap
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name

public fun interface HtmlPage {

    context(PageContextWithData, HTML)
    public fun renderPage()

    public companion object {

        public fun createHtmlString(
            pageContext: PageContext,
            dataSet: DataTree<*>?,
            page: HtmlPage,
        ): String = document {
            append.html {
                with(PageContextWithData(pageContext, dataSet ?: DataTree.EMPTY)) {
                    with(page) {
                        renderPage()
                    }
                }
            }
        }.serialize(true)
    }

}


// data builders

public fun DataSink<Any>.page(
    name: Name,
    pageMeta: Meta = Meta.EMPTY,
    block: context(PageContextWithData) HTML.() -> Unit,
) {
    val page = HtmlPage(block)
    wrap<HtmlPage>(name, page, pageMeta)
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