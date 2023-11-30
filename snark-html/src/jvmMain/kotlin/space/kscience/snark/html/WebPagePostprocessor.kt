package space.kscience.snark.html

import kotlinx.html.*
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.parseAsName
import space.kscience.snark.TextProcessor

public class WebPageTextProcessor(private val page: PageContext) : TextProcessor {
    private val regex = """\$\{([\w.]*)(?>\("(.*)"\))?}""".toRegex()

    /**
     * A basic [TextProcessor] that replaces `${...}` expressions in text. The following expressions are recognised:
     * * `homeRef` resolves to [homeRef]
     * * `resolveRef("...")` -> [PageContext.resolveRef]
     * * `resolvePageRef("...")` -> [PageContext.resolvePageRef]
     * * `pageMeta.get("...") -> [PageContext.pageMeta] get string method
     * Otherwise return unchanged string
     */
    override fun process(text: CharSequence): String = text.replace(regex) { match ->
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

public class WebPagePostprocessor<out R>(
    public val page: PageContext,
    private val consumer: TagConsumer<R>,
) : TagConsumer<R> by consumer {

    private val processor = WebPageTextProcessor(page)

    override fun onTagAttributeChange(tag: Tag, attribute: String, value: String?) {
        if (tag is A && attribute == "href" && value != null) {
            consumer.onTagAttributeChange(tag, attribute, processor.process(value))
        } else {
            consumer.onTagAttributeChange(tag, attribute, value)
        }
    }

    override fun onTagContent(content: CharSequence) {
        consumer.onTagContent(processor.process(content))
    }

    override fun onTagContentUnsafe(block: Unsafe.() -> Unit) {
        val proxy = object :Unsafe{
            override fun String.unaryPlus() {
                consumer.onTagContentUnsafe {
                    processor.process(this@unaryPlus).unaryPlus()
                }
            }
        }
        proxy.block()
    }
}

public inline fun FlowContent.withSnarkPage(page: PageContext, block: FlowContent.() -> Unit) {
    val fc = object : FlowContent by this {
        override val consumer: TagConsumer<*> = WebPagePostprocessor(page, this@withSnarkPage.consumer)
    }
    fc.block()
}