package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.media.OfflinePackProgress
import com.example.data.model.AppLanguageOption
import com.example.data.model.OfflinePackSizeOption
import com.example.data.model.ThemeMode
import com.example.data.model.UiStyleMode
import com.example.data.model.UserPreferencesDoc
import com.example.ui.components.BackdropWordmark
import com.example.ui.components.TactileSurface

private data class TutorialCardSpec(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val highlight: String
)

@Composable
fun OnboardingFlowScreen(
    preferences: UserPreferencesDoc,
    onLanguageSelected: (String) -> Unit,
    onUiStyleSelected: (UiStyleMode) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onAmoledDarkSelected: (Boolean) -> Unit,
    onDynamicThemingSelected: (Boolean) -> Unit,
    onClassicHandlesSelected: (Boolean) -> Unit,
    onCompleteOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
    initialStep: Int = 0,
    offlinePackProgress: OfflinePackProgress = OfflinePackProgress(),
    onStartOfflinePack: (OfflinePackSizeOption) -> Unit = {},
    onPauseOfflinePack: () -> Unit = {},
    onCancelOfflinePack: () -> Unit = {},
    onClearOfflinePack: () -> Unit = {}
) {
    var step by remember(initialStep) { mutableIntStateOf(initialStep.coerceIn(0, 3)) }
    val totalSteps = 4
    var selectedOfflineOption by remember(preferences.offlinePackTargetCount) {
        mutableIntStateOf(OfflinePackSizeOption.fromCount(preferences.offlinePackTargetCount).ordinal)
    }
    val activeOfflineOption = OfflinePackSizeOption.entries.getOrElse(selectedOfflineOption) { OfflinePackSizeOption.STUDIO }

    val tutorialCards = remember {
        listOf(
            TutorialCardSpec(
                icon = Icons.Rounded.Layers,
                title = "Spatial Depth Engine V5",
                body = "Isolate foreground & portrait subjects cleanly with ML Kit + CIE Lab segmentation and precision Mask Brushes.",
                highlight = "Spatial Depth V5"
            ),
            TutorialCardSpec(
                icon = Icons.Rounded.LockClock,
                title = "8 Brand Lock Notification Presets",
                body = "Style lock notifications after leading mobile themes alongside bottom drag-to-expand and bottom port charging glow.",
                highlight = "Lock Screen Mode 1.5"
            ),
            TutorialCardSpec(
                icon = Icons.Rounded.AutoAwesome,
                title = "10 Studio Fonts, Glow & Liquid Glass Tiers",
                body = "Customize typography across the app, shape luminous clock glow edges, and tune hardware-matched Liquid Glass shader tiers.",
                highlight = "V5 Multi-Engine Architecture"
            ),
            TutorialCardSpec(
                icon = Icons.Rounded.Widgets,
                title = "22 Home & Lock Widgets",
                body = "Pin live World Clock, Focus Timer, Habit Streak, Step Counter, Compass, Network Speed & Weather widgets inside Lock Screen Mode and on Home.",
                highlight = "22-Widget Suite"
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.navigationBars))
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag("onboarding_flow_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar with Backdrops logo + Skip
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackdropWordmark(markSize = 28.dp)
                    TextButton(
                        onClick = onCompleteOnboarding,
                        modifier = Modifier.testTag("onboarding_skip_button")
                    ) {
                        Text(if (step == 3) "Skip Tutorial" else "Skip Setup")
                    }
                }
                LinearProgressIndicator(
                    progress = { (step + 1) / totalSteps.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main step body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (step) {
                    0 -> {
                        // Step 1: 9-Language Selection (including Arabic RTL)
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = stringResource(id = R.string.onboarding_welcome),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Choose your preferred language. Selecting Arabic mirrors the interface right-to-left automatically.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 146.dp),
                                contentPadding = PaddingValues(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(AppLanguageOption.entries) { lang ->
                                    val selected = preferences.parsedLanguage == lang
                                    Surface(
                                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = RoundedCornerShape(18.dp),
                                        border = BorderStroke(
                                            width = if (selected) 2.dp else 1.dp,
                                            color = if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(18.dp))
                                            .clickable { onLanguageSelected(lang.code) }
                                            .testTag("onboarding_lang_${lang.code}")
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = lang.nativeLabel,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = lang.englishLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Step 2: Interface Design System
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Pick Your Interface System",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Switch anytime in Studio Settings.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            UiStyleMode.entries.forEach { mode ->
                                val selected = preferences.parsedUiStyle == mode
                                TactileSurface(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .clickable { onUiStyleSelected(mode) }
                                        .testTag("onboarding_uistyle_${mode.name}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(18.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.Layers,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(mode.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(mode.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Step 3: Theme, Studio Preferences & Optional Offline Wallpaper Pack
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Tune Theme & Offline Pack",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                val modes = listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark")
                                modes.forEachIndexed { idx, (mode, label) ->
                                    SegmentedButton(
                                        selected = preferences.parsedThemeMode == mode,
                                        onClick = { onThemeModeSelected(mode) },
                                        shape = SegmentedButtonDefaults.itemShape(idx, modes.size)
                                    ) { Text(label) }
                                }
                            }
                            OnboardingSwitchRow(
                                title = "AMOLED Pitch Black",
                                subtitle = "True #000000 backdrop when dark theme is active",
                                checked = preferences.amoledDark,
                                onCheckedChange = onAmoledDarkSelected
                            )
                            OnboardingSwitchRow(
                                title = "Dynamic Material You Palette",
                                subtitle = "Adapt colors automatically from your system wallpaper",
                                checked = preferences.dynamicThemingEnabled,
                                onCheckedChange = onDynamicThemingSelected
                            )
                            OnboardingSwitchRow(
                                title = "Use Classic Resize Handles",
                                subtitle = "Show traditional square corner boxes in Depth Studio",
                                checked = preferences.useClassicClockHandles,
                                onCheckedChange = onClassicHandlesSelected
                            )
                            OfflinePackSettingsCard(
                                progress = offlinePackProgress,
                                selectedOption = activeOfflineOption,
                                onSelectOption = { selectedOfflineOption = it.ordinal },
                                onStartOrResume = { onStartOfflinePack(activeOfflineOption) },
                                onPause = onPauseOfflinePack,
                                onCancel = onCancelOfflinePack,
                                onClearPack = onClearOfflinePack
                            )
                        }
                    }

                    else -> {
                        // Step 4: Skippable 4-Card Quick Tutorial
                        val pagerState = rememberPagerState(pageCount = { tutorialCards.size })
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "What's Inside Backdrop 1.5",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                pageSpacing = 14.dp
                            ) { idx ->
                                val card = tutorialCards[idx]
                                Surface(
                                    shape = RoundedCornerShape(30.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                                                        MaterialTheme.colorScheme.surfaceContainerLow
                                                    )
                                                )
                                            )
                                            .padding(24.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape,
                                            modifier = Modifier.size(64.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = card.icon,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(30.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = card.highlight,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = card.title,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = card.body,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(tutorialCards.size) { i ->
                                    val active = pagerState.currentPage == i
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 3.dp)
                                            .height(7.dp)
                                            .width(if (active) 22.dp else 7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (active) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outlineVariant
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step -= 1 },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Back")
                    }
                }
                if (step < totalSteps - 1) {
                    Button(
                        onClick = { step += 1 },
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("onboarding_next_button")
                    ) {
                        Text("Continue ")
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Button(
                        onClick = onCompleteOnboarding,
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("onboarding_finish_button")
                    ) {
                        Text("Start Exploring")
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    TactileSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
