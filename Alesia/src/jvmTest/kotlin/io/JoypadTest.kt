package io

import kotlin.test.*


internal class JoypadTest {
    lateinit var joypad: Joypad

    @BeforeTest
    fun setUp() {
        joypad = Joypad()
    }

    private fun setAllKeys(pressed: Boolean) {
        joypad.aKeyPressed = pressed
        joypad.bKeyPressed = pressed
        joypad.startKeyPressed = pressed
        joypad.selectKeyPressed = pressed
        joypad.upKeyPressed = pressed
        joypad.downKeyPressed = pressed
        joypad.rightKeyPressed = pressed
        joypad.leftKeyPressed = pressed
    }

    @Test
    fun `Pressing a key sets the corresponding field`() {
        // Given all keys are initially not pressed
        setAllKeys(false)

        for (key in Joypad.Key.values()) {
            // When a key is pressed
            joypad.handleKey(key, true)

            // Then the key field is set
            val keyField = when (key) {
                Joypad.Key.A -> joypad.aKeyPressed
                Joypad.Key.B -> joypad.bKeyPressed
                Joypad.Key.Start -> joypad.startKeyPressed
                Joypad.Key.Select -> joypad.selectKeyPressed
                Joypad.Key.Up -> joypad.upKeyPressed
                Joypad.Key.Down -> joypad.downKeyPressed
                Joypad.Key.Left -> joypad.leftKeyPressed
                Joypad.Key.Right -> joypad.rightKeyPressed
            }
            assertTrue(keyField)
        }
    }

    @Test
    fun `Releasing a key sets the corresponding field`() {
        // Given all keys are initially pressed
        setAllKeys(true)

        for (key in Joypad.Key.values()) {
            // When a key is released
            joypad.handleKey(key, false)

            // Then the key field is unset
            val keyField = when (key) {
                Joypad.Key.A -> joypad.aKeyPressed
                Joypad.Key.B -> joypad.bKeyPressed
                Joypad.Key.Start -> joypad.startKeyPressed
                Joypad.Key.Select -> joypad.selectKeyPressed
                Joypad.Key.Up -> joypad.upKeyPressed
                Joypad.Key.Down -> joypad.downKeyPressed
                Joypad.Key.Left -> joypad.leftKeyPressed
                Joypad.Key.Right -> joypad.rightKeyPressed
            }
            assertFalse(keyField)
        }
    }

    @Test
    fun `Keys are mapped to the correct bit - A`() {
        // Given joypad control wants to fetch action buttons
        val joypadControl = (0b0001_0000u).toUByte()
        // And the A key is pressed
        joypad.aKeyPressed = true

        // When the joypad value is fetched
        val joypadValue = joypad.generateJoypadValue(joypadControl)

        // Then the value returns the A key is pressed
        assertEquals(0b0001_1110u, joypadValue)
    }

    @Test
    fun `Keys are mapped to the correct bit - B`() {
        // Given joypad control wants to fetch action buttons
        val joypadControl = (0b0001_0000u).toUByte()
        // And the B key is pressed
        joypad.bKeyPressed = true

        // When the joypad value is fetched
        val joypadValue = joypad.generateJoypadValue(joypadControl)

        // Then the value returns the B key is pressed
        assertEquals(0b0001_1101u, joypadValue)
    }

    @Test
    fun `Keys are mapped to the correct bit - Select`() {
        // Given joypad control wants to fetch action buttons
        val joypadControl = (0b0001_0000u).toUByte()
        // And the Select key is pressed
        joypad.selectKeyPressed = true

        // When the joypad value is fetched
        val joypadValue = joypad.generateJoypadValue(joypadControl)

        // Then the value returns the Select key is pressed
        assertEquals(0b0001_1011u, joypadValue)
    }

    @Test
    fun `Keys are mapped to the correct bit - Start`() {
        // Given joypad control wants to fetch action buttons
        val joypadControl = (0b0001_0000u).toUByte()
        // And the Start key is pressed
        joypad.startKeyPressed = true

        // When the joypad value is fetched
        val joypadValue = joypad.generateJoypadValue(joypadControl)

        // Then the value returns the Start key is pressed
        assertEquals(0b0001_0111u, joypadValue)
    }

    @Test
    fun `Keys are mapped to the correct bit - Right`() {
        // Given joypad control wants to fetch direction buttons
        val joypadControl = (0b0010_0000u).toUByte()
        // And the Right key is pressed
        joypad.rightKeyPressed = true

        // When the joypad value is fetched
        val joypadValue = joypad.generateJoypadValue(joypadControl)

        // Then the value returns the Right key is pressed
        assertEquals(0b0010_1110u, joypadValue)
    }

    @Test
    fun `Keys are mapped to the correct bit - Left`() {
        // Given joypad control wants to fetch direction buttons
        val joypadControl = (0b0010_0000u).toUByte()
        // And the Left key is pressed
        joypad.leftKeyPressed = true

        // When the joypad value is fetched
        val joypadValue = joypad.generateJoypadValue(joypadControl)

        // Then the value returns the Left key is pressed
        assertEquals(0b0010_1101u, joypadValue)
    }

    @Test
    fun `Keys are mapped to the correct bit - Up`() {
        // Given joypad control wants to fetch direction buttons
        val joypadControl = (0b0010_0000u).toUByte()
        // And the Up key is pressed
        joypad.upKeyPressed = true

        // When the joypad value is fetched
        val joypadValue = joypad.generateJoypadValue(joypadControl)

        // Then the value returns the Up key is pressed
        assertEquals(0b0010_1011u, joypadValue)
    }

    @Test
    fun `Keys are mapped to the correct bit - Down`() {
        // Given joypad control wants to fetch direction buttons
        val joypadControl = (0b0010_0000u).toUByte()
        // And the Down key is pressed
        joypad.downKeyPressed = true

        // When the joypad value is fetched
        val joypadValue = joypad.generateJoypadValue(joypadControl)

        // Then the value returns the Down key is pressed
        assertEquals(0b0010_0111u, joypadValue)
    }

    @Test
    fun `Multiple keys can be pressed at the same time`() {
        // Given joypad control wants to fetch direction buttons
        val joypadControl = (0b0010_0000u).toUByte()
        // And the Down key is pressed
        joypad.downKeyPressed = true
        // And the Right key is pressed
        joypad.rightKeyPressed = true

        // When the joypad value is fetched
        val joypadValue = joypad.generateJoypadValue(joypadControl)

        // Then the value returns the Down key and Right key is pressed
        assertEquals(0b0010_0110u, joypadValue)
    }

    @Test
    fun `JoypadControl can fetch action buttons`() {
        // Given joypad control wants to fetch action buttons
        val joypadControl = (0b0001_0000u).toUByte()
        // And the A button is pressed
        joypad.aKeyPressed = true
        // And the Up button is pressed
        joypad.upKeyPressed = true

        // When the joypad value is fetched
        val joypadValue = joypad.generateJoypadValue(joypadControl)

        // Then the value returns the A key is pressed and ignore the up button
        assertEquals(0b0001_1110u, joypadValue)
    }

    @Test
    fun `JoypadControl can fetch direction buttons`() {
        // Given joypad control wants to fetch direction buttons
        val joypadControl = (0b0010_0000u).toUByte()
        // And the A button is pressed
        joypad.aKeyPressed = true
        // And the Up button is pressed
        joypad.upKeyPressed = true

        // When the joypad value is fetched
        val joypadValue = joypad.generateJoypadValue(joypadControl)

        // Then the value returns the Up key is pressed and ignore the A button
        assertEquals(0b0010_1011u, joypadValue)
    }

    @Test
    fun `Joypad value is 0xFF if joypad controls does not set intent`() {
        // Given joypad control does not desire to fetch any control in particular
        val joypadControl = (0b0011_0000u).toUByte()
        // And the A button is pressed
        joypad.aKeyPressed = true
        // And the Up button is pressed
        joypad.upKeyPressed = true

        // When the joypad value is fetched
        val joypadValue = joypad.generateJoypadValue(joypadControl)

        // Then the value returns 0xFF to pretend no keys are pressed
        assertEquals(0xFFu, joypadValue)
    }
}