package space.kscience.snark.html.language

import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.*
import space.kscience.snark.html.PageContext

/**
 * Represents a string that can be localized based on the language or the context of a page.
 * The actual value of the string is determined by the implementing classes of this interface.
 */
public interface LocalizedString {

    context(page: PageContext)
    public fun value(): String

    public companion object {
        public val LOCALIZATION_KEY: Name = NameToken("localization").asName()

        public fun fixed(string: String): LocalizedString = FixedString(string)

        public fun byLanguage(
            values: Map<String, String>,
            defaultLanguage: String = values.keys.first()
        ): LocalizedString = InPlaceLocalizedString(values, defaultLanguage)

        public fun byLanguage(
            vararg values: Pair<String, String>,
        ): LocalizedString = InPlaceLocalizedString(values.toMap())


        public fun byResource(
            name: Name,
            defaultValue: String? = null
        ): LocalizedString = PageLocalizedString(name, defaultValue)

        public fun byResource(
            name: String,
            defaultValue: String? = null
        ): LocalizedString = PageLocalizedString(name.parseAsName(), defaultValue)

    }
}

@JvmInline
public value class FixedString(public val string: String) : LocalizedString {
    context(page: PageContext)
    override fun value(): String = string
}

public class InPlaceLocalizedString(
    public val values: Map<String, String>,
    public val defaultLanguage: String = values.keys.first()
) : LocalizedString {

    init {
        require(values.isNotEmpty()) { "At least one language must be specified" }
    }

    context(page: PageContext)
    override fun value(): String =
        values[page.language] ?: values[defaultLanguage] ?: error("Incorrect value for language ${page.language}")
}

public class PageLocalizedString(
    public val resourceName: Name,
    public val defaultValue: String? = null
) : LocalizedString {

    context(page: PageContext)
    override fun value(): String =
        page.pageMeta[LocalizedString.LOCALIZATION_KEY + page.language + resourceName].string ?: defaultValue
        ?: error("No resource $resourceName for language ${page.language}")
}