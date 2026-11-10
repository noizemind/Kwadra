package app.kwadra

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTest {
    @Test
    fun rejectsOccupiedAndOffBoard() {
        val board = Engine.emptyBoard().map { it.toMutableList() }
        val piece = piece(listOf(Cell(0, 0), Cell(0, 1)))
        assertFalse(Engine.canPlace(board, piece, 0, 7))
        assertTrue(Engine.canPlace(board, piece, 0, 6))
        board[0][6] = 1
        assertFalse(Engine.canPlace(board, piece, 0, 6))
        assertNull(Engine.placePiece(Engine.menuState(0), 0, 0, 0) { 0.0 })
    }

    @Test
    fun clearsRowAndColumnTogether() {
        val board = Engine.emptyBoard().map { it.toMutableList() }
        for (c in 0 until 8) board[2][c] = 1
        for (r in 0 until 8) board[r][4] = 1
        val grid = board.map { it.toList() }
        val clears = Engine.findClears(grid)
        assertEquals(listOf(2), clears.rows)
        assertEquals(listOf(4), clears.cols)
        val next = Engine.applyClears(grid, clears)
        assertEquals(0, next[2][0])
        assertEquals(0, next[0][4])
        assertEquals(0, next[1][0])
    }

    @Test
    fun placeScoresCombo() {
        Shapes.resetIds()
        val state = Engine.newGame(0) { 0.1 }.copy(
            board = Engine.emptyBoard().mapIndexed { r, row ->
                row.mapIndexed { c, _ ->
                    when {
                        r == 0 && c < 7 -> 2
                        r == 3 && c == 3 -> 2
                        else -> 0
                    }
                }
            },
            tray = listOf(piece(listOf(Cell(0, 0))), null, null),
        )
        val result = Engine.placePiece(state.copy(tray = listOf(piece(listOf(Cell(0, 0))), state.tray[1], state.tray[2])), 0, 0, 7) { 0.2 }
        assertNotNull(result)
        assertEquals(listOf(0), result!!.clears.rows)
        assertEquals(1, result.combo)
        assertEquals(0, result.next.board[0][0])
        assertEquals(2, result.next.board[3][3])
        assertEquals(110, result.points)
    }

    @Test
    fun deadlockLeavesAHoleForADot() {
        val board = List(8) { r -> List(8) { c -> if (r == 0 && c == 0) 0 else 1 } }
        val big = piece(listOf(Cell(0, 0), Cell(0, 1), Cell(1, 0), Cell(1, 1)))
        assertFalse(Engine.fitsAnywhere(board, big))
        assertTrue(Engine.isDeadlock(board, listOf(big, null, null)))
        assertTrue(Engine.fitsAnywhere(board, piece(listOf(Cell(0, 0)))))
        assertFalse(Engine.isDeadlock(board, listOf(null, null, null)))
    }

    @Test
    fun perfectClearBonus() {
        assertFalse(Engine.isEmpty(Engine.stamp(Engine.emptyBoard(), piece(listOf(Cell(0, 0))), 0, 0)))
        val full = Engine.emptyBoard().mapIndexed { r, row -> if (r == 0) List(8) { 1 } else row }
        assertTrue(Engine.isEmpty(Engine.applyClears(full, Engine.findClears(full))))
        assertEquals(680, Engine.scorePlace(8, 1, 1, true))
        assertEquals(280, Engine.scorePlace(4, 2, 1, false))
    }

    @Test
    fun newGameIsAlwaysPlaceable() {
        val random = kotlin.random.Random(1)
        repeat(30) {
            val state = Engine.newGame(0) { random.nextDouble() }
            assertFalse(Engine.isDeadlock(state.board, state.tray))
            assertEquals(3, state.tray.count { it != null })
        }
    }

    @Test
    fun thirdPieceRefillsTheTray() {
        Shapes.resetIds()
        val state = Engine.newGame(0) { 0.0 }
        val a = Engine.placePiece(state, 0, 0, 0) { 0.0 }!!
        val b = Engine.placePiece(a.next, 1, 0, 1) { 0.0 }!!
        val c = Engine.placePiece(b.next, 2, 0, 2) { 0.0 }!!
        assertTrue(c.refilled)
        assertTrue(c.next.tray.all { it != null })
        assertFalse(c.gameOver)
    }

    @Test
    fun catalogMatchesTheWebShapes() {
        assertEquals(58, Shapes.orientationCount())
        val dot = Shapes.spawn { 0.0 }
        assertEquals(1, dot.w)
        assertEquals(1, dot.h)
        assertEquals(listOf(Cell(0, 0)), dot.cells)
    }

    private fun piece(cells: List<Cell>): Piece {
        val w = cells.maxOf { it.c } + 1
        val h = cells.maxOf { it.r } + 1
        return Piece(1, 0, cells, w, h)
    }
}
