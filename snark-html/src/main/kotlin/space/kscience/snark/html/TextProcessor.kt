package space.kscience.snark.html

import space.kscience.dataforge.meta.string
import space.kscience.dataforge.misc.DfId
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.parseAsName

/**
 * An object that conducts page-based text transformation. Like using link replacement or templating.
 */
@DfId(TextProcessor.TYPE)
public fun interface TextProcessor {
    context(WebPage)
    public fun process(text: String): String

    public companion object {
        public const val TYPE: String = "snark.textTransformation"
        public val TEXT_TRANSFORMATION_KEY: NameToken = NameToken("transformation")
    }
}

/**
 * A basic [TextProcessor] that replaces `${...}` expressions in text. The following expressions are recognised:
 * * `homeRef` resolves to [homeRef]
 * * `resolveRef("...")` -> [WebPage.resolveRef]
 * * `resolvePageRef("...")` -> [WebPage.resolvePageRef]
 * * `pageMeta.get("...") -> [WebPage.pageMeta] get string method
 * Otherwise return unchanged string
 */
public object BasicTextProcessor : TextProcessor {

    private val regex = """\$\{([\w.]*)(?>\("(.*)"\))?}""".toRegex()

    context(WebPage)
    override fun process(text: String): String = text.replace(regex) { match ->
        when (match.groups[1]!!.value) {
            "homeRef" -> homeRef
            "resolveRef" -> {
                val refString = match.groups[2]?.value ?: error("resolveRef requires a string (quoted) argument")
                resolveRef(refString)
            }

            "resolvePageRef" -> {
                val refString = match.groups[2]?.value
                    ?: error("resolvePageRef requires a string (quoted) argument")
                localisedPageRef(refString.parseAsName())
            }

            "pageMeta.get" -> {
                val nameString = match.groups[2]?.value
                    ?: error("resolvePageRef requires a string (quoted) argument")
                pageMeta[nameString.parseAsName()].string ?: "@null"
            }

            else -> match.value
        }
    }
}


