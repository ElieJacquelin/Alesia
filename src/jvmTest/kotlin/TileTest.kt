import io.mockk.InternalPlatformDsl.toArray
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalStdlibApi
@ExperimentalUnsignedTypes

class TileTest {
    lateinit var tile: Tile

    private fun initTile(bytes: Array<UByte>): Array<Array<UByte>> {
        val result = mutableListOf<Array<UByte>>()
        for (i in bytes.indices step 2) {
            result.add(arrayOf(bytes[i], bytes[i+1]))
        }
        return result.toTypedArray()
    }

    @Test
    fun `Get correct ColorIDs from tile`() {
        Array(160) {Array<Pixel>(144) { Pixel(0, 0, false) } }
        // Based on https://www.huderlem.com/demos/gameboy2bpp.html
        val pixels = initTile(arrayOf(0x7Cu, 0x7Cu, 0x00u, 0xC6u, 0xC6u, 0x00u, 0x00u, 0xFEu, 0xC6u, 0xC6u, 0x00u, 0xC6u, 0xC6u, 0x00u, 0x00u, 0x00u))
        val expectedColorID = arrayOf(
            arrayOf(ColorID.ZERO, ColorID.THREE,ColorID.THREE,ColorID.THREE,ColorID.THREE,ColorID.THREE, ColorID.ZERO,ColorID.ZERO),
            arrayOf(ColorID.TWO, ColorID.TWO, ColorID.ZERO, ColorID.ZERO, ColorID.ZERO, ColorID.TWO, ColorID.TWO, ColorID.ZERO),
            arrayOf(ColorID.ONE, ColorID.ONE, ColorID.ZERO,ColorID.ZERO,ColorID.ZERO, ColorID.ONE,ColorID.ONE,ColorID.ZERO),
            arrayOf(ColorID.TWO, ColorID.TWO, ColorID.TWO, ColorID.TWO, ColorID.TWO, ColorID.TWO, ColorID.TWO, ColorID.ZERO),
            arrayOf(ColorID.THREE, ColorID.THREE, ColorID.ZERO, ColorID.ZERO, ColorID.ZERO, ColorID.THREE, ColorID.THREE, ColorID.ZERO),
            arrayOf(ColorID.TWO, ColorID.TWO, ColorID.ZERO, ColorID.ZERO, ColorID.ZERO, ColorID.TWO, ColorID.TWO, ColorID.ZERO),
            arrayOf(ColorID.ONE, ColorID.ONE, ColorID.ZERO,ColorID.ZERO,ColorID.ZERO, ColorID.ONE,ColorID.ONE,ColorID.ZERO),
            arrayOf(ColorID.ZERO, ColorID.ZERO, ColorID.ZERO,ColorID.ZERO,ColorID.ZERO, ColorID.ZERO,ColorID.ZERO,ColorID.ZERO),
        )

        tile = Tile(pixels)
        for(x in 0..7) {
            for(y in 0..7) {
                assertEquals(expectedColorID[x][y], tile.getColorId(x, y))
            }
        }
    }



}