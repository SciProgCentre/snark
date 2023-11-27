package space.kscience.snark

import kotlinx.io.readByteArray
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.PluginFactory
import space.kscience.dataforge.context.PluginTag
import space.kscience.dataforge.context.gather
import space.kscience.dataforge.io.IOPlugin
import space.kscience.dataforge.io.IOReader
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.parseAsName
import space.kscience.dataforge.workspace.WorkspacePlugin

/**
 * Represents a Snark workspace plugin.
 */
public class Snark : WorkspacePlugin() {
    public val io: IOPlugin by require(IOPlugin)
    override val tag: PluginTag get() = Companion.tag

    public val readers: Map<Name, SnarkIOReader<Any>> by lazy {
        context.gather<SnarkIOReader<Any>>(SnarkIOReader.DF_TYPE, inherit = true)
    }

    /**
     * A lazy-initialized map of `TextProcessor` instances used for page-based text transformation.
     *
     * @property textProcessors The `TextProcessor` instances accessible by their names.
     */
    public val textProcessors: Map<Name, TextProcessor> by lazy {
        context.gather(TextProcessor.DF_TYPE, true)
    }

    public fun textProcessor(transformationMeta: Meta): TextProcessor {
        val transformationName = transformationMeta.string
            ?: transformationMeta["name"].string ?: error("Transformation name not defined in $transformationMeta")
        return textProcessors[transformationName.parseAsName()]
            ?: error("Text transformation with name $transformationName not found in $this")
    }

    public companion object : PluginFactory<Snark> {
        override val tag: PluginTag = PluginTag("snark")

        override fun build(context: Context, meta: Meta): Snark = Snark()

        private val byteArrayIOReader = IOReader {
            readByteArray()
        }

        internal val byteArraySnarkParser = SnarkIOReader(byteArrayIOReader)

    }
}