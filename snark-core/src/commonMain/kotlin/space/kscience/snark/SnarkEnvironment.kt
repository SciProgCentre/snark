package space.kscience.snark

import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Global
import space.kscience.dataforge.context.Plugin
import space.kscience.dataforge.data.DataSourceBuilder
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.DataTreeBuilder
import space.kscience.dataforge.meta.MutableMeta
import kotlin.reflect.typeOf

public class SnarkEnvironment(public val parentContext: Context) {
    private var _data: DataTree<*>? = null
    public val data: DataTree<Any> get() = _data ?: DataTree.empty()

    public fun data(builder: DataSourceBuilder<Any>.() -> Unit) {
        _data = DataTreeBuilder<Any>(typeOf<Any>(), parentContext.coroutineContext).apply(builder)
        //TODO use node meta
    }

    public val meta: MutableMeta = MutableMeta()

    public fun meta(block: MutableMeta.() -> Unit) {
        meta.apply(block)
    }

    private val _plugins = HashSet<Plugin>()
    public val plugins: Set<Plugin> get() = _plugins

    public fun registerPlugin(plugin: Plugin) {
        _plugins.add(plugin)
    }

    public companion object{
        public val default: SnarkEnvironment = SnarkEnvironment(Global)
    }
}

public fun SnarkEnvironment(parentContext: Context = Global, block: SnarkEnvironment.() -> Unit): SnarkEnvironment =
    SnarkEnvironment(parentContext).apply(block)
