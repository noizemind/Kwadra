package app.clearline

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.clearline.audio.ClearTones
import app.clearline.audio.RtpAudioSession
import app.clearline.audio.VoiceRoute
import app.clearline.data.ActiveCall
import app.clearline.data.AppState
import app.clearline.data.CallDirection
import app.clearline.data.CallPhase
import app.clearline.data.Contact
import app.clearline.data.SipAccount
import app.clearline.data.Store
import app.clearline.lan.LanMesh
import app.clearline.sip.SdpMedia
import app.clearline.sip.SipEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ClearlineController(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    private val tones = ClearTones()
    private val rtp = RtpAudioSession()
    private val route = VoiceRoute(app)
    private val appCtx = app.applicationContext
    private var remoteMedia: SdpMedia? = null

    private val lan = LanMesh(
        context = app,
        scope = viewModelScope,
        onPeers = { peers -> state = state.copy(lanPeers = peers) },
        onMessage = { msg ->
            state = state.copy(lanMessages = (state.lanMessages + msg).takeLast(200))
            if (!msg.mine) tones.tap()
        },
    )

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
                    stopMedia()
                    state = state.copy(activeCall = null)
                }
                CallPhase.ENDING -> {
                    tones.stopLoops()
                    tones.hangup()
                    stopMedia()
                    state = state.copy(activeCall = current?.copy(phase = phase))
                }
                CallPhase.RINGING_OUT -> {
                    tones.startRingback()
                    state = state.copy(activeCall = current?.copy(phase = phase))
                }
                CallPhase.RINGING_IN -> {
                    tones.startRingtone()
                    state = state.copy(activeCall = current?.copy(phase = phase))
                    current?.let {
                        CallService.start(appCtx, "Incoming · Clearline", it.displayName)
                    }
                }
                CallPhase.ACTIVE -> {
                    tones.stopLoops()
                    tones.connect()
                    val updated = current?.copy(
                        phase = phase,
                        connectedAt = current.connectedAt ?: System.currentTimeMillis(),
                    )
                    state = state.copy(activeCall = updated)
                    startMedia()
                    updated?.let {
                        CallService.start(appCtx, it.displayName, "Clearline call")
                    }
                }
                CallPhase.DIALING -> {
                    state = state.copy(activeCall = current?.copy(phase = phase))
                }
                CallPhase.HELD -> {
                    rtp.muted = true
                    state = state.copy(activeCall = current?.copy(phase = phase, held = true))
                }
            }
        },
        onCallEnded = { record ->
            val recents = listOf(record) + state.recents
            store.saveRecents(recents)
            state = state.copy(recents = recents)
        },
        onRemoteMedia = { media ->
            remoteMedia = media
            if (state.activeCall?.phase == CallPhase.ACTIVE) startMedia()
        },
    )

    init {
        sip.configure(state.account)
        sip.start()
        viewModelScope.launch {
            delay(1600)
            dismissSplash()
        }
    }

    fun dismissSplash() {
        if (state.splash) {
            state = state.copy(splash = false)
            tones.connect()
        }
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

    fun sendInCallDtmf(ch: Char) {
        tones.dtmf(ch)
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
        remoteMedia = null
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
        val muted = !call.muted
        rtp.muted = muted || call.held
        state = state.copy(activeCall = call.copy(muted = muted))
    }

    fun toggleSpeaker() {
        val call = state.activeCall ?: return
        tones.tap()
        val on = !call.onSpeaker
        rtp.speakerphone = on
        route.setSpeaker(on)
        state = state.copy(activeCall = call.copy(onSpeaker = on))
    }

    fun toggleHold() {
        val call = state.activeCall ?: return
        tones.tap()
        val held = !call.held
        rtp.muted = held || call.muted
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
        remoteMedia = null
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

    fun toggleLan(on: Boolean) {
        val name = state.account.displayName.ifBlank { "Clearline-${lan.selfId}" }
        if (on) {
            lan.start(name)
            state = state.copy(lanOn = true)
            tones.connect()
        } else {
            lan.stop()
            state = state.copy(lanOn = false, lanPeers = emptyList())
        }
    }

    fun setLanDraft(v: String) {
        state = state.copy(lanDraft = v)
    }

    fun sendLanChat() {
        val text = state.lanDraft
        if (text.isBlank()) return
        state = state.copy(lanDraft = "")
        lan.sendChat(text)
        tones.tap()
    }

    private fun startMedia() {
        val call = state.activeCall
        val demo = state.account.demoMode || state.account.domain.isBlank() || remoteMedia == null
        rtp.muted = call?.muted == true || call?.held == true
        rtp.speakerphone = call?.onSpeaker == true
        route.enterCall(speaker = call?.onSpeaker == true)
        rtp.start(
            localRtpPort = if (demo) 0 else sip.localRtpPort,
            remote = remoteMedia?.host,
            remoteRtpPort = remoteMedia?.port ?: 0,
            echoDemo = demo,
        )
    }

    private fun stopMedia() {
        rtp.stop()
        route.leaveCall()
        CallService.stop(appCtx)
        remoteMedia = null
    }

    override fun onCleared() {
        lan.stop()
        sip.stop()
        tones.stopLoops()
        stopMedia()
        super.onCleared()
    }
}
