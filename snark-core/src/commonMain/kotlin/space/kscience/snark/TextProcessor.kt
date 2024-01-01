package space.kscience.snark

import space.kscience.dataforge.misc.DfType
import space.kscience.dataforge.names.NameToken

/**
 * An object that conducts page-based text transformation. Like using link replacement or templating.
 */
@DfType(TextProcessor.DF_TYPE)
public fun interface TextProcessor {

    public fun process(text: CharSequence): String

    public companion object {
        public const val DF_TYPE: String = "snark.textTransformation"
        public val TEXT_TRANSFORMATION_KEY: NameToken = NameToken("transformation")
    }
}


