package app.kwadra

data class Cell(val r: Int, val c: Int)

data class Piece(
    val id: Int,
    val color: Int,
    val cells: List<Cell>,
    val w: Int,
    val h: Int,
)

data class GameState(
    val board: List<List<Int>>,
    val tray: List<Piece?>,
    val score: Int,
    val combo: Int,
    val best: Int,
    val status: String,
)

data class Clear(val rows: List<Int>, val cols: List<Int>) {
    val count: Int get() = rows.size + cols.size
    fun hits(r: Int, c: Int) = r in rows || c in cols
}

data class PlaceResult(
    val stamped: List<List<Int>>,
    val clears: Clear,
    val placed: List<Cell>,
    val points: Int,
    val combo: Int,
    val perfect: Boolean,
    val refilled: Boolean,
    val gameOver: Boolean,
    val next: GameState,
)

object Engine {
    const val BOARD = 8
    const val TRAY = 3

    fun emptyBoard(): List<List<Int>> = List(BOARD) { List(BOARD) { 0 } }

    fun canPlace(grid: List<List<Int>>, piece: Piece, row: Int, col: Int): Boolean {
        for (cell in piece.cells) {
            val r = row + cell.r
            val c = col + cell.c
            if (r < 0 || c < 0 || r >= BOARD || c >= BOARD) return false
            if (grid[r][c] != 0) return false
        }
        return true
    }

    fun stamp(grid: List<List<Int>>, piece: Piece, row: Int, col: Int): List<List<Int>> {
        val next = grid.map { it.toMutableList() }
        val color = piece.color + 1
        for (cell in piece.cells) next[row + cell.r][col + cell.c] = color
        return next.map { it.toList() }
    }

    fun findClears(grid: List<List<Int>>): Clear {
        val rows = (0 until BOARD).filter { r -> grid[r].all { it != 0 } }
        val cols = (0 until BOARD).filter { c -> (0 until BOARD).all { r -> grid[r][c] != 0 } }
        return Clear(rows, cols)
    }

    fun applyClears(grid: List<List<Int>>, clears: Clear): List<List<Int>> {
        if (clears.count == 0) return grid
        return List(BOARD) { r ->
            List(BOARD) { c -> if (clears.hits(r, c)) 0 else grid[r][c] }
        }
    }

    fun isEmpty(grid: List<List<Int>>): Boolean = grid.all { row -> row.all { it == 0 } }

    fun fitsAnywhere(grid: List<List<Int>>, piece: Piece): Boolean {
        for (r in 0..BOARD - piece.h) {
            for (c in 0..BOARD - piece.w) {
                if (canPlace(grid, piece, r, c)) return true
            }
        }
        return false
    }

    fun isDeadlock(grid: List<List<Int>>, tray: List<Piece?>): Boolean {
        val left = tray.filterNotNull()
        if (left.isEmpty()) return false
        return left.all { !fitsAnywhere(grid, it) }
    }

    fun scorePlace(cells: Int, lineCount: Int, combo: Int, perfect: Boolean): Int {
        var points = cells * 10
        if (lineCount > 0) {
            points += lineCount * 100 * combo
            if (lineCount >= 2) points += 40 * combo * (lineCount - 1)
        }
        if (perfect) points += 500
        return points
    }

    fun newGame(best: Int = 0, rand: () -> Double = { Math.random() }): GameState {
        val board = emptyBoard()
        return GameState(board, dealTray(board, rand), 0, 0, best, "playing")
    }

    fun menuState(best: Int = 0) = GameState(emptyBoard(), listOf(null, null, null), 0, 0, best, "menu")

    fun placePiece(
        state: GameState,
        slot: Int,
        row: Int,
        col: Int,
        rand: () -> Double = { Math.random() },
    ): PlaceResult? {
        val piece = state.tray.getOrNull(slot) ?: return null
        if (state.status != "playing") return null
        if (!canPlace(state.board, piece, row, col)) return null

        val stamped = stamp(state.board, piece, row, col)
        val clears = findClears(stamped)
        val cleared = applyClears(stamped, clears)
        val lineCount = clears.count
        val combo = if (lineCount > 0) state.combo + 1 else 0
        val perfect = lineCount > 0 && isEmpty(cleared)
        val points = scorePlace(piece.cells.size, lineCount, maxOf(combo, 1), perfect)
        val score = state.score + points
        val tray = state.tray.mapIndexed { i, p -> if (i == slot) null else p }
        val refilled = tray.all { it == null }
        val nextTray = if (refilled) dealTray(cleared, rand) else tray
        val gameOver = isDeadlock(cleared, nextTray)
        return PlaceResult(
            stamped = stamped,
            clears = clears,
            placed = piece.cells.map { Cell(row + it.r, col + it.c) },
            points = points,
            combo = combo,
            perfect = perfect,
            refilled = refilled,
            gameOver = gameOver,
            next = GameState(
                board = cleared,
                tray = nextTray,
                score = score,
                combo = combo,
                best = maxOf(state.best, score),
                status = if (gameOver) "gameover" else "playing",
            ),
        )
    }

    private fun dealTray(grid: List<List<Int>>, rand: () -> Double): List<Piece?> {
        repeat(24) {
            val tray = List(TRAY) { Shapes.spawn(rand) }
            if (tray.any { fitsAnywhere(grid, it) }) return tray
        }
        val dot = Shapes.spawn(rand)
        return listOf(dot.copy(cells = listOf(Cell(0, 0)), w = 1, h = 1), Shapes.spawn(rand), Shapes.spawn(rand))
    }
}

object Shapes {
    const val COLOR_COUNT = 6
    private var nextId = 1

    private class Ori(val cells: List<Cell>, val w: Int, val h: Int, val weight: Double)

    private val oris: List<Ori>
    private val weightSum: Double

    init {
        val weight = doubleArrayOf(
            0.7, 1.1, 1.2, 1.0, 0.45, 1.2, 0.55, 0.35, 1.1, 1.0, 0.85, 0.7, 1.0, 0.7, 0.9, 0.75, 0.65,
            0.5, 0.35, 0.7, 0.55, 0.5,
        )
        val shape = arrayOf(
            arrayOf(intArrayOf(1)),
            arrayOf(intArrayOf(1, 1)),
            arrayOf(intArrayOf(1, 1, 1)),
            arrayOf(intArrayOf(1, 1, 1, 1)),
            arrayOf(intArrayOf(1, 1, 1, 1, 1)),
            arrayOf(intArrayOf(1, 1), intArrayOf(1, 1)),
            arrayOf(intArrayOf(1, 1, 1), intArrayOf(1, 1, 1)),
            arrayOf(intArrayOf(1, 1, 1), intArrayOf(1, 1, 1), intArrayOf(1, 1, 1)),
            arrayOf(intArrayOf(1, 0), intArrayOf(1, 1)),
            arrayOf(intArrayOf(1, 1, 1), intArrayOf(1, 0, 0)),
            arrayOf(intArrayOf(1, 1, 1), intArrayOf(1, 0, 0), intArrayOf(1, 0, 0)),
            arrayOf(intArrayOf(1, 1, 1, 1), intArrayOf(1, 0, 0, 0)),
            arrayOf(intArrayOf(1, 1, 1), intArrayOf(0, 1, 0)),
            arrayOf(intArrayOf(0, 1, 0), intArrayOf(1, 1, 1), intArrayOf(0, 1, 0)),
            arrayOf(intArrayOf(1, 1, 0), intArrayOf(0, 1, 1)),
            arrayOf(intArrayOf(1, 1, 1), intArrayOf(1, 0, 1)),
            arrayOf(intArrayOf(1, 1), intArrayOf(1, 1), intArrayOf(1, 0)),
            arrayOf(intArrayOf(1, 0), intArrayOf(0, 1)),
            arrayOf(intArrayOf(1, 0, 0), intArrayOf(0, 1, 0), intArrayOf(0, 0, 1)),
            arrayOf(intArrayOf(1, 1, 1), intArrayOf(1, 1, 0)),
            arrayOf(intArrayOf(1, 1, 0, 0), intArrayOf(0, 0, 1, 1)),
            arrayOf(intArrayOf(1, 0, 1), intArrayOf(1, 1, 1)),
        )
        val built = ArrayList<Ori>()
        var sum = 0.0
        for (i in shape.indices) {
            for (turned in orientations(shape[i])) {
                built += Ori(toCells(turned), turned[0].size, turned.size, weight[i])
                sum += weight[i]
            }
        }
        oris = built
        weightSum = sum
    }

    fun orientationCount() = oris.size

    fun resetIds() {
        nextId = 1
    }

    fun spawn(rand: () -> Double): Piece {
        var roll = rand() * weightSum
        var picked = oris[0]
        for (ori in oris) {
            roll -= ori.weight
            if (roll <= 0) {
                picked = ori
                break
            }
        }
        var color = kotlin.math.floor(rand() * COLOR_COUNT).toInt()
        if (color < 0) color = 0
        if (color >= COLOR_COUNT) color = COLOR_COUNT - 1
        return Piece(nextId++, color, picked.cells, picked.w, picked.h)
    }

    private fun orientations(base: Array<IntArray>): List<Array<IntArray>> {
        val out = ArrayList<Array<IntArray>>()
        val seen = HashSet<String>()
        var cur = base
        repeat(4) {
            if (seen.add(key(cur))) out += cur
            cur = rotate(cur)
        }
        return out
    }

    private fun rotate(m: Array<IntArray>): Array<IntArray> {
        val h = m.size
        val w = m[0].size
        return Array(w) { c -> IntArray(h) { r -> m[h - 1 - r][c] } }
    }

    private fun key(m: Array<IntArray>) = m.joinToString("|") { row -> row.joinToString("") }

    private fun toCells(m: Array<IntArray>): List<Cell> {
        val cells = ArrayList<Cell>()
        for (r in m.indices) for (c in m[r].indices) if (m[r][c] != 0) cells += Cell(r, c)
        return cells
    }
}
