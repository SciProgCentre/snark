package space.kscience.snark.html

import space.kscience.dataforge.misc.Type
import space.kscience.dataforge.names.NameToken

@Type(TextTransformation.TYPE)
public fun interface TextTransformation {
    context(Page) public fun transform(text: String): String

    public companion object {
        public const val TYPE: String = "snark.textTransformation"
        public val TEXT_TRANSFORMATION_KEY: NameToken = NameToken("transformation")
    }
}

public object BasicTextTransformation : TextTransformation {

    private val regex = "\\\$\\{(\\w*)(?>\\(\"(.*)\"\\))?\\}".toRegex()

    context(Page) override fun transform(text: String): String {
        return text.replace(regex) { match ->
            when (match.groups[1]!!.value) {
                "homeRef" -> homeRef
                "resolveRef" -> {
                    val refString = match.groups[2]?.value ?: error("resolveRef requires a string (quoted) argument")
                    resolveRef(refString)
                }
                "resolvePageRef" -> {
                    val refString = match.groups[2]?.value ?: error("resolvePageRef requires a string (quoted) argument")
                    resolvePageRef(refString)
                }
                else -> match.value
            }
        }
    }
}


