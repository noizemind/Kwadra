package app.kwadra

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Floater(val text: String, val row: Int, val col: Int, val born: Long)
data class Spark(val row: Int, val col: Int, val dx: Float, val dy: Float, val color: Int, val born: Long)

class GameController(private val store: Store, private val tones: Tones, private val buzz: Buzz) {
    private var saved: GameState? = store.loadGame()
    var languageTags by mutableStateOf(listOf("en"))
    var settings by mutableStateOf(store.loadSettings())
    var state by mutableStateOf(Engine.menuState(store.loadBest()))
    var hasContinue by mutableStateOf(saved != null)
    var showHint by mutableStateOf(!store.hintSeen())
    var settingsOpen by mutableStateOf(false)
    var pending by mutableStateOf<PlaceResult?>(null)
    var banner by mutableStateOf<String?>(null)
    var shake by mutableStateOf(false)
    var remark by mutableStateOf("")
    val floaters = mutableStateListOf<Floater>()
    val sparks = mutableStateListOf<Spark>()
    val copy: Copy get() = Texts.of(Texts.resolve(settings.locale, languageTags))

    init {
        tones.on = settings.sound
        buzz.on = settings.haptics
        remark = copy.remarks.random()
    }

    fun displayBoard(): List<List<Int>> = pending?.stamped ?: state.board

    fun displayTray(): List<Piece?> = if (pending?.refilled == true) listOf(null, null, null) else state.tray

    fun fitBoard(): List<List<Int>> = pending?.next?.board ?: state.board

    fun busy() = pending != null || state.status != "playing"

    fun patch(next: Settings) {
        settings = next
        tones.on = next.sound
        buzz.on = next.haptics
        store.saveSettings(next)
        remark = copy.remarks.random()
    }

    fun start(resume: Boolean) {
        if (settings.sound) tones.tap()
        if (resume) {
            val loaded = saved
            if (loaded != null) {
                state = loaded.copy(status = "playing", best = maxOf(loaded.best, store.loadBest()))
                hasContinue = false
                settingsOpen = false
                return
            }
        }
        state = Engine.newGame(state.best)
        saved = state
        hasContinue = false
        settingsOpen = false
        store.saveGame(state)
    }

    fun restart() {
        pending = null
        banner = null
        sparks.clear()
        shake = false
        if (settings.sound) tones.tap()
        state = Engine.newGame(state.best)
        saved = state
        store.saveGame(state)
    }

    fun toMenu() {
        if (pending != null) commit()
        if (state.status == "playing") {
            saved = state
            store.saveGame(state)
        }
        banner = null
        sparks.clear()
        shake = false
        state = Engine.menuState(maxOf(state.best, store.loadBest()))
        hasContinue = saved?.status == "playing"
        settingsOpen = false
        remark = copy.remarks.random()
    }

    fun drop(slot: Int, row: Int, col: Int): Boolean {
        if (busy()) return false
        val result = Engine.placePiece(state, slot, row, col) ?: return false
        pending = result
        showHint = false
        store.setHintSeen()
        val mid = result.placed.firstOrNull() ?: Cell(3, 3)
        floaters += Floater("+${result.points}", mid.r, mid.c, System.currentTimeMillis())
        banner = when {
            result.perfect -> copy.perfect
            result.combo >= 2 -> copy.combo(result.combo)
            else -> null
        }
        if (result.clears.count >= 2) shake = true
        if (result.clears.count > 0) {
            val now = System.currentTimeMillis()
            val rnd = Random(now)
            for (r in 0 until Engine.BOARD) {
                for (c in 0 until Engine.BOARD) {
                    val value = result.stamped[r][c]
                    if (value == 0 || !result.clears.hits(r, c)) continue
                    repeat(2) {
                        if (sparks.size >= 32) return@repeat
                        val ang = rnd.nextFloat() * 6.2831855f
                        val dist = 10f + rnd.nextFloat() * 22f
                        sparks += Spark(r, c, cos(ang) * dist, sin(ang) * dist, value - 1, now)
                    }
                }
            }
        }
        if (settings.sound) {
            tones.place()
            if (result.clears.count > 0) {
                tones.clear(result.clears.count)
                if (result.combo >= 2) tones.combo(result.combo)
                if (result.perfect) tones.perfect()
            }
        }
        if (settings.haptics) {
            if (result.clears.count > 0) buzz.pattern(8, 30, 16) else buzz.once(10)
        }
        return true
    }

    fun reject() {
        if (settings.sound) tones.reject()
        if (settings.haptics) buzz.once(8)
    }

    fun dragBegan() {
        if (settings.sound) tones.tap()
        if (settings.haptics) buzz.once(6)
    }

    fun commit() {
        val result = pending ?: return
        pending = null
        shake = false
        state = result.next
        if (result.gameOver) {
            saved = null
            hasContinue = false
            if (settings.sound) tones.over()
            if (settings.haptics) buzz.once(40)
        } else {
            saved = state
            hasContinue = true
        }
        store.saveGame(state)
        store.saveBest(state.best)
    }

    fun freshRecord() = state.score > 0 && state.score >= state.best

    fun persist() {
        if (state.status == "playing" && pending == null) store.saveGame(state)
        store.saveBest(state.best)
        store.saveSettings(settings)
    }
}
