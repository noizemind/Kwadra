package app.clearline.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.clearline.ClearlineController
import app.clearline.data.CallDirection
import app.clearline.data.CallOutcome
import app.clearline.data.CallPhase
import app.clearline.data.Contact
import app.clearline.data.RegState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClearlineApp(vm: ClearlineController) {
    ClearlineTheme {
        val colors = LocalClearColors.current
        val state = vm.state
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.pageBrush),
        ) {
            AeroSkyDecor()
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                when (state.tab) {
                    0 -> ContactsPane(vm)
                    1 -> DialerPane(vm)
                    2 -> LanPane(vm)
                    3 -> RecentsPane(vm)
                    else -> SettingsPane(vm)
                }
                BottomNav(selected = state.tab, onSelect = vm::setTab)
            }

            AnimatedVisibility(
                visible = state.activeCall != null,
                enter = fadeIn() + slideInVertically { it / 4 },
                exit = fadeOut() + slideOutVertically { it / 4 },
            ) {
                state.activeCall?.let { CallOverlay(vm) }
            }

            AnimatedVisibility(
                visible = state.splash,
                enter = fadeIn(),
                exit = fadeOut() + scaleOut(targetScale = 1.05f),
            ) {
                AeroSplash { vm.dismissSplash() }
            }
        }
    }
}

@Composable
private fun AeroSplash(onDone: () -> Unit) {
    val colors = LocalClearColors.current
    val type = LocalClearType.current
    val pulse = rememberInfiniteTransition(label = "splash")
    val scale by pulse.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ss",
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.heroBrush)
            .clickable(onClick = onDone),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .scale(scale)
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(listOf(Color(0xAAFFFFFF), Color(0x55FFFFFF))),
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("C", style = type.brand.copy(fontSize = 52.sp), color = Color.White)
            }
            Spacer(Modifier.height(18.dp))
            Text("Clearline", style = type.brand.copy(fontSize = 36.sp), color = Color.White)
            Text("sky · glass · voice", style = type.caption, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun AeroSkyDecor() {
    val float = rememberInfiniteTransition(label = "aero")
    val drift by float.animateFloat(
        initialValue = 0f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(5200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift",
    )
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .offset((-40).dp, (80 + drift).dp)
                .size(220.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xAAFFFFFF), Color(0x55A8E8FF), Color.Transparent),
                    ),
                    CircleShape,
                ),
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(40.dp, (40 - drift).dp)
                .size(180.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0x99FFFFFF), Color(0x447FE3F0), Color.Transparent),
                    ),
                    CircleShape,
                ),
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset((-20).dp, (-120).dp)
                .size(260.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0x66FFFFFF), Color(0x335CC8FF), Color.Transparent),
                    ),
                    CircleShape,
                ),
        )
    }
}

@Composable
private fun ContactsPane(vm: ClearlineController) {
    val colors = LocalClearColors.current
    val type = LocalClearType.current
    val state = vm.state
    var editing by remember { mutableStateOf<Contact?>(null) }
    val filtered = state.contacts.filter {
        state.search.isBlank() ||
            it.name.contains(state.search, true) ||
            it.uri.contains(state.search, true)
    }

    Column(Modifier.fillMaxSize()) {
        HeroHeader(
            subtitle = regLabel(state.regState, state.regDetail),
            trailing = {
                RoundIcon(Icons.Default.Add, "Add") { editing = Contact(name = "", uri = "") }
            },
        )
        SearchField(state.search, vm::setSearch, placeholder = "Search people")
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(filtered, key = { it.id }) { c ->
                ContactRow(
                    contact = c,
                    onCall = { vm.callUri(c.uri, c.name) },
                    onEdit = { editing = c },
                )
            }
            if (filtered.isEmpty()) {
                item {
                    Text(
                        "No contacts yet",
                        style = type.body,
                        color = colors.muted,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
    }

    editing?.let { draft ->
        ContactEditor(
            initial = draft,
            onDismiss = { editing = null },
            onSave = {
                vm.upsertContact(it)
                editing = null
            },
            onDelete = if (draft.name.isNotBlank() || draft.uri.isNotBlank()) {
                {
                    vm.deleteContact(draft.id)
                    editing = null
                }
            } else null,
        )
    }
}

@Composable
private fun DialerPane(vm: ClearlineController) {
    val colors = LocalClearColors.current
    val type = LocalClearType.current
    val buf = vm.state.dialBuffer

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        Text("Clearline", style = type.brand.copy(fontSize = 28.sp), color = colors.ink)
        Text("Dial anyone on your SIP line", style = type.caption, color = colors.muted)
        Spacer(Modifier.height(28.dp))
        Text(
            text = buf.ifEmpty { " " },
            style = type.dial,
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("*", "0", "#"),
        )
        keys.forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { k ->
                    DialKey(k) { vm.dialDigit(k.first()) }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "⌫",
                style = type.title,
                color = colors.muted,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { vm.dialBackspace() }
                    .padding(18.dp),
            )
            Box(
                Modifier
                    .size(72.dp)
                    .shadow(16.dp, CircleShape, ambientColor = Color(0x662EA8E5), spotColor = Color(0x772EA8E5))
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF9AEFFF), Color(0xFF2EA8E5), Color(0xFF0B6BB5)),
                        ),
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.75f), CircleShape)
                    .clickable { vm.callDialBuffer() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Call, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.size(56.dp))
        }
    }
}

@Composable
private fun LanPane(vm: ClearlineController) {
    val colors = LocalClearColors.current
    val type = LocalClearType.current
    val state = vm.state
    Column(Modifier.fillMaxSize()) {
        HeroHeader(subtitle = if (state.lanOn) "LAN mesh · ${state.lanPeers.size} nearby" else "LAN mesh offline")
        Box(Modifier.padding(horizontal = 16.dp)) {
            ToggleRow(
                title = "Share presence on Wi‑Fi",
                subtitle = "UDP beacon + TCP chat on the local network",
                checked = state.lanOn,
                onToggle = vm::toggleLan,
            )
        }
        if (state.lanPeers.isNotEmpty()) {
            Text(
                "Nearby",
                style = type.caption,
                color = colors.muted,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.lanPeers.take(6).forEach { peer ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(listOf(Color(0xFF9AEFFF), Color(0xFF2EA8E5))),
                                )
                                .border(1.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(peer.name.take(1).uppercase(), color = Color.White, style = type.body)
                        }
                        Text(peer.name.take(8), style = type.caption, color = colors.inkSoft, maxLines = 1)
                    }
                }
            }
        }
        LazyColumn(
            Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            reverseLayout = false,
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(state.lanMessages, key = { it.id }) { msg ->
                val align = if (msg.mine) Alignment.CenterEnd else Alignment.CenterStart
                Box(Modifier.fillMaxWidth(), contentAlignment = align) {
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (msg.mine) Brush.verticalGradient(listOf(Color(0xFF7FE3F0), Color(0xFF2EA8E5)))
                                else Brush.verticalGradient(listOf(Color(0xF2FFFFFF), Color(0xCCE8F6FF))),
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        if (!msg.mine) {
                            Text(msg.fromName, style = type.caption, color = colors.inkSoft)
                        }
                        Text(msg.text, style = type.body, color = if (msg.mine) Color.White else colors.ink)
                    }
                }
            }
            if (state.lanMessages.isEmpty()) {
                item {
                    Text(
                        "Turn on LAN and say hi — peers on the same Wi‑Fi will light up.",
                        style = type.body,
                        color = colors.muted,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                SearchField(state.lanDraft, vm::setLanDraft, placeholder = "Message the LAN…")
            }
            Spacer(Modifier.width(4.dp))
            RoundIcon(Icons.AutoMirrored.Filled.Send, "Send") { vm.sendLanChat() }
        }
    }
}

@Composable
private fun RecentsPane(vm: ClearlineController) {
    val colors = LocalClearColors.current
    val type = LocalClearType.current
    val fmt = remember { SimpleDateFormat("d MMM · HH:mm", Locale.getDefault()) }

    Column(Modifier.fillMaxSize()) {
        HeroHeader(subtitle = "Recent calls")
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(vm.state.recents, key = { it.id }) { r ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { vm.callUri(r.peer, r.displayName) }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val tint = when (r.outcome) {
                        CallOutcome.MISSED, CallOutcome.DECLINED, CallOutcome.FAILED -> colors.coral
                        else -> colors.teal
                    }
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(tint.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier
                                .size(20.dp)
                                .scale(if (r.direction == CallDirection.INCOMING) 1f else 1f),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.displayName, style = type.body, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${dirLabel(r.direction)} · ${outcomeLabel(r.outcome)}" +
                                if (r.durationSec > 0) " · ${formatDuration(r.durationSec)}" else "",
                            style = type.caption,
                            color = colors.muted,
                        )
                    }
                    Text(fmt.format(Date(r.startedAt)), style = type.caption, color = colors.muted)
                }
            }
            if (vm.state.recents.isEmpty()) {
                item {
                    Text("Calls you make will land here", style = type.body, color = colors.muted, modifier = Modifier.padding(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsPane(vm: ClearlineController) {
    val colors = LocalClearColors.current
    val type = LocalClearType.current
    val acc = vm.state.account
    var draft by remember(acc) { mutableStateOf(acc) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        HeroHeader(subtitle = "SIP account · MicroSIP-style fields")
        Text("Account", style = type.title, color = colors.ink)
        Spacer(Modifier.height(12.dp))
        Field("Display name", draft.displayName) { draft = draft.copy(displayName = it) }
        Field("Username", draft.username) { draft = draft.copy(username = it) }
        Field("Password", draft.password, password = true) { draft = draft.copy(password = it) }
        Field("Domain", draft.domain) { draft = draft.copy(domain = it) }
        Field("Proxy (optional)", draft.proxy) { draft = draft.copy(proxy = it) }
        Field("Port", draft.port.toString(), keyboard = KeyboardType.Number) {
            draft = draft.copy(port = it.toIntOrNull() ?: 5060)
        }
        Spacer(Modifier.height(8.dp))
        ToggleRow(
            title = "Demo mode",
            subtitle = "Simulate register & calls without a SIP server",
            checked = draft.demoMode,
            onToggle = { draft = draft.copy(demoMode = it) },
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Save & register") { vm.saveAccount(draft) }
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Simulate incoming call") { vm.demoIncoming() }
        Spacer(Modifier.height(28.dp))
        Text(
            "Clearline is an original softphone. SIP flows follow classic REGISTER/INVITE/BYE patterns used by open softphones such as MicroSIP — the interface and sounds are Clearline’s own.",
            style = type.caption,
            color = colors.muted,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CallOverlay(vm: ClearlineController) {
    val colors = LocalClearColors.current
    val type = LocalClearType.current
    val call = vm.state.activeCall ?: return
    var tick by remember { mutableLongStateOf(0L) }
    var showPad by remember { mutableStateOf(false) }
    LaunchedEffect(call.connectedAt, call.phase) {
        while (call.phase == CallPhase.ACTIVE || call.phase == CallPhase.HELD) {
            tick = System.currentTimeMillis()
            delay(500)
        }
    }
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ps",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.heroBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(36.dp))
            Text("Clearline", style = type.caption, color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.height(28.dp))
            if (!showPad) {
                Box(
                    Modifier
                        .size(120.dp)
                        .scale(if (call.phase == CallPhase.RINGING_IN || call.phase == CallPhase.RINGING_OUT) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        call.displayName.take(1).uppercase(),
                        style = type.brand.copy(fontSize = 48.sp),
                        color = Color.White,
                    )
                }
                Spacer(Modifier.height(22.dp))
                Text(call.displayName, style = type.title.copy(fontSize = 28.sp), color = Color.White, textAlign = TextAlign.Center)
                Text(call.peer, style = type.caption, color = Color.White.copy(alpha = 0.65f))
                Spacer(Modifier.height(10.dp))
            }
            AnimatedContent(
                targetState = call.phase,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "phase",
            ) { phase ->
                val label = when (phase) {
                    CallPhase.DIALING -> "Calling…"
                    CallPhase.RINGING_OUT -> "Ringing…"
                    CallPhase.RINGING_IN -> "Incoming call"
                    CallPhase.ACTIVE -> {
                        val start = call.connectedAt ?: tick
                        formatDuration(((tick - start).coerceAtLeast(0) / 1000).toInt())
                    }
                    CallPhase.HELD -> "On hold"
                    CallPhase.ENDING -> "Call ended"
                    CallPhase.IDLE -> ""
                }
                Text(label, style = type.body, color = Color(0xFF99F6E4))
            }
            Spacer(Modifier.weight(1f))
            if (call.phase == CallPhase.RINGING_IN) {
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    CircleAction(Icons.Default.CallEnd, colors.coral, "Decline") { vm.hangup() }
                    CircleAction(Icons.Default.Call, colors.ok, "Accept") { vm.accept() }
                }
            } else if (showPad && (call.phase == CallPhase.ACTIVE || call.phase == CallPhase.HELD)) {
                InCallPad(onDigit = vm::sendInCallDtmf)
                Spacer(Modifier.height(16.dp))
                SecondaryButtonLight("Hide keypad") { showPad = false }
                Spacer(Modifier.height(16.dp))
                CircleAction(Icons.Default.CallEnd, colors.coral, "Hang up") { vm.hangup() }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    SoftAction(
                        if (call.muted) Icons.Default.MicOff else Icons.Default.Mic,
                        if (call.muted) "Unmute" else "Mute",
                    ) { vm.toggleMute() }
                    SoftAction(
                        if (call.held) Icons.Default.PlayArrow else Icons.Default.Pause,
                        if (call.held) "Resume" else "Hold",
                    ) { vm.toggleHold() }
                    SoftAction(Icons.Default.Dialpad, "Keypad") {
                        if (call.phase == CallPhase.ACTIVE || call.phase == CallPhase.HELD) showPad = true
                    }
                    SoftAction(Icons.AutoMirrored.Filled.VolumeUp, if (call.onSpeaker) "Speaker on" else "Speaker") {
                        vm.toggleSpeaker()
                    }
                }
                Spacer(Modifier.height(28.dp))
                CircleAction(Icons.Default.CallEnd, colors.coral, "Hang up") { vm.hangup() }
            }
            Spacer(Modifier.height(36.dp))
        }
    }
}

@Composable
private fun InCallPad(onDigit: (Char) -> Unit) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { k ->
                    Box(
                        Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable { onDigit(k.first()) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(k, color = Color.White, style = LocalClearType.current.dial.copy(fontSize = 24.sp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SecondaryButtonLight(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White.copy(alpha = 0.9f), style = LocalClearType.current.caption)
    }
}

@Composable
private fun HeroHeader(subtitle: String, trailing: (@Composable () -> Unit)? = null) {
    val colors = LocalClearColors.current
    val type = LocalClearType.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 16.dp, top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Clearline",
                style = type.brand,
                color = colors.ink,
                modifier = Modifier.padding(bottom = 2.dp),
            )
            Text(subtitle, style = type.caption, color = colors.muted)
        }
        trailing?.invoke()
    }
}

@Composable
private fun BottomNav(selected: Int, onSelect: (Int) -> Unit) {
    val colors = LocalClearColors.current
    val items = listOf(
        Triple(Icons.Default.People, "People", 0),
        Triple(Icons.Default.Dialpad, "Dial", 1),
        Triple(Icons.Default.Wifi, "LAN", 2),
        Triple(Icons.Default.History, "Recents", 3),
        Triple(Icons.Default.Settings, "Setup", 4),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .shadow(18.dp, RoundedCornerShape(28.dp), ambientColor = Color(0x552EA8E5), spotColor = Color(0x662EA8E5))
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF2FFFFFF), Color(0xD9E8F7FF), Color(0xC2C9E9FF)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(28.dp))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        items.forEach { (icon, label, idx) ->
            val on = selected == idx
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .then(
                        if (on) Modifier.background(
                            Brush.verticalGradient(listOf(Color(0xAAFFFFFF), Color(0x665CC8FF))),
                            RoundedCornerShape(16.dp),
                        ) else Modifier,
                    )
                    .clickable { onSelect(idx) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(icon, contentDescription = label, tint = if (on) colors.sky else colors.muted, modifier = Modifier.size(22.dp))
                Text(label, style = LocalClearType.current.caption, color = if (on) colors.inkSoft else colors.muted)
            }
        }
    }
}

@Composable
private fun ContactRow(contact: Contact, onCall: () -> Unit, onEdit: () -> Unit) {
    val colors = LocalClearColors.current
    val type = LocalClearType.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onEdit)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(46.dp)
                .shadow(8.dp, CircleShape, ambientColor = Color(0x442EA8E5), spotColor = Color(0x552EA8E5))
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF7FE3F0), Color(0xFF2EA8E5), Color(0xFF0B6BB5))),
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(contact.name.take(1).uppercase(), color = Color.White, style = type.title)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(contact.name, style = type.body, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(contact.uri, style = type.caption, color = colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier
                .size(44.dp)
                .shadow(6.dp, CircleShape, ambientColor = Color(0x332EA8E5), spotColor = Color(0x442EA8E5))
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD6F0FF))),
                )
                .border(1.dp, Color.White, CircleShape)
                .clickable(onClick = onCall),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Call, null, tint = colors.sky)
        }
    }
}

@Composable
private fun DialKey(label: String, onClick: () -> Unit) {
    val colors = LocalClearColors.current
    val type = LocalClearType.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        Modifier
            .size(78.dp)
            .scale(if (pressed) 0.94f else 1f)
            .shadow(10.dp, CircleShape, ambientColor = Color(0x442EA8E5), spotColor = Color(0x552EA8E5))
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFFFFFF), Color(0xFFE3F4FF), Color(0xFFBFE4FF)),
                ),
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.95f), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = type.dial.copy(fontSize = 28.sp), color = colors.ink)
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String = "Search") {
    val colors = LocalClearColors.current
    Box(
        Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x332EA8E5), spotColor = Color(0x442EA8E5))
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xF2FFFFFF), Color(0xCCE8F6FF))),
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, style = LocalClearType.current.body, color = colors.muted)
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = LocalClearType.current.body.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.sky),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    password: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    val colors = LocalClearColors.current
    Column(Modifier.padding(bottom = 10.dp)) {
        Text(label, style = LocalClearType.current.caption, color = colors.muted)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            textStyle = LocalClearType.current.body.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.teal),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.8f))
                .border(1.dp, colors.line.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    val colors = LocalClearColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.7f))
            .clickable { onToggle(!checked) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = LocalClearType.current.body, color = colors.ink)
            Text(subtitle, style = LocalClearType.current.caption, color = colors.muted)
        }
        Box(
            Modifier
                .width(48.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (checked) colors.teal else colors.line),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .padding(3.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x552EA8E5), spotColor = Color(0x662EA8E5))
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF7FE3F0), Color(0xFF2EA8E5), Color(0xFF0F7EC4)),
                ),
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = LocalClearType.current.body, color = Color.White)
    }
}

@Composable
private fun SecondaryButton(label: String, onClick: () -> Unit) {
    val colors = LocalClearColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.45f))
            .border(1.5.dp, colors.skyBright.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = LocalClearType.current.body, color = colors.inkSoft)
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, desc: String, onClick: () -> Unit) {
    val colors = LocalClearColors.current
    Box(
        Modifier
            .size(44.dp)
            .shadow(8.dp, CircleShape, ambientColor = Color(0x442EA8E5), spotColor = Color(0x552EA8E5))
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD6F0FF))),
            )
            .border(1.dp, Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = colors.sky)
    }
}

@Composable
private fun CircleAction(icon: ImageVector, bg: Color, desc: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(74.dp)
                .clip(CircleShape)
                .background(bg)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, desc, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(desc, style = LocalClearType.current.caption, color = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
private fun SoftAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, label, tint = Color.White)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = LocalClearType.current.caption, color = Color.White.copy(alpha = 0.75f))
    }
}

@Composable
private fun ContactEditor(
    initial: Contact,
    onDismiss: () -> Unit,
    onSave: (Contact) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val colors = LocalClearColors.current
    var name by remember { mutableStateOf(initial.name) }
    var uri by remember { mutableStateOf(initial.uri) }
    var note by remember { mutableStateOf(initial.note) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x990B1F2A))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(colors.foam)
                .clickable(enabled = false) {}
                .padding(20.dp)
                .navigationBarsPadding(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Contact", style = LocalClearType.current.title, color = colors.ink, modifier = Modifier.weight(1f))
                Icon(Icons.Default.Close, "Close", tint = colors.muted, modifier = Modifier.clickable(onClick = onDismiss))
            }
            Spacer(Modifier.height(12.dp))
            Field("Name", name) { name = it }
            Field("SIP URI / number", uri) { uri = it }
            Field("Note", note) { note = it }
            PrimaryButton("Save") {
                if (name.isNotBlank() && uri.isNotBlank()) {
                    onSave(initial.copy(name = name.trim(), uri = uri.trim(), note = note.trim()))
                }
            }
            if (onDelete != null) {
                Spacer(Modifier.height(8.dp))
                SecondaryButton("Delete") { onDelete() }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun regLabel(state: RegState, detail: String): String = when (state) {
    RegState.OFFLINE -> "Offline"
    RegState.CONNECTING -> "Connecting…"
    RegState.REGISTERED -> detail.ifBlank { "Online" }
    RegState.FAILED -> "Failed · $detail"
}

private fun dirLabel(d: CallDirection) = if (d == CallDirection.INCOMING) "Incoming" else "Outgoing"

private fun outcomeLabel(o: CallOutcome) = when (o) {
    CallOutcome.ANSWERED -> "Answered"
    CallOutcome.MISSED -> "Missed"
    CallOutcome.DECLINED -> "Declined"
    CallOutcome.FAILED -> "Failed"
    CallOutcome.CANCELLED -> "Cancelled"
}

private fun formatDuration(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return "%d:%02d".format(m, s)
}
