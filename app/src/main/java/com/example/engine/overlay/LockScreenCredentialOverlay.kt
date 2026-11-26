package com.example.engine.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LockCredentialHasher
import com.example.data.model.LockCredentialType
import com.example.data.model.UiStyleMode
import com.example.data.model.UserPreferencesDoc
import com.example.ui.theme.DotGothicFontFamily
import com.example.ui.theme.LocalUiStyleMode
import com.example.ui.theme.rememberStudioHaptics
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.pow

@Composable
internal fun LockScreenCredentialGate(
    preferences: UserPreferencesDoc,
    onUnlockSuccess: () -> Unit,
    onCancelToClock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiStyle = LocalUiStyleMode.current
    val isGlass = uiStyle == UiStyleMode.FROSTED_GLASS_OS
    val isGlyph = uiStyle == UiStyleMode.GLYPH_UI
    val haptics = rememberStudioHaptics()
    val scope = rememberCoroutineScope()

    val credType = preferences.parsedLockCredentialType
    var entered by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }

    fun triggerError(message: String) {
        errorMessage = message
        haptics.performTick(45L)
        scope.launch {
            val amplitudes = listOf(-22f, 20f, -14f, 10f, -5f, 0f)
            amplitudes.forEach { target ->
                shakeOffset.animateTo(
                    targetValue = target,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = 1400f
                    )
                )
            }
        }
    }

    fun attemptVerify(candidate: String, clearOnFail: Boolean = true) {
        if (LockCredentialHasher.verify(candidate, preferences.lockCredentialSalt, preferences.lockCredentialHash)) {
            errorMessage = null
            haptics.performConfirm()
            onUnlockSuccess()
        } else if (clearOnFail) {
            entered = ""
            triggerError(if (credType == LockCredentialType.PIN) "Incorrect PIN" else "Incorrect password")
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("lockscreen_credential_gate")
    ) {
        val compactHeight = maxHeight < 560.dp
        val keySize = if (compactHeight) 54.dp else 72.dp
        val rowSpacing = if (compactHeight) 10.dp else 14.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = if (compactHeight) 6.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
        // Top advisory badge + header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = when {
                    isGlyph -> Color(0xFF141414)
                    isGlass -> Color.White.copy(alpha = 0.14f)
                    else -> Color(0xFF47310A).copy(alpha = 0.55f)
                },
                border = BorderStroke(
                    1.dp,
                    when {
                        isGlyph -> Color.White.copy(alpha = 0.28f)
                        isGlass -> Color.White.copy(alpha = 0.30f)
                        else -> Color(0xFFFFCA62).copy(alpha = 0.45f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFFFD166),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Backdrop Lock • Not as secure as Android Keyguard",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
            }

            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(28.dp)
            )

            Text(
                text = if (credType == LockCredentialType.PIN) "Enter Backdrop PIN" else "Enter Backdrop Password",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            AnimatedContent(
                targetState = errorMessage,
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(120)) },
                label = "cred_error_anim"
            ) { msg ->
                if (!msg.isNullOrBlank()) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF8A8A),
                        modifier = Modifier.testTag("lockscreen_cred_error")
                    )
                } else {
                    Text(
                        text = if (credType == LockCredentialType.PIN) "Tap digits to unlock" else "Type password and submit",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.68f)
                    )
                }
            }
        }

        if (credType == LockCredentialType.PIN) {
            // PIN indicator dots + UI-specific Keypad
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth()
            ) {
                // Animated PIN dots
                Row(
                    modifier = Modifier
                        .graphicsLayer { translationX = shakeOffset.value }
                        .height(28.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val slotCount = maxOf(4, entered.length).coerceAtMost(8)
                    repeat(slotCount) { idx ->
                        val filled = idx < entered.length
                        val dotScale by animateFloatAsState(
                            targetValue = if (filled) 1.18f else 0.85f,
                            animationSpec = if (isGlass) {
                                spring(dampingRatio = 0.42f, stiffness = 360f)
                            } else {
                                spring(dampingRatio = 0.65f, stiffness = 620f)
                            },
                            label = "pin_dot_$idx"
                        )
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .graphicsLayer {
                                    scaleX = dotScale
                                    scaleY = dotScale
                                }
                                .clip(CircleShape)
                                .background(
                                    if (filled) Color.White else Color.White.copy(alpha = 0.24f)
                                )
                                .then(
                                    if (!filled && isGlass) {
                                        Modifier.border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                    } else Modifier
                                )
                        )
                    }
                }

                // 3x4 PIN Keypad
                UiSpecificPinKeypad(
                    isGlass = isGlass,
                    isGlyph = isGlyph,
                    canSubmit = entered.length >= 4,
                    keySize = keySize,
                    rowSpacing = rowSpacing,
                    onDigitTap = { digit ->
                        if (entered.length < 8) {
                            errorMessage = null
                            haptics.performTick(18L)
                            val updated = entered + digit
                            entered = updated
                            if (updated.length >= 4) {
                                if (LockCredentialHasher.verify(updated, preferences.lockCredentialSalt, preferences.lockCredentialHash)) {
                                    haptics.performConfirm()
                                    onUnlockSuccess()
                                } else if (updated.length == 8) {
                                    entered = ""
                                    triggerError("Incorrect PIN")
                                }
                            }
                        }
                    },
                    onBackspace = {
                        if (entered.isNotEmpty()) {
                            haptics.performTick(14L)
                            entered = entered.dropLast(1)
                            errorMessage = null
                        }
                    },
                    onSubmit = {
                        if (entered.length >= 4) {
                            attemptVerify(entered, clearOnFail = true)
                        }
                    }
                )
            }
        } else {
            // Password Unlock Card
            Surface(
                shape = RoundedCornerShape(
                    when {
                        isGlyph -> 20.dp
                        isGlass -> 30.dp
                        else -> 24.dp
                    }
                ),
                color = when {
                    isGlyph -> Color(0xFF121212)
                    isGlass -> Color.White.copy(alpha = 0.14f)
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
                },
                border = when {
                    isGlyph -> BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                    isGlass -> BorderStroke(1.1.dp, Color.White.copy(alpha = 0.34f))
                    else -> null
                },
                tonalElevation = if (isGlass || isGlyph) 0.dp else 4.dp,
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .graphicsLayer { translationX = shakeOffset.value }
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val primaryTextColor = if (isGlass || isGlyph) Color.White else MaterialTheme.colorScheme.onSurface
                    OutlinedTextField(
                        value = entered,
                        onValueChange = {
                            entered = it
                            errorMessage = null
                        },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { attemptVerify(entered, clearOnFail = true) }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = primaryTextColor.copy(alpha = 0.78f)
                                )
                            }
                        },
                        shape = RoundedCornerShape(if (isGlyph) 14.dp else 18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = primaryTextColor,
                            unfocusedTextColor = primaryTextColor,
                            focusedBorderColor = when {
                                isGlyph -> Color.White
                                isGlass -> Color.White.copy(alpha = 0.65f)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            unfocusedBorderColor = primaryTextColor.copy(alpha = 0.35f),
                            focusedLabelColor = primaryTextColor.copy(alpha = 0.85f),
                            unfocusedLabelColor = primaryTextColor.copy(alpha = 0.68f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lockscreen_password_input")
                    )

                    Button(
                        onClick = { attemptVerify(entered, clearOnFail = true) },
                        enabled = entered.isNotEmpty(),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                isGlyph -> Color.White
                                isGlass -> Color.White.copy(alpha = 0.88f)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            contentColor = when {
                                isGlyph -> Color.Black
                                isGlass -> Color(0xFF10131C)
                                else -> MaterialTheme.colorScheme.onPrimary
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("lockscreen_password_submit")
                    ) {
                        Text("Unlock", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        TextButton(
            onClick = {
                entered = ""
                errorMessage = null
                onCancelToClock()
            },
            modifier = Modifier
                .padding(bottom = 8.dp)
                .height(48.dp)
                .testTag("lockscreen_cred_cancel_button")
        ) {
            Text(
                text = "Cancel • Back to Lock Screen",
                color = Color.White.copy(alpha = 0.86f),
                style = MaterialTheme.typography.labelLarge
            )
        }
        }
    }
}

@Composable
private fun UiSpecificPinKeypad(
    isGlass: Boolean,
    isGlyph: Boolean = false,
    canSubmit: Boolean,
    keySize: androidx.compose.ui.unit.Dp = 72.dp,
    rowSpacing: androidx.compose.ui.unit.Dp = 14.dp,
    onDigitTap: (String) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit
) {
    val latestOnDigitTap by rememberUpdatedState(onDigitTap)
    val latestOnBackspace by rememberUpdatedState(onBackspace)
    val latestOnSubmit by rememberUpdatedState(onSubmit)
    val rows = remember {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("BACK", "0", "OK")
        )
    }

    val keyCenters = remember { mutableStateMapOf<String, Offset>() }
    var activePressedKey by remember { mutableStateOf<String?>(null) }
    val density = LocalDensity.current
    val lightRadiusPx = with(density) { (keySize * 2.6f).toPx() }

    val activeGlowStrength by animateFloatAsState(
        targetValue = if (activePressedKey != null) 1f else 0f,
        animationSpec = if (isGlass) {
            spring(dampingRatio = 0.50f, stiffness = 420f)
        } else {
            tween(90)
        },
        label = "keypad_spill_glow"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(rowSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        rows.forEach { rowKeys ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowKeys.forEach { keyId ->
                    val isDigit = keyId.length == 1 && keyId[0].isDigit()
                    val isPressed = activePressedKey == keyId

                    val myCenter = keyCenters[keyId]
                    val pressedCenter = activePressedKey?.let { keyCenters[it] }

                    // Frosted OS signature physics: radiating directional illumination onto surrounding untapped keys
                    // for the EXACT duration the pressed button is held down.
                    val neighborGlow = if (isGlass && !isPressed && activeGlowStrength > 0.01f && myCenter != null && pressedCenter != null) {
                        val distance = hypot(myCenter.x - pressedCenter.x, myCenter.y - pressedCenter.y)
                        val norm = (1f - (distance / lightRadiusPx)).coerceIn(0f, 1f)
                        norm.pow(1.35f) * activeGlowStrength
                    } else 0f

                    val keyScale by animateFloatAsState(
                        targetValue = when {
                            isPressed && isGlass -> 0.87f
                            isPressed -> 0.91f
                            isGlass && neighborGlow > 0.25f -> 1f + 0.025f * neighborGlow
                            else -> 1f
                        },
                        animationSpec = if (isGlass) {
                            spring(
                                dampingRatio = 0.40f,
                                stiffness = 280f
                            )
                        } else {
                            spring(
                                dampingRatio = 0.65f,
                                stiffness = 620f
                            )
                        },
                        label = "key_scale_$keyId"
                    )

                    val m3TonalFlash by animateFloatAsState(
                        targetValue = if (!isGlass && !isGlyph && isPressed) 1f else 0f,
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = 640f),
                        label = "m3_flash_$keyId"
                    )

                    val keyModifier = Modifier
                        .size(keySize)
                        .onGloballyPositioned { coords ->
                            keyCenters[keyId] = coords.boundsInParent().center
                        }
                        .graphicsLayer {
                            scaleX = keyScale
                            scaleY = keyScale
                        }
                        .clip(CircleShape)
                        .semantics { role = Role.Button }

                    if (isDigit) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = keyModifier
                                .then(
                                    when {
                                        isGlyph -> {
                                            Modifier
                                                .background(if (isPressed) Color.White else Color(0xFF141414))
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isPressed) Color.White else Color.White.copy(alpha = 0.28f),
                                                    shape = CircleShape
                                                )
                                        }
                                        isGlass -> {
                                            Modifier
                                                .background(
                                                    Color.White.copy(
                                                        alpha = if (isPressed) 0.30f else 0.12f + 0.14f * neighborGlow
                                                    )
                                                )
                                                .drawBehind {
                                                    if (isPressed || neighborGlow > 0.02f) {
                                                        val origin = if (isPressed || pressedCenter == null || myCenter == null) {
                                                            center
                                                        } else {
                                                            val dx = pressedCenter.x - myCenter.x
                                                            val dy = pressedCenter.y - myCenter.y
                                                            val len = hypot(dx, dy).coerceAtLeast(1f)
                                                            Offset(
                                                                x = center.x + (dx / len) * (size.width * 0.46f),
                                                                y = center.y + (dy / len) * (size.height * 0.46f)
                                                            )
                                                        }
                                                        val intensity = if (isPressed) 0.65f else neighborGlow * 0.55f
                                                        drawCircle(
                                                            brush = Brush.radialGradient(
                                                                colors = listOf(
                                                                    Color.White.copy(alpha = intensity),
                                                                    Color(0xFFCDE6FF).copy(alpha = intensity * 0.45f),
                                                                    Color.Transparent
                                                                ),
                                                                center = origin,
                                                                radius = size.minDimension * 1.15f
                                                            )
                                                        )
                                                    }
                                                }
                                                .border(
                                                    width = if (isPressed) 1.5.dp else (1.0f + 0.45f * neighborGlow).dp,
                                                    color = Color.White.copy(
                                                        alpha = if (isPressed) 0.75f else (0.24f + 0.48f * neighborGlow).coerceAtMost(0.85f)
                                                    ),
                                                    shape = CircleShape
                                                )
                                        }
                                        else -> {
                                            // Flat Pixel Material 3 tonal surface
                                            Modifier.background(
                                                if (m3TonalFlash > 0.1f) {
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.84f)
                                                }
                                            )
                                        }
                                    }
                                )
                                .pointerInput(keyId) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        activePressedKey = keyId
                                        val up = waitForUpOrCancellation()
                                        if (activePressedKey == keyId) {
                                            activePressedKey = null
                                        }
                                        if (up != null) {
                                            latestOnDigitTap(keyId)
                                        }
                                    }
                                }
                                .semantics { contentDescription = "Digit $keyId" }
                                .testTag("lockscreen_pin_key_$keyId")
                        ) {
                            Text(
                                text = keyId,
                                fontSize = if (keySize < 64.dp) 22.sp else 26.sp,
                                fontFamily = if (isGlyph) DotGothicFontFamily else MaterialTheme.typography.headlineMedium.fontFamily,
                                fontWeight = if (isGlass || isGlyph) FontWeight.Medium else FontWeight.SemiBold,
                                color = when {
                                    isGlyph -> if (isPressed) Color.Black else Color.White
                                    isGlass -> Color.White
                                    m3TonalFlash > 0.35f -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                textAlign = TextAlign.Center
                            )
                        }
                    } else if (keyId == "BACK") {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = keyModifier
                                .background(
                                    when {
                                        isGlyph -> if (isPressed) Color.White else Color(0xFF141414)
                                        isGlass -> Color.White.copy(alpha = if (isPressed) 0.24f else 0.08f + 0.10f * neighborGlow)
                                        else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
                                    }
                                )
                                .then(
                                    when {
                                        isGlyph -> Modifier.border(1.dp, Color.White.copy(alpha = 0.24f), CircleShape)
                                        isGlass -> Modifier.border(
                                            1.dp,
                                            Color.White.copy(alpha = 0.18f + 0.36f * neighborGlow),
                                            CircleShape
                                        )
                                        else -> Modifier
                                    }
                                )
                                .pointerInput(keyId) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        activePressedKey = keyId
                                        val up = waitForUpOrCancellation()
                                        if (activePressedKey == keyId) activePressedKey = null
                                        if (up != null) latestOnBackspace()
                                    }
                                }
                                .testTag("lockscreen_pin_key_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                contentDescription = "Delete digit",
                                tint = when {
                                    isGlyph -> if (isPressed) Color.Black else Color.White
                                    isGlass -> Color.White
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = keyModifier
                                .background(
                                    when {
                                        !canSubmit -> when {
                                            isGlyph -> Color(0xFF0E0E0E)
                                            isGlass -> Color.White.copy(alpha = 0.05f)
                                            else -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.35f)
                                        }
                                        isGlyph -> Color(0xFFD92B2B)
                                        isGlass -> Color.White.copy(alpha = if (isPressed) 0.36f else 0.18f + 0.12f * neighborGlow)
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                                .then(
                                    when {
                                        isGlyph -> Modifier.border(
                                            1.dp,
                                            if (canSubmit) Color(0xFFD92B2B) else Color.White.copy(alpha = 0.16f),
                                            CircleShape
                                        )
                                        isGlass && canSubmit -> Modifier.border(
                                            1.dp,
                                            Color.White.copy(alpha = 0.35f + 0.35f * neighborGlow),
                                            CircleShape
                                        )
                                        else -> Modifier
                                    }
                                )
                                .clickable(enabled = canSubmit, onClick = { latestOnSubmit() })
                                .testTag("lockscreen_pin_key_ok")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Submit PIN",
                                tint = when {
                                    !canSubmit -> Color.White.copy(alpha = 0.35f)
                                    isGlass || isGlyph -> Color.White
                                    else -> MaterialTheme.colorScheme.onPrimary
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
