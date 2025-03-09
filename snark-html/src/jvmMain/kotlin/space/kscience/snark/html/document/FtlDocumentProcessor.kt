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
import java.io.StringReader
import java.io.StringWriter


public class FtlDocumentProcessor(
    override val context: Context,
    private val document: DocumentBuilder,
    counters: Map<String, Int> = emptyMap(),
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


    private val ftlConfig = Configuration(Configuration.VERSION_2_3_32).apply {
        defaultEncoding = "UTF-8"
        templateExceptionHandler = TemplateExceptionHandler { te: TemplateException, env, out ->
            logger.error(te) { "An exception while rendering template" }
        }
    }

    private val data = mapOf(
        "documentName" to document.route.toStringUnescaped(),

        "label" to TemplateMethodModelEx { args: List<Any?> ->
            val counter = args.getOrNull(0)?.toString() ?: "@default"
            val value = getAndIncrementCounter(counter)
            val ref = ref(counter, value)
            //language=HTML
            """<a class="snark-label" id="$ref" href="#$ref">$value</a>"""
        },

        "section" to TemplateMethodModelEx { args: List<Any?> ->
            val level: Int = args.getOrNull(0)?.toString()?.toIntOrNull() ?: 1
            val counter = getAndIncrementCounter("section[$level]")
            val ref = ref("section[$level]", counter)
            //language=HTML
            """<a class="snark-section snark-label" id="$ref" href = "#$ref">$counter</a>"""
        },

        "documentMeta" to document.documentMeta.toMap().let {
            it.plus(
                "get" to TemplateMethodModelEx { args: List<Any?> ->
                    val nameString = args.getOrNull(0)?.toString() ?: ""
                    document.documentMeta[nameString.parseAsName()].string ?: "@null"
                }
            )
        }

    )


    override fun process(text: CharSequence): String {
        val template = Template("fragment", StringReader(text.toString()), ftlConfig)
        return StringWriter().also {
            template.process(data, it)
        }.toString()
    }
}