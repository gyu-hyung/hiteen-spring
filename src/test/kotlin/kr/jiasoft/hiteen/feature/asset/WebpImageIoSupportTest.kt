package kr.jiasoft.hiteen.feature.asset

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import javax.imageio.ImageIO

class WebpImageIoSupportTest {

    @Test
    fun `ImageIO has webp writer`() {
        val writers = ImageIO.getImageWritersByFormatName("webp")
        assertTrue(writers.hasNext(), "No ImageIO webp writer found. Ensure com.luciad.imageio:webp-imageio is on classpath.")
    }
}

