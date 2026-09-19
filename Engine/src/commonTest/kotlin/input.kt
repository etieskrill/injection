import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.KeyEvent
import org.etieskrill.engine.input.KeyEventAction
import org.etieskrill.engine.input.ModifierKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class Test {

    @Test
    fun `Input events are equal`() = assertEquals(
        KeyEvent(Key.A, KeyEventAction.PRESS, emptyList()),
        KeyEvent(Key.A, KeyEventAction.PRESS, emptyList())
    )

    @Test
    fun `Input events with modifiers are equal`() = assertEquals(
        KeyEvent(Key.A, KeyEventAction.PRESS, listOf(ModifierKey.CONTROL)),
        KeyEvent(Key.A, KeyEventAction.PRESS, listOf(ModifierKey.CONTROL))
    )

    @Test
    fun `Input events with different modifiers are not equal`() = assertNotEquals(
        KeyEvent(Key.A, KeyEventAction.PRESS, listOf(ModifierKey.CONTROL)),
        KeyEvent(Key.A, KeyEventAction.PRESS, listOf())
    )

    @Test
    fun `Modifier key input events without modifiers are equal`() = assertEquals(
        KeyEvent(Key.CTRL, KeyEventAction.PRESS, listOf()),
        KeyEvent(Key.CTRL, KeyEventAction.PRESS, listOf())
    )

    @Test
    fun `Modifier key input events with same modifiers are equal`() = assertEquals(
        KeyEvent(Key.CTRL, KeyEventAction.PRESS, listOf(ModifierKey.CONTROL)),
        KeyEvent(Key.CTRL, KeyEventAction.PRESS, listOf(ModifierKey.CONTROL))
    )

    @Test
    fun `Modifier key input events with different modifiers are equal`() = assertEquals(
        KeyEvent(Key.CTRL, KeyEventAction.PRESS, listOf()),
        KeyEvent(Key.CTRL, KeyEventAction.PRESS, listOf(ModifierKey.CONTROL))
    )

    @Test
    fun `Modifier keys equal their aliases`() = assertEquals(
        KeyEvent(Key.CTRL, KeyEventAction.PRESS, listOf()),
        KeyEvent(Key.LEFT_CONTROL, KeyEventAction.PRESS, listOf()),
    )

    @Test
    fun `Modifier keys equal their aliases with modifiers`() = assertEquals(
        KeyEvent(Key.CTRL, KeyEventAction.PRESS, listOf(ModifierKey.ALT)),
        KeyEvent(Key.LEFT_CONTROL, KeyEventAction.PRESS, listOf(ModifierKey.ALT)),
    )

    @Test
    fun `Modifier keys equal their aliases with different modifiers`() {
        assertEquals(
            KeyEvent(Key.CTRL, KeyEventAction.PRESS, listOf()),
            KeyEvent(Key.LEFT_CONTROL, KeyEventAction.PRESS, listOf(ModifierKey.SUPER))
        )
        assertEquals(
            KeyEvent(Key.CTRL, KeyEventAction.PRESS, listOf(ModifierKey.CONTROL)),
            KeyEvent(Key.LEFT_CONTROL, KeyEventAction.PRESS, listOf(ModifierKey.CONTROL))
        )
        assertEquals(
            KeyEvent(Key.CTRL, KeyEventAction.PRESS, listOf()),
            KeyEvent(Key.LEFT_CONTROL, KeyEventAction.PRESS, listOf(ModifierKey.CONTROL))
        )
    }

    @Test
    fun `Modifier key hash codes equal their aliases with any modifiers`() {
        assertEquals(
            KeyEvent(Key.CTRL, KeyEventAction.PRESS, listOf()).hashCode(),
            KeyEvent(Key.LEFT_CONTROL, KeyEventAction.PRESS, listOf(ModifierKey.SUPER)).hashCode()
        )
        assertEquals(
            KeyEvent(Key.CTRL, KeyEventAction.PRESS, listOf(ModifierKey.CONTROL)).hashCode(),
            KeyEvent(Key.LEFT_CONTROL, KeyEventAction.PRESS, listOf(ModifierKey.CONTROL)).hashCode()
        )
        assertEquals(
            KeyEvent(Key.CTRL, KeyEventAction.PRESS, listOf()).hashCode(),
            KeyEvent(Key.LEFT_CONTROL, KeyEventAction.PRESS, listOf(ModifierKey.CONTROL)).hashCode()
        )
    }

}
