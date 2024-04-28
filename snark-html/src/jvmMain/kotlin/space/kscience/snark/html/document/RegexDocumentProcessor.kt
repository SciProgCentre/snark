package space.kscience.snark.html.document

import space.kscience.dataforge.meta.int
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.*
import space.kscience.snark.TextProcessor
import java.net.URI

public class RegexDocumentProcessor(public val document: DocumentBuilder) : TextProcessor {

    private val counters = mutableMapOf<String, Int>()

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

    override fun process(text: CharSequence): String = text.replace(functionRegex) { match ->
        when (match.groups["function"]?.value) {

            "documentName" -> {
                document.documentName.toStringUnescaped()
            }

            "label" -> {
                val counter = match.groups["arg1"]?.value ?: "@default"
                val value = getAndIncrementCounter(counter)
                val ref = ref(counter, value)
                //language=HTML
                """<a class="snark-label" id="$ref" href="#$ref">$value</a>"""
            }

//            "ref" -> {
//                val target = match.groups["arg1"]?.value
//                when
//            }

            "section" -> {
                val level: Int = match.groups["arg1"]?.value?.toIntOrNull() ?: 1
                val counter = getAndIncrementCounter("section[$level]")
                val ref = ref("section[$level]", counter)
                //language=HTML
                """<a class="snark-section snark-label" id="$ref" href = "#$ref">$counter</a>"""
            }

            "documentMeta.get" -> {
                val nameString = match.groups["arg1"]?.value
                    ?: error("resolvePageRef requires a string (quoted) argument")
                document.documentMeta[nameString.parseAsName()].string ?: "@null"
            }

            else -> match.value
        }
    }.replace(attributeRegex) { match ->
        val uri = URI(match.groups["uri"]!!.value)
        val snarkUrl = when (uri.authority) {
            "documentName" -> document.documentName.toStringUnescaped()
//            "ref" -> page.resolveRef(uri.path)
            "meta" -> document.documentMeta[uri.path.parseAsName()].string ?: "@null"
            else -> match.value
        }
        "=\"$snarkUrl\""
    }


    public companion object {
        internal val functionRegex =
            """\$\{(?<function>\w*)(?:\((?<arg1>[^(),]*)(\s*,\s*(?<arg2>[^(),]*))?\))?\}""".toRegex()
        private val attributeRegex = """="(?<uri>snark://([^"]*))"""".toRegex()
    }
}
