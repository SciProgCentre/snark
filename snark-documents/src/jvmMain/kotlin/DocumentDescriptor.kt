package space.kscience.snark.html.document

import space.kscience.dataforge.meta.*
import space.kscience.dataforge.misc.DFExperimental


public class Author : Scheme() {
    public var name: String by string { error("Name is required") }
    public var affiliation: String? by string()

    public companion object : SchemeSpec<Author>(::Author)
}

public class DocumentDescriptor : Scheme() {

    public var route: String? by string()

    public var title: String? by string()

    public var documentMeta: Meta? by node()

    public var authors: List<Author> by listOfScheme(Author)

    /** Stylesheets linked in the page head, relative to the document directory. */
    public var stylesheets: List<String>? by stringList()

    /** Scripts loaded in the page head (deferred), relative to the document directory. */
    public var scripts: List<String>? by stringList()

    @OptIn(DFExperimental::class)
    public var fragments: List<DocumentFragment> by meta.listOfSerializable<DocumentFragment>()

    public companion object : SchemeSpec<DocumentDescriptor>(::DocumentDescriptor)
}