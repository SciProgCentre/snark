package space.kscience.snark.html

import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.parseAsName
import space.kscience.snark.TextProcessor

/**
 * A basic [TextProcessor] that replaces `${...}` expressions in text. The following expressions are recognised:
 * * `homeRef` resolves to [homeRef]
 * * `resolveRef("...")` -> [WebPage.resolveRef]
 * * `resolvePageRef("...")` -> [WebPage.resolvePageRef]
 * * `pageMeta.get("...") -> [WebPage.pageMeta] get string method
 * Otherwise return unchanged string
 */
public class WebPagePreprocessor(public val page: WebPage) : TextProcessor {

    private val regex = """\$\{([\w.]*)(?>\("(.*)"\))?}""".toRegex()


    override fun process(text: String): String = text.replace(regex) { match ->
        when (match.groups[1]!!.value) {
            "homeRef" -> page.homeRef
            "resolveRef" -> {
                val refString = match.groups[2]?.value ?: error("resolveRef requires a string (quoted) argument")
                page.resolveRef(refString)
            }

            "resolvePageRef" -> {
                val refString = match.groups[2]?.value
                    ?: error("resolvePageRef requires a string (quoted) argument")
                page.localisedPageRef(refString.parseAsName())
            }

            "pageMeta.get" -> {
                val nameString = match.groups[2]?.value
                    ?: error("resolvePageRef requires a string (quoted) argument")
                page.pageMeta[nameString.parseAsName()].string ?: "@null"
            }

            else -> match.value
        }
    }
}