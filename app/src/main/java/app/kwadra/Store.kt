package app.kwadra

import android.content.Context

data class Settings(
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val theme: String = "system",
    val locale: String = "system",
)

class Store(context: Context) {
    private val prefs = context.getSharedPreferences("kwadra", Context.MODE_PRIVATE)

    fun loadSettings(): Settings {
        val theme = prefs.getString("theme", "system") ?: "system"
        val locale = prefs.getString("locale", "system") ?: "system"
        return Settings(
            sound = prefs.getBoolean("sound", true),
            haptics = prefs.getBoolean("haptics", true),
            theme = if (theme == "light" || theme == "dark" || theme == "system") theme else "system",
            locale = if (locale == "system" || locale in Texts.locales) locale else "system",
        )
    }

    fun saveSettings(settings: Settings) {
        prefs.edit()
            .putBoolean("sound", settings.sound)
            .putBoolean("haptics", settings.haptics)
            .putString("theme", settings.theme)
            .putString("locale", settings.locale)
            .apply()
    }

    fun hintSeen() = prefs.getBoolean("hint", false)

    fun setHintSeen() {
        prefs.edit().putBoolean("hint", true).apply()
    }

    fun loadBest(): Int = maxOf(prefs.getInt("best", 0), decode(prefs.getString("game", null))?.best ?: 0)

    fun saveBest(best: Int) {
        prefs.edit().putInt("best", best).apply()
    }

    fun loadGame(): GameState? {
        val state = decode(prefs.getString("game", null)) ?: return null
        if (state.status != "playing") return null
        return state
    }

    fun saveGame(state: GameState) {
        val edit = prefs.edit()
        if (state.status != "playing") edit.remove("game")
        else edit.putString("game", encode(state))
        edit.putInt("best", maxOf(prefs.getInt("best", 0), state.best))
        edit.apply()
    }

    fun encode(state: GameState): String = buildString {
        append(state.score).append('|').append(state.combo).append('|').append(state.best).append('|')
        state.board.forEachIndexed { r, row ->
            row.forEachIndexed { c, v ->
                if (r + c > 0) append(',')
                append(v)
            }
        }
        for (piece in state.tray) {
            append('|')
            if (piece == null) {
                append('-')
                continue
            }
            append(piece.id).append(':').append(piece.color).append(':')
            append(piece.w).append(':').append(piece.h).append(':')
            piece.cells.forEachIndexed { i, cell ->
                if (i > 0) append('/')
                append(cell.r).append('.').append(cell.c)
            }
        }
    }

    fun decode(line: String?): GameState? {
        if (line.isNullOrEmpty()) return null
        val parts = line.split('|')
        if (parts.size != 7) return null
        val score = parts[0].toIntOrNull() ?: return null
        val combo = parts[1].toIntOrNull() ?: return null
        val best = parts[2].toIntOrNull() ?: return null
        if (score < 0 || combo < 0 || best < 0) return null
        val cells = parts[3].split(',')
        if (cells.size != Engine.BOARD * Engine.BOARD) return null
        val board = List(Engine.BOARD) { r ->
            List(Engine.BOARD) { c ->
                val v = cells[r * Engine.BOARD + c].toIntOrNull() ?: return null
                if (v !in 0..Shapes.COLOR_COUNT) return null
                v
            }
        }
        val tray = (0 until Engine.TRAY).map { decodePiece(parts[4 + it]) ?: if (parts[4 + it] == "-") null else return null }
        return GameState(board, tray, score, combo, best, "playing")
    }

    private fun decodePiece(text: String): Piece? {
        if (text == "-") return null
        val head = text.split(':')
        if (head.size != 5) return null
        val id = head[0].toIntOrNull() ?: return null
        val color = head[1].toIntOrNull() ?: return null
        val w = head[2].toIntOrNull() ?: return null
        val h = head[3].toIntOrNull() ?: return null
        if (color !in 0 until Shapes.COLOR_COUNT || w !in 1..Engine.BOARD || h !in 1..Engine.BOARD) return null
        if (head[4].isEmpty()) return null
        val cells = head[4].split('/').map { pair ->
            val rc = pair.split('.')
            if (rc.size != 2) return null
            val r = rc[0].toIntOrNull() ?: return null
            val c = rc[1].toIntOrNull() ?: return null
            if (r !in 0 until h || c !in 0 until w) return null
            Cell(r, c)
        }
        return Piece(id, color, cells, w, h)
    }
}
