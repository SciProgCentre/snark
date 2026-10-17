package space.kscience.snark.html.document

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name

@Serializable
public sealed interface DocumentFragment {
    public val meta: Meta
}

@Serializable
@SerialName("markup")
public class MarkupDocumentFragment(
    public val text: String,
    override val meta: Meta = Meta.EMPTY,
) : DocumentFragment

@Serializable
@SerialName("image")
public class ImageDocumentFragment(
    public val ref: String,
    override val meta: Meta = Meta.EMPTY,
) : DocumentFragment

@Serializable
@SerialName("data")
public class DataDocumentFragment(
    public val name: Name,
    override val meta: Meta = Meta.EMPTY,
) : DocumentFragment

@Serializable
@SerialName("list")
public class ListDocumentFragment(
    public val fragments: List<DocumentFragment>,
    override val meta: Meta = Meta.EMPTY,
) : DocumentFragment

@Serializable
@SerialName("layout")
public class LayoutDocumentFragment(
    public val fragments: Map<String, DocumentFragment>,
    override val meta: Meta = Meta.EMPTY,
) : DocumentFragment
