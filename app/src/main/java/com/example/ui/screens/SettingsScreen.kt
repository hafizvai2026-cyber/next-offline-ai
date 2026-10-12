package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.ElectricBlue
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateToModels: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val language by viewModel.preferencesManager.languageFlow.collectAsState(initial = "auto")
    val responseLength by viewModel.preferencesManager.responseLengthFlow.collectAsState(initial = "balanced")
    val temperature by viewModel.preferencesManager.temperatureFlow.collectAsState(initial = 0.7f)
    val maxTokens by viewModel.preferencesManager.maxTokensFlow.collectAsState(initial = 512)
    val haptic by viewModel.preferencesManager.hapticFlow.collectAsState(initial = true)
    val enterToSend by viewModel.preferencesManager.enterToSendFlow.collectAsState(initial = false)
    val customInstruction by viewModel.preferencesManager.customInstructionFlow.collectAsState(initial = "")

    var sliderValue by remember(temperature) { mutableFloatStateOf(temperature) }
    var instructionInput by remember(customInstruction) { mutableStateOf(customInstruction) }
    var showClearConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        item {
            Text(
                text = "সেটিংস ও এআই কনফিগারেশন",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ব্যক্তিগত পছন্দ এবং অফলাইন রেসপন্স আচরণ কাস্টমাইজ করুন",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Language Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("ডিফল্ট রেসপন্স ভাষা", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val langs = listOf("auto" to "স্বয়ংক্রিয় (Auto)", "bengali" to "বাংলা", "english" to "English")
                        langs.forEach { (key, label) ->
                            FilterChip(
                                selected = language == key,
                                onClick = { scope.launch { viewModel.preferencesManager.setLanguage(key) } },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Temperature / Creativity Slider Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ক্রিয়েটিভিটি / টেম্পারেচার (Temperature)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(String.format("%.1f", sliderValue), fontSize = 13.sp, color = ElectricBlue, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "কম মান (০.২-০.৫) দিলে উত্তর হবে নির্ভুল ও সুনির্দিষ্ট। বেশি মান (০.৭-১.০) দিলে উত্তর হবে সৃজনশীল ও বিস্তারিত।",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = {
                            scope.launch { viewModel.preferencesManager.setTemperature(sliderValue) }
                        },
                        valueRange = 0.2f..1.0f,
                        steps = 7,
                        colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue),
                        modifier = Modifier.testTag("temperature_slider")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Custom Instructions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("কাস্টম সিস্টেম নির্দেশাবলী (Custom Instructions)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "এআই আপনার সাথে কীভাবে কথা বলবে তা নির্ধারণ করুন (যেমন: 'সর্বদা সহজ বাংলায় উত্তর দাও')",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = instructionInput,
                        onValueChange = { instructionInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch { viewModel.preferencesManager.setCustomInstruction(instructionInput.trim()) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("সংরক্ষণ করুন", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Toggles Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("হ্যাপটিক ফিডব্যাক (কম্পন)", fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                            Text("বাটনে ক্লিক ও রেসপন্সে হালকা কম্পন", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = haptic,
                            onCheckedChange = { scope.launch { viewModel.preferencesManager.setHaptic(it) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = ElectricBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Enter বাটনে বার্তা প্রেরণ", fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                            Text("কীবোর্ডের এন্টার চাপলে সরাসরি মেসেজ যাবে", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = enterToSend,
                            onCheckedChange = { scope.launch { viewModel.preferencesManager.setEnterToSend(it) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = ElectricBlue)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Danger Zone: Clear History
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("ইতিহাস ও ডেটা ব্যবস্থাপনা", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ডিভাইসে সংরক্ষিত সকল চ্যাট এবং বার্তা এক ক্লিকে স্থায়ীভাবে মুছে ফেলুন।",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showClearConfirm = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("clear_all_history_button")
                    ) {
                        Text("সকল কথোপকথন মুছে ফেলুন", fontSize = 12.sp, color = Color.Red)
                    }
                }
            }
        }
    }

    // Clear Confirm Dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("সব হিস্ট্রি মুছে ফেলবেন?", fontSize = 16.sp) },
            text = { Text("এই প্রক্রিয়াটি অপরিবর্তনীয়। আপনার সকল আগের কথোপকথন স্থায়ীভাবে হারিয়ে যাবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllConversations()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("সব মুছুন") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("বাতিল") }
            }
        )
    }
}
