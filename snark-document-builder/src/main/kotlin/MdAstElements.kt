package documentBuilder

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@Serializable
public data class Point(val line: Int, val column: Int, val offset: Int)

@Serializable
public data class Position(val start: Point, val end: Point)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)

@JsonSubTypes(
    JsonSubTypes.Type(value = MdAstRoot::class, name = "root"),
    JsonSubTypes.Type(value = MdAstParagraph::class, name = "paragraph"),
    JsonSubTypes.Type(value = MdAstText::class, name = "text"),
    JsonSubTypes.Type(value = MdAstHeading::class, name = "heading"),
    JsonSubTypes.Type(value = MdAstCode::class, name = "code"),
    JsonSubTypes.Type(value = MdAstBlockquote::class, name = "blockquote")
)

@Serializable
public sealed interface MdAstElement{
    public abstract var position: Position
}

@Serializable
public sealed interface MdAstParent: MdAstElement{
    public var children: List<MdAstElement>
}

@Serializable
@SerialName("root")
public data class MdAstRoot(
    override var children: List<MdAstElement>,
    override var position: Position
): MdAstParent


@Serializable
@SerialName("paragraph")
public data class MdAstParagraph(
    override var children: List<MdAstElement>,
    override var position: Position
): MdAstParent


@Serializable
@SerialName("text")
public data class MdAstText(
    val value: String,
    override var position: Position
): MdAstElement

@Serializable
@SerialName("heading")
public data class MdAstHeading(
    val depth: Int, 
    override var children: List<MdAstElement>,
    override var position: Position
): MdAstParent

@Serializable
@SerialName("code")
public data class MdAstCode(
    var lang: String? = null,
    var meta: String? = null,
    var value: String,
    override var position: Position,
) : MdAstElement

@Serializable
@SerialName("blockquote")
public data class MdAstBlockquote(
    override var children: List<MdAstElement>,
    override var position: Position
): MdAstParent