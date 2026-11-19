package app.clearline.data

import java.util.UUID

data class SipAccount(
    val displayName: String = "",
    val username: String = "",
    val password: String = "",
    val domain: String = "",
    val proxy: String = "",
    val port: Int = 5060,
    val transport: String = "UDP",
    val demoMode: Boolean = true,
)

data class Contact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val uri: String,
    val note: String = "",
)

enum class CallDirection { OUTGOING, INCOMING }

enum class CallOutcome { ANSWERED, MISSED, DECLINED, FAILED, CANCELLED }

data class CallRecord(
    val id: String = UUID.randomUUID().toString(),
    val peer: String,
    val displayName: String,
    val direction: CallDirection,
    val outcome: CallOutcome,
    val startedAt: Long = System.currentTimeMillis(),
    val durationSec: Int = 0,
)

enum class RegState { OFFLINE, CONNECTING, REGISTERED, FAILED }

enum class CallPhase {
    IDLE,
    DIALING,
    RINGING_OUT,
    RINGING_IN,
    ACTIVE,
    HELD,
    ENDING,
}

data class ActiveCall(
    val id: String = UUID.randomUUID().toString(),
    val peer: String,
    val displayName: String,
    val direction: CallDirection,
    val phase: CallPhase = CallPhase.DIALING,
    val muted: Boolean = false,
    val onSpeaker: Boolean = false,
    val held: Boolean = false,
    val startedAt: Long = System.currentTimeMillis(),
    val connectedAt: Long? = null,
)

data class AppState(
    val account: SipAccount = SipAccount(),
    val contacts: List<Contact> = emptyList(),
    val recents: List<CallRecord> = emptyList(),
    val regState: RegState = RegState.OFFLINE,
    val regDetail: String = "",
    val activeCall: ActiveCall? = null,
    val tab: Int = 0,
    val dialBuffer: String = "",
    val search: String = "",
    val lanOn: Boolean = false,
    val lanPeers: List<app.clearline.lan.LanPeer> = emptyList(),
    val lanMessages: List<app.clearline.lan.LanMessage> = emptyList(),
    val lanDraft: String = "",
    val splash: Boolean = true,
)
