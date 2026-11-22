package com.myexpenses.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myexpenses.app.data.local.entity.CategoryEntity
import com.myexpenses.app.data.preferences.UserPreferences
import com.myexpenses.app.domain.DateTimeUtils
import com.myexpenses.app.ui.theme.AccentPeriwinkle
import com.myexpenses.app.ui.theme.NeonBlue
import com.myexpenses.app.ui.theme.NeonCyan
import com.myexpenses.app.ui.theme.NeonMagenta
import com.myexpenses.app.ui.viewmodel.VoiceSessionStage

private val CosmicTopBg = Color(0xFF060714)
private val CosmicBottomBg = Color(0xFF0E122B)
private val CosmicBorder = Color(0xFF232746)
private val CosmicSubtleText = Color(0xFFA0A8C8)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceOverlayPanel(
    stage: VoiceSessionStage,
    preferences: UserPreferences,
    categories: List<CategoryEntity>,
    isArabic: Boolean,
    hasMicPermission: Boolean,
    isStandalonePopup: Boolean,
    onRequestMicAndStart: () -> Unit,
    onStopListening: () -> Unit,
    onSwitchLanguage: (String) -> Unit,
    onSubmitTypedText: (String) -> Unit,
    onUpdatePendingCategory: (Int, CategoryEntity) -> Unit,
    onRemovePendingItem: (Int) -> Unit,
    onConfirmPending: () -> Unit,
    onUndoSaved: () -> Unit,
    onTriggerOfflineDownload: () -> Unit,
    onOpenSpeechSettings: () -> Unit,
    onEnableOnlineFallback: () -> Unit,
    onOpenFullApp: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var showTypeInput by remember { mutableStateOf(false) }
    var typedInput by remember { mutableStateOf("") }

    val panelShape = RoundedCornerShape(
        topStart = 32.dp,
        topEnd = 32.dp,
        bottomStart = if (isStandalonePopup) 32.dp else 0.dp,
        bottomEnd = if (isStandalonePopup) 32.dp else 0.dp
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(panelShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(CosmicBottomBg, CosmicTopBg)
                )
            )
            .border(1.dp, CosmicBorder, panelShape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: Language pills + utility icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sleek segmented language pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF141830),
                    border = BorderStroke(1.dp, CosmicBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        LanguagePillButton(
                            label = "عربي",
                            selected = preferences.speechLanguage == "ar",
                            onClick = { onSwitchLanguage("ar") }
                        )
                        LanguagePillButton(
                            label = "EN",
                            selected = preferences.speechLanguage == "en",
                            onClick = { onSwitchLanguage("en") }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showTypeInput = !showTypeInput }) {
                        Icon(
                            imageVector = Icons.Rounded.Keyboard,
                            contentDescription = if (isArabic) "إدخال نصي" else "Type command",
                            tint = if (showTypeInput) NeonCyan else CosmicSubtleText
                        )
                    }
                    if (isStandalonePopup && onOpenFullApp != null) {
                        IconButton(onClick = onOpenFullApp) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = if (isArabic) "فتح التطبيق" else "Open app",
                                tint = CosmicSubtleText
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = if (isArabic) "إغلاق" else "Close",
                            tint = CosmicSubtleText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Offline privacy pill
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF131936),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val systemRecognitionActive = stage is VoiceSessionStage.Listening && !stage.isOfflineEngine
                    Icon(
                        imageVector = if (systemRecognitionActive) Icons.Rounded.Cloud else Icons.Rounded.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = NeonCyan
                    )
                    Text(
                        text = when {
                            systemRecognitionActive ->
                                if (isArabic) "خدمة النظام قد تستخدم الإنترنت" else "System speech may use the Internet"
                            stage is VoiceSessionStage.Listening ->
                                if (isArabic) "التعرف على الجهاز" else "On-device recognition"
                            !preferences.allowOnlineSpeechFallback ->
                                if (isArabic) "التعرف المحلي فقط" else "On-device recognition only"
                            else ->
                                if (isArabic) "التعرف المحلي مع بديل سحابي" else "On-device with online fallback"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFD5E6FF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Dynamic stage content
            when (stage) {
                is VoiceSessionStage.Ready -> {
                    CosmicNeonMicOrb(
                        isListening = false,
                        rmsLevel = 0f,
                        onClick = onRequestMicAndStart
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (!hasMicPermission) {
                            if (isArabic) "اضغط لمنح صلاحية الميكروفون" else "Tap to grant microphone permission"
                        } else {
                            if (isArabic) "اضغط وتحدث بمصروفك" else "Tap & Speak"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isArabic) {
                            "\"صرفت 500 بقالة و1500 مواصلات\" أو \"كم صرفت اليوم؟\""
                        } else {
                            "\"Spent 500 groceries and 1500 fuel\" or \"How much today?\""
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = CosmicSubtleText,
                        textAlign = TextAlign.Center
                    )
                }

                is VoiceSessionStage.Listening -> {
                    CosmicNeonMicOrb(
                        isListening = true,
                        rmsLevel = stage.rmsLevel,
                        onClick = onStopListening
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (isArabic) "أستمع الآن..." else "Listening",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.92f),
                        letterSpacing = 0.4.sp
                    )
                    if (stage.partialText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF151A36),
                            border = BorderStroke(1.dp, AccentPeriwinkle.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stage.partialText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(onClick = onStopListening) {
                        Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp), tint = NeonCyan)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "إيقاف وتحليل" else "Stop & Parse",
                            color = NeonCyan
                        )
                    }
                }

                is VoiceSessionStage.NeedsConfirmation -> {
                    Text(
                        text = if (isArabic) "مراجعة قبل الحفظ (${stage.items.size})" else "Review Before Saving (${stage.items.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    val reason = if (isArabic) stage.reasonAr else stage.reasonEn
                    if (!reason.isNullOrBlank()) {
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonCyan,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "\"${stage.rawTranscript}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CosmicSubtleText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        stage.items.forEachIndexed { index, item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF141830)),
                                border = BorderStroke(1.dp, CosmicBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = DateTimeUtils.formatCurrency(item.amount, preferences.currency, isArabic),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentPeriwinkle
                                            )
                                            Text(
                                                text = "${item.note} • ${DateTimeUtils.formatDayHeader(item.date, isArabic)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White
                                            )
                                        }
                                        if (stage.items.size > 1) {
                                            IconButton(onClick = { onRemovePendingItem(index) }) {
                                                Icon(Icons.Rounded.Close, contentDescription = null, tint = CosmicSubtleText)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        categories.forEach { cat ->
                                            FilterChip(
                                                selected = item.categoryId == cat.id,
                                                onClick = { onUpdatePendingCategory(index, cat) },
                                                label = { Text(cat.displayName(isArabic)) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    containerColor = Color(0xFF1C2140),
                                                    labelColor = Color.White,
                                                    selectedContainerColor = AccentPeriwinkle.copy(alpha = 0.28f),
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRequestMicAndStart,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, CosmicBorder)
                        ) {
                            Text(if (isArabic) "إعادة التحدث" else "Retry", color = Color.White)
                        }
                        Button(
                            onClick = onConfirmPending,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPeriwinkle, contentColor = Color(0xFF0A0A18))
                        ) {
                            Text(if (isArabic) "حفظ الآن" else "Save Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is VoiceSessionStage.SavedSuccess -> {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(58.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isArabic) stage.summaryAr else stage.summaryEn,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"${stage.rawTranscript}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CosmicSubtleText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onUndoSaved,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, CosmicBorder)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isArabic) "تراجع" else "Undo", color = Color.White)
                        }
                        Button(
                            onClick = onRequestMicAndStart,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPeriwinkle, contentColor = Color(0xFF0A0A18))
                        ) {
                            Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isArabic) "تسجيل آخر" else "Record More", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is VoiceSessionStage.CommandResult -> {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF151B3A),
                        border = BorderStroke(1.dp, AccentPeriwinkle.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isArabic) stage.messageAr else stage.messageEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss, border = BorderStroke(1.dp, CosmicBorder)) {
                            Text(if (isArabic) "تم" else "Done", color = Color.White)
                        }
                        Button(
                            onClick = onRequestMicAndStart,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPeriwinkle, contentColor = Color(0xFF0A0A18))
                        ) {
                            Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isArabic) "تحدث مجددًا" else "Speak Again", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is VoiceSessionStage.OfflineModelRequired -> {
                    Icon(
                        imageVector = Icons.Rounded.WifiOff,
                        contentDescription = null,
                        tint = NeonMagenta,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isArabic) {
                            "حزمة التعرف الصوتي المحلي (${stage.languageTag}) غير مثبتة على جهازك"
                        } else {
                            "Offline speech model (${stage.languageTag}) is not installed on this device"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isArabic) {
                            "لحماية خصوصيتك، لا يرسل التطبيق صوتك إلى الإنترنت دون إذنك."
                        } else {
                            "For your privacy, audio is never sent online without your consent."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = CosmicSubtleText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    if (stage.canTriggerDownload) {
                        Button(
                            onClick = onTriggerOfflineDownload,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPeriwinkle, contentColor = Color(0xFF0A0A18))
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isArabic) "تنزيل حزمة اللغة المحلية" else "Download Offline Language Pack")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    OutlinedButton(
                        onClick = onOpenSpeechSettings,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, CosmicBorder)
                    ) {
                        Text(if (isArabic) "فتح إعدادات التعرف الصوتي للنظام" else "Open System Speech Settings", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(onClick = onEnableOnlineFallback) {
                        Text(
                            if (isArabic) "السماح بالبديل السحابي عند عدم توفر الحزمة المحلية"
                            else "Allow optional online speech fallback",
                            color = NeonCyan
                        )
                    }
                }

                is VoiceSessionStage.Error -> {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isArabic) stage.messageAr else stage.messageEn,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    if (stage.rawTranscript.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"${stage.rawTranscript}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CosmicSubtleText
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { showTypeInput = true },
                            border = BorderStroke(1.dp, CosmicBorder)
                        ) {
                            Text(if (isArabic) "كتابة يدوية" else "Type Instead", color = Color.White)
                        }
                        Button(
                            onClick = onRequestMicAndStart,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPeriwinkle, contentColor = Color(0xFF0A0A18))
                        ) {
                            Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isArabic) "حاول مجددًا" else "Try Again", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Optional Quick Text Input Bar
            if (showTypeInput) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = typedInput,
                    onValueChange = { typedInput = it },
                    placeholder = {
                        Text(
                            if (isArabic) "اكتب مثلًا: صرفت 500 بقالة و2000 غداء"
                            else "Type e.g.: spent 500 groceries and 2000 lunch",
                            color = CosmicSubtleText
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentPeriwinkle,
                        unfocusedBorderColor = CosmicBorder,
                        focusedContainerColor = Color(0xFF12162E),
                        unfocusedContainerColor = Color(0xFF12162E)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (typedInput.isNotBlank()) {
                            onSubmitTypedText(typedInput)
                            typedInput = ""
                        }
                    }),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (typedInput.isNotBlank()) {
                                    onSubmitTypedText(typedInput)
                                    typedInput = ""
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, tint = NeonCyan)
                        }
                    }
                )
            }

            if (stage is VoiceSessionStage.Ready && showTypeInput) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val samples = if (isArabic) {
                        listOf("صرفت 500 بقالة", "دفعت ألفين بنزين", "كم صرفت اليوم؟")
                    } else {
                        listOf("Spent 500 groceries", "Paid 2000 fuel", "How much today?")
                    }
                    samples.forEach { sample ->
                        AssistChip(
                            onClick = { onSubmitTypedText(sample) },
                            label = { Text(sample, color = Color.White) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguagePillButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) Color(0xFF282D54) else Color.Transparent,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) AccentPeriwinkle else CosmicSubtleText,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

/**
 * Cosmic Cyan-Magenta-Violet Glowing Voice Orb inspired by the user's reference design.
 * Rotates smoothly and breathes dynamically with real-time microphone RMS audio levels.
 */
@Composable
private fun CosmicNeonMicOrb(
    isListening: Boolean,
    rmsLevel: Float,
    onClick: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "cosmic_orb")

    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 2800 else 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rotation"
    )

    val breathe by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = if (isListening) 1.09f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 750 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_breathe"
    )

    val dynamicScale = if (isListening) breathe + (rmsLevel * 0.16f) else breathe

    Box(
        modifier = Modifier
            .size(164.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Outer multi-layer cosmic cyan + magenta + electric blue neon aura
        Canvas(
            modifier = Modifier
                .size(164.dp)
                .scale(dynamicScale)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension / 2f

            // Soft outer magenta/cyan radial bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonMagenta.copy(alpha = if (isListening) 0.36f else 0.22f),
                        NeonBlue.copy(alpha = if (isListening) 0.24f else 0.14f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxR
                ),
                radius = maxR,
                center = center
            )

            // Rotating multi-color neon ring halo
            rotate(degrees = rotation, pivot = center) {
                val sweepBrush = Brush.sweepGradient(
                    colors = listOf(
                        NeonCyan,
                        NeonBlue,
                        NeonMagenta,
                        AccentPeriwinkle,
                        NeonCyan
                    ),
                    center = center
                )
                // Soft wide halo ring
                drawCircle(
                    brush = sweepBrush,
                    radius = maxR * 0.68f,
                    center = center,
                    alpha = if (isListening) 0.55f else 0.32f,
                    style = Stroke(width = if (isListening) 24.dp.toPx() else 16.dp.toPx())
                )
                // Mid intense glow ring
                drawCircle(
                    brush = sweepBrush,
                    radius = maxR * 0.65f,
                    center = center,
                    alpha = if (isListening) 0.85f else 0.55f,
                    style = Stroke(width = 10.dp.toPx())
                )
                // Crisp luminous inner edge ring
                drawCircle(
                    brush = sweepBrush,
                    radius = maxR * 0.60f,
                    center = center,
                    alpha = 0.95f,
                    style = Stroke(width = 2.2.dp.toPx())
                )
            }
        }

        // Frosted dark glass inner sphere with crisp thin luminous rim
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF171E45), Color(0xFF0A0D20))
                    )
                )
                .border(
                    width = 1.3.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.7f),
                            NeonCyan.copy(alpha = 0.6f),
                            NeonMagenta.copy(alpha = 0.6f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isListening) Icons.Rounded.Mic else Icons.Rounded.Mic,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}
