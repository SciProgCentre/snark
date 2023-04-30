import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

package documentBuilder

@Serializable
data class Point(val line: Int, val column: Int, val offset: Int)

@Serializable
data class Position(val start: Point, val end: Point)

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
sealed interface MdAstElement{
    abstract var position: Position
}

@Serializable
sealed interface MdAstParent: MdAstElement{
    var children: List<MdAstElement>
}

@Serializable
@SerialName("root")
data class MdAstRoot(
    override var children: List<MdAstElement>,
    override var position: Position
): MdAstParent


@Serializable
@SerialName("paragraph")
data class MdAstParagraph(
    override var children: List<MdAstElement>,
    override var position: Position
): MdAstParent


@Serializable
@SerialName("text")
data class MdAstText(
    val value: String,
    override var position: Position
): MdAstElement

@Serializable
@SerialName("heading")
data class MdAstHeading(
    val depth: Int, 
    override var children: List<MdAstElement>,
    override var position: Position
): MdAstParent

@Serializable
@SerialName("code")
data class MdAstCode(
    var lang: String? = null,
    var meta: String? = null,
    var value: String,
    override var position: Position,
) : MdAstElement

@Serializable
@SerialName("blockquote")
data class MdAstBlockquote(
    override var children: List<MdAstElement>,
    override var position: Position
): MdAstParent