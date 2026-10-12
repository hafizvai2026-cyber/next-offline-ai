package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.ElectricBlue

data class ToolCategory(
    val title: String,
    val promptTemplate: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun WritingAssistantScreen(
    viewModel: MainViewModel,
    onNavigateToChat: () -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    var selectedTone by remember { mutableStateOf("Professional") }
    var selectedCategory by remember { mutableStateOf("লেখা সুন্দর করুন") }

    val tones = listOf(
        "Professional" to "প্রফেশনাল",
        "Friendly" to "বন্ধুত্বপূর্ণ",
        "Formal" to "আনুষ্ঠানিক",
        "Emotional" to "আবেগপূর্ণ",
        "Simple" to "সহজ ও প্রাঞ্জল",
        "Educational" to "শিক্ষামূলক",
        "Creative" to "সৃজনশীল",
        "Concise" to "সংক্ষিপ্ত",
        "Detailed" to "বিস্তারিত"
    )

    val tools = listOf(
        ToolCategory("লেখা সুন্দর করুন", "নিচের লেখাটি $selectedTone টোনে সুন্দর ও মার্জিত করে দিন:\n\n", Icons.Default.FormatPaint),
        ToolCategory("বানান ঠিক করুন", "নিচের লেখার বানান ও ব্যাকরণগত ভুল সংশোধন করে দিন:\n\n", Icons.Default.Spellcheck),
        ToolCategory("সংক্ষেপে লিখুন", "নিচের লেখাটির মূল ভাব বজায় রেখে সংক্ষেপে সারসংক্ষেপ তৈরি করুন:\n\n", Icons.Default.AutoAwesome),
        ToolCategory("বিস্তারিত লিখুন", "নিচের বিষয়ের ওপর $selectedTone টোনে বিস্তারিত বিশ্লেষণ ও প্রবন্ধ লিখুন:\n\n", Icons.Default.Create),
        ToolCategory("ইংরেজিতে অনুবাদ", "নিচের লেখাটি নির্ভুল ও প্রাঞ্জল ইংরেজিতে অনুবাদ করুন:\n\n", Icons.Default.Translate),
        ToolCategory("বাংলায় অনুবাদ", "Translate this text into fluent and natural Bengali:\n\n", Icons.Default.Translate),
        ToolCategory("ফেসবুক পোস্ট", "$selectedTone টোনে একটি আকর্ষণীয় ফেসবুক পোস্ট ও ক্যাপশন লিখে দিন:\n\n", Icons.Default.Create),
        ToolCategory("প্রফেশনাল ইমেইল", "$selectedTone টোনে একটি পেশাদার প্রাতিষ্ঠানিক ইমেইল লিখে দিন:\n\n", Icons.Default.Create),
        ToolCategory("দরখাস্ত / আবেদনপত্র", "একটি উপযুক্ত ও আনুষ্ঠানিক দরখাস্ত / আবেদনপত্রের খসড়া লিখে দিন। বিষয়:\n\n", Icons.Default.Create),
        ToolCategory("গল্প ও কবিতা", "$selectedTone ধারায় একটি ছোট গল্প বা অনুপ্রেরণাদায়ী কবিতা লিখুন:\n\n", Icons.Default.Create)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        item {
            Text(
                text = "বাংলা লেখালেখি ও এআই অ্যাসিস্ট্যান্ট",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ইন্টারনেট ছাড়া যেকোনো লেখা তৈরি, পরিমার্জন বা অনুবাদ করুন",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tool selector cards
            Text(
                text = "টুল নির্বাচন করুন",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tools) { tool ->
                    val isSelected = tool.title == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = tool.title },
                        label = { Text(tool.title, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(tool.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tone selector
            Text(
                text = "লেখার টোন / মেজাজ নির্বাচন করুন",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tones) { (key, label) ->
                    val isSelected = selectedTone == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTone = key },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input Field Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "আপনার মূল বক্তব্য বা বিষয়বস্তু দিন:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rawText,
                        onValueChange = { rawText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("writing_tool_input"),
                        placeholder = {
                            Text(
                                text = "উদাহরণ: 'আজকের সভায় উপস্থিত না থাকার কারণ জানিয়ে দরখাস্ত' বা কোনো খসড়া টেক্সট...",
                                fontSize = 13.sp
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val activeTool = tools.find { it.title == selectedCategory }
                            val promptToSend = "${activeTool?.promptTemplate ?: ""}$rawText"
                            viewModel.startNewConversation(initialInstruction = selectedCategory) {
                                viewModel.sendMessage(promptToSend)
                                onNavigateToChat()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_writing_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("তৈরি করুন ($selectedCategory)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
