package space.kscience.snark.html

import kotlinx.html.HTML
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import space.kscience.dataforge.data.DataSink
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.writeValue
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name

public fun interface HtmlPage {

    context(page: PageContextWithData)
    public fun HTML.renderPage()

    public companion object {

        private const val HTML_HEADER = "<!DOCTYPE html>\n"

        public fun createHtmlString(
            pageContext: PageContext,
            dataSet: DataTree<*>?,
            page: HtmlPage,
        ): String = HTML_HEADER + createHTML(true).run {
            html {
                with(PageContextWithData(pageContext, dataSet ?: DataTree.EMPTY)) {
                    with(page) {
                        renderPage()
                    }
                }
            }
        }
    }
}



// data builders

public suspend fun DataSink<Any>.page(
    name: Name,
    pageMeta: Meta = Meta.EMPTY,
    block: context(PageContextWithData) HTML.() -> Unit,
) {
    val page = HtmlPage(block)
    writeValue<HtmlPage>(name, page, pageMeta)
}