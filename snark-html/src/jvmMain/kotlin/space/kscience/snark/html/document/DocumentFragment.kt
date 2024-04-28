package space.kscience.snark.html.document

import kotlinx.io.files.Path
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name

public sealed interface DocumentFragment{
    public val meta: Meta
}

public class MarkupDocumentFragment(public val text: String, override val meta: Meta) : DocumentFragment

public class ImageDocumentFragment(public val image: Path, override val meta: Meta) : DocumentFragment

public class DataDocumentFragment(public val dataName: Name, override val meta: Meta) : DocumentFragment

public class ListDocumentFragment(public val fragments: List<DocumentFragment>, override val meta: Meta) : DocumentFragment

public class LayoutDocumentFragment(public val fragments: Map<String,DocumentFragment>, override val meta: Meta) : DocumentFragment
