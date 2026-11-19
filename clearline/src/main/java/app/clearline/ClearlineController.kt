package app.clearline

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.clearline.audio.ClearTones
import app.clearline.data.ActiveCall
import app.clearline.data.AppState
import app.clearline.data.CallDirection
import app.clearline.data.CallPhase
import app.clearline.data.CallRecord
import app.clearline.data.Contact
import app.clearline.data.RegState
import app.clearline.data.SipAccount
import app.clearline.data.Store
import app.clearline.sip.SipEngine
import kotlinx.coroutines.launch

class ClearlineController(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    private val tones = ClearTones()

    var state by mutableStateOf(
        AppState(
            account = store.loadAccount(),
            contacts = store.loadContacts(),
            recents = store.loadRecents(),
        ),
    )
        private set

    private val sip = SipEngine(
        scope = viewModelScope,
        onReg = { reg, detail ->
            state = state.copy(regState = reg, regDetail = detail)
        },
        onCallPhase = { phase, _ ->
            val current = state.activeCall
            when (phase) {
                CallPhase.IDLE -> {
                    tones.stopLoops()
                    state = state.copy(activeCall = null)
                }
                CallPhase.ENDING -> {
                    tones.stopLoops()
                    tones.hangup()
                    state = state.copy(activeCall = current?.copy(phase = phase))
                }
                CallPhase.RINGING_OUT -> {
                    tones.startRingback()
                    state = state.copy(activeCall = current?.copy(phase = phase))
                }
                CallPhase.RINGING_IN -> {
                    tones.startRingtone()
                    state = state.copy(activeCall = current?.copy(phase = phase))
                }
                CallPhase.ACTIVE -> {
                    tones.stopLoops()
                    tones.connect()
                    state = state.copy(
                        activeCall = current?.copy(
                            phase = phase,
                            connectedAt = current.connectedAt ?: System.currentTimeMillis(),
                        ),
                    )
                }
                CallPhase.DIALING -> {
                    state = state.copy(activeCall = current?.copy(phase = phase))
                }
                CallPhase.HELD -> {
                    state = state.copy(activeCall = current?.copy(phase = phase, held = true))
                }
            }
        },
        onCallEnded = { record ->
            val recents = listOf(record) + state.recents
            store.saveRecents(recents)
            state = state.copy(recents = recents)
        },
    )

    init {
        sip.configure(state.account)
        sip.start()
    }

    fun setTab(tab: Int) {
        state = state.copy(tab = tab)
    }

    fun setSearch(q: String) {
        state = state.copy(search = q)
    }

    fun setDialBuffer(v: String) {
        state = state.copy(dialBuffer = v)
    }

    fun dialDigit(ch: Char) {
        tones.dtmf(ch)
        state = state.copy(dialBuffer = state.dialBuffer + ch)
    }

    fun dialBackspace() {
        if (state.dialBuffer.isNotEmpty()) {
            state = state.copy(dialBuffer = state.dialBuffer.dropLast(1))
        }
    }

    fun saveAccount(account: SipAccount) {
        store.saveAccount(account)
        state = state.copy(account = account)
        sip.configure(account)
        sip.start()
        tones.connect()
    }

    fun upsertContact(contact: Contact) {
        val list = state.contacts.filterNot { it.id == contact.id } + contact
        val sorted = list.sortedBy { it.name.lowercase() }
        store.saveContacts(sorted)
        state = state.copy(contacts = sorted)
        tones.tap()
    }

    fun deleteContact(id: String) {
        val list = state.contacts.filterNot { it.id == id }
        store.saveContacts(list)
        state = state.copy(contacts = list)
    }

    fun callUri(uri: String, name: String = "") {
        if (state.activeCall != null) return
        if (uri.isBlank()) return
        tones.dial()
        val display = name.ifBlank {
            state.contacts.firstOrNull { it.uri.equals(uri, true) }?.name ?: uri
        }
        state = state.copy(
            activeCall = ActiveCall(
                peer = uri,
                displayName = display,
                direction = CallDirection.OUTGOING,
                phase = CallPhase.DIALING,
            ),
            dialBuffer = "",
            tab = 0,
        )
        sip.placeCall(uri, display)
    }

    fun callDialBuffer() = callUri(state.dialBuffer)

    fun hangup() {
        sip.declineOrHangup()
    }

    fun accept() {
        tones.stopLoops()
        sip.acceptIncoming()
    }

    fun toggleMute() {
        val call = state.activeCall ?: return
        tones.tap()
        state = state.copy(activeCall = call.copy(muted = !call.muted))
    }

    fun toggleSpeaker() {
        val call = state.activeCall ?: return
        tones.tap()
        state = state.copy(activeCall = call.copy(onSpeaker = !call.onSpeaker))
    }

    fun toggleHold() {
        val call = state.activeCall ?: return
        tones.tap()
        val held = !call.held
        state = state.copy(
            activeCall = call.copy(
                held = held,
                phase = if (held) CallPhase.HELD else CallPhase.ACTIVE,
            ),
        )
    }

    fun demoIncoming() {
        if (state.activeCall != null) return
        val from = state.contacts.firstOrNull() ?: Contact(name = "Echo Lab", uri = "echo@demo.clearline")
        state = state.copy(
            activeCall = ActiveCall(
                peer = from.uri,
                displayName = from.name,
                direction = CallDirection.INCOMING,
                phase = CallPhase.RINGING_IN,
            ),
        )
        sip.simulateIncoming(from.uri, from.name)
    }

    fun setSounds(on: Boolean) {
        tones.enabled = on
    }

    override fun onCleared() {
        sip.stop()
        tones.stopLoops()
        super.onCleared()
    }
}
