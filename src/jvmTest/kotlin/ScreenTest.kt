import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalStdlibApi
@ExperimentalUnsignedTypes


internal class ScreenTest {

    lateinit var memory: Memory
    lateinit var screen: Screen

    @BeforeTest
    fun setUp() {
        memory = Memory()
        screen = Screen(memory)
    }

    private fun storeTile(baseAddress: UShort) {
        for(i in baseAddress..(baseAddress+15u).toUShort()) {
            memory.set(i.toUShort(), 0xFFu)
        }
    }


    @Test
    fun `Get first tile data - 8000 method`() {
        // Given a tile has been stored on the first ID
        storeTile(0x8000u)
        // When tiles are generated
        screen.generateTiles()

        validateSprite(0, false)
    }

    @Test
    fun `Get last tile data - 8000 method`() {
        // Given a tile has been stored on the last ID
        storeTile(0x8FF0u)
        // When tiles are generated
        screen.generateTiles()

        validateSprite(255, false)
    }

    @Test
    fun `Get first tile data - 8800 method`() {
        // Given a tile has been stored on the first ID
        storeTile(0x9000u)
        // When tiles are generated
        screen.generateTiles()

        validateSprite(0, true)
    }

    @Test
    fun `Get last tile data - 8800 method`() {
        // Given a tile has been stored on the last ID
        storeTile(0x8FF0u)
        // When tiles are generated
        screen.generateTiles()

        validateSprite(255, true)
    }

    @Test
    fun `Get last tile data - 8800 method - lower boundary`() {
        // Given a tile has been stored before the boundary
        storeTile(0x97F0u)
        // When tiles are generated
        screen.generateTiles()

        validateSprite(127, true)
    }

    @Test
    fun `Get last tile data - 8800 method - higher boundary`() {
        // Given a tile has been stored after the boundary
        storeTile(0x8800u)
        // When tiles are generated
        screen.generateTiles()

        validateSprite(128, true)
    }

    private fun validateSprite(tileId: Int, `8800AddressMode`: Boolean) {
        //Then only one sprite has the ID
        assertEquals(1, screen.tiles.filter { tile -> tile.getTileId(`8800AddressMode`) == tileId }.size)
        val lastTile = screen.tiles.find { tile -> tile.getTileId(`8800AddressMode`) == tileId }!!
        // That sprite has 8 rows
        assertEquals(8, lastTile.pixels.size)
        // of 2 bytes each
        assertEquals(2, lastTile.pixels[0].size)
        // And all values are set accordingly
        for (row in lastTile.pixels) {
            for (pixel in row) {
                assertEquals(0xFFu, pixel)
            }
        }
    }

    @Test
    fun `Generate OAM`() {
        // Given the OAM data for the first sprite is stored
        memory.set(0xFE00u, 0xFAu)
        memory.set(0xFE01u, 0xFBu)
        memory.set(0xFE02u, 0xFCu)
        memory.set(0xFE03u, 0xFDu)

        // When the OAM is generated
        screen.generateOAM()

        // Then the OAM has 40 items
        assertEquals(40, screen.OAM.size)
        // And the first sprite has the relevant data
        assertEquals(0xFAu, screen.OAM[0].yPos)
        assertEquals(0xFBu, screen.OAM[0].XPos)
        assertEquals(0xFCu, screen.OAM[0].tileIndex)
        assertEquals(0xFDu, screen.OAM[0].attributesFlags)
    }
}