package space.kscience.snark.html.document

import kotlinx.io.files.Path
import space.kscience.dataforge.meta.Meta

public sealed interface DocumentFragment{
    public val meta: Meta
}

public class TextDocumentFragment(public val text: String, override val meta: Meta) : DocumentFragment

public class ImageDocumentFragment(public val image: Path, override val meta: Meta) : DocumentFragment

public class CompositeDocumentFragment(public val fragments: List<DocumentFragment>, override val meta: Meta) : DocumentFragment
