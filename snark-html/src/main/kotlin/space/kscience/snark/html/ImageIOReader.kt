package space.kscience.snark.html

import io.ktor.util.asStream
import io.ktor.utils.io.core.Input
import space.kscience.dataforge.io.IOReader
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.reflect.KType
import kotlin.reflect.typeOf

internal object ImageIOReader : IOReader<BufferedImage> {
    override val type: KType get() = typeOf<BufferedImage>()

    override fun readObject(input: Input): BufferedImage = ImageIO.read(input.asStream())
}