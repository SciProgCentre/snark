package space.kscience.snark.html.document

import freemarker.template.*
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.ContextAware
import space.kscience.dataforge.context.error
import space.kscience.dataforge.context.logger
import space.kscience.dataforge.meta.int
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.meta.toMap
import space.kscience.dataforge.names.*
import space.kscience.snark.TextProcessor
import space.kscience.snark.html.PageContext
import space.kscience.snark.html.homeRef
import space.kscience.snark.html.toWebPath
import java.io.StringReader
import java.io.StringWriter

/**
 * Processes document text as a FreeMarker template: numbering, cross-references, document metadata and links.
 *
 * `${ref("key")}` writes an empty link; [resolveReferences] fills it after the render, when all numbers are known.
 *
 * @param page page context used for link functions, if available
 */
public class FtlDocumentProcessor(
    override val context: Context,
    private val document: DocumentBuilder,
    counters: Map<String, Int> = emptyMap(),
    private val page: PageContext? = null,
) : TextProcessor, ContextAware {

    private val counters = counters.toMutableMap()

    private fun getCounter(counter: String): Int = counters[counter] ?: 1

    private fun getAndIncrementCounter(counter: String): Int {
        val currentCounter = counters[counter] ?: document.documentMeta[NameToken("counter", counter).asName()].int ?: 1
        counters[counter] = currentCounter + 1
        return currentCounter
    }

    private fun resetAndGet(counter: String, value: Int = 1): Int {
        counters[counter] = value
        return value
    }

    private fun ref(counter: String, value: Int): String {
        //TODO replace by name parse in future
        val token = Name.parse(counter).last()
        return when (token.body) {
            "section" -> {
                val level = token.index?.toIntOrNull() ?: 1
                (1..level).joinToString(separator = "_") {
                    if (it == level) value.toString() else getCounter("section[$it]").toString()
                }
            }

            else -> "snark_counter_${counter}_$value"
        }
    }

    // numbers of labelled items assigned during the render, by key
    private val assignedLabels = mutableMapOf<String, String>()

    private val refRegex = Regex("""<a class="snark-ref" href="#([^"]*)"></a>""")

    /**
     * Fills `${ref("key")}` links in [html], rendered with this processor, with the numbers its counters assigned.
     * Call it after all fragments are rendered, so that references can point to items defined later.
     */
    public fun resolveReferences(html: String): String = refRegex.replace(html) { match ->
        val key = match.groupValues[1]
        val number = assignedLabels[key] ?: run {
            logger.error { "Unknown reference '$key' in document ${document.route}" }
            "??"
        }
        """<a class="snark-ref" href="#$key">$number</a>"""
    }

    private fun List<Any?>.stringArg(index: Int): String? = getOrNull(index)?.toString()

    private fun anchor(classes: String, id: String, text: String): String =
        """<a class="$classes" id="$id" href="#$id">$text</a>"""

    private val ftlConfig = Configuration(Configuration.VERSION_2_3_32).apply {
        defaultEncoding = "UTF-8"
        templateExceptionHandler = TemplateExceptionHandler { te: TemplateException, _, _ ->
            logger.error(te) { "An exception while rendering template" }
        }
    }

    private val data: Map<String, Any> = buildMap {
        put("documentName", document.route.toStringUnescaped())

        // ${label("figure")} or ${label("figure", "fig-tree")}
        put("label", TemplateMethodModelEx { args: List<Any?> ->
            val counter = args.stringArg(0) ?: "@default"
            val value = getAndIncrementCounter(counter)
            val key = args.stringArg(1)
            if (key != null) assignedLabels[key] = value.toString()
            anchor("snark-label", key ?: ref(counter, value), value.toString())
        })

        // ${section(2)} or ${section(2, "sec-names")}
        put("section", TemplateMethodModelEx { args: List<Any?> ->
            val level: Int = args.stringArg(0)?.toIntOrNull() ?: 1
            val counter = getAndIncrementCounter("section[$level]")
            val key = args.stringArg(1)
            if (key != null) assignedLabels[key] = counter.toString()
            anchor("snark-section snark-label", key ?: ref("section[$level]", counter), counter.toString())
        })

        // ${ref("fig-tree")}: a link to a labelled item; its number is filled in by resolveReferences
        put("ref", TemplateMethodModelEx { args: List<Any?> ->
            """<a class="snark-ref" href="#${args.stringArg(0) ?: ""}"></a>"""
        })

        put("documentMeta", document.documentMeta.toMap().plus(
            "get" to TemplateMethodModelEx { args: List<Any?> ->
                val nameString = args.stringArg(0) ?: ""
                document.documentMeta[nameString.parseAsName()].string ?: "@null"
            }
        ))

        if (page != null) {
            // ${documentRef("figures/plot.png")}: a file from the document directory
            put("documentRef", TemplateMethodModelEx { args: List<Any?> ->
                page.resolveRef(document.route.toWebPath() + "/" + (args.stringArg(0) ?: ""))
            })
            put("homeRef", page.homeRef)
            put("resolveRef", TemplateMethodModelEx { args: List<Any?> -> page.resolveRef(args.stringArg(0) ?: "") })
            put("resolvePageRef", TemplateMethodModelEx { args: List<Any?> ->
                page.resolvePageRef((args.stringArg(0) ?: "").parseAsName())
            })
        }
    }

    override fun process(text: CharSequence): String {
        if (!text.contains("\${") && !text.contains("<#")) return text.toString()
        val template = Template("fragment", StringReader(text.toString()), ftlConfig)
        return StringWriter().also {
            template.process(data, it)
        }.toString()
    }
}
