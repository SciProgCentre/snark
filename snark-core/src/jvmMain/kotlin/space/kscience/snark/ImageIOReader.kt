package space.kscience.snark

import kotlinx.io.Source
import kotlinx.io.asInputStream
import space.kscience.dataforge.io.IOReader
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/**
 * The ImageIOReader class is an implementation of the IOReader interface specifically for reading images using the ImageIO library.
 * It reads the image data from a given source and returns a BufferedImage object.
 *
 * @property type The KType of the data to be read by the ImageIOReader.
 */
public object ImageIOReader : IOReader<BufferedImage> {
    override fun readFrom(source: Source): BufferedImage = ImageIO.read(source.asInputStream())
}