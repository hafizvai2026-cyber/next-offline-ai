package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val context = LocalContext.current
    val blocks = parseMarkdownBlocks(content)

    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.CodeBlock -> {
                    CodeBlockView(
                        language = block.language,
                        code = block.code,
                        context = context
                    )
                }
                is MarkdownBlock.Heading -> {
                    val fontSize = when (block.level) {
                        1 -> 20.sp
                        2 -> 18.sp
                        else -> 16.sp
                    }
                    Text(
                        text = renderInlineMarkdown(block.text),
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = (fontSize.value * 1.3).sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                is MarkdownBlock.Quote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = renderInlineMarkdown(block.text),
                            fontSize = 14.sp,
                            color = textColor.copy(alpha = 0.9f),
                            lineHeight = 22.sp
                        )
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = renderInlineMarkdown(block.text),
                        fontSize = 15.sp,
                        color = textColor,
                        lineHeight = 23.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CodeBlockView(
    language: String,
    code: String,
    context: Context
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0D1424))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF162238))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.ifBlank { "code" }.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64B5F6),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Code", code))
                    Toast.makeText(context, "কোড কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .size(32.dp)
                    .testTag("copy_code_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "কোড কপি করুন",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = Color(0xFFE2E8F0),
                lineHeight = 19.sp
            )
        }
    }
}

sealed class MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock()
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class Quote(val text: String) : MarkdownBlock()
}

fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.split("\n")
    var inCodeBlock = false
    var codeLang = ""
    val codeContent = StringBuilder()
    val paraContent = StringBuilder()

    for (line in lines) {
        if (line.trim().startsWith("```")) {
            if (inCodeBlock) {
                blocks.add(MarkdownBlock.CodeBlock(codeLang, codeContent.toString().trimEnd()))
                codeContent.clear()
                inCodeBlock = false
            } else {
                if (paraContent.isNotBlank()) {
                    blocks.add(MarkdownBlock.Paragraph(paraContent.toString().trim()))
                    paraContent.clear()
                }
                inCodeBlock = true
                codeLang = line.trim().removePrefix("```").trim()
            }
            continue
        }

        if (inCodeBlock) {
            codeContent.append(line).append("\n")
            continue
        }

        if (line.trim().startsWith("#")) {
            if (paraContent.isNotBlank()) {
                blocks.add(MarkdownBlock.Paragraph(paraContent.toString().trim()))
                paraContent.clear()
            }
            val level = line.takeWhile { it == '#' }.length.coerceIn(1, 3)
            val headingText = line.drop(level).trim()
            blocks.add(MarkdownBlock.Heading(level, headingText))
            continue
        }

        if (line.trim().startsWith(">")) {
            if (paraContent.isNotBlank()) {
                blocks.add(MarkdownBlock.Paragraph(paraContent.toString().trim()))
                paraContent.clear()
            }
            blocks.add(MarkdownBlock.Quote(line.trim().removePrefix(">").trim()))
            continue
        }

        if (line.isBlank()) {
            if (paraContent.isNotBlank()) {
                blocks.add(MarkdownBlock.Paragraph(paraContent.toString().trim()))
                paraContent.clear()
            }
        } else {
            if (paraContent.isNotEmpty()) paraContent.append("\n")
            paraContent.append(line)
        }
    }

    if (inCodeBlock) {
        blocks.add(MarkdownBlock.CodeBlock(codeLang, codeContent.toString().trimEnd()))
    } else if (paraContent.isNotBlank()) {
        blocks.add(MarkdownBlock.Paragraph(paraContent.toString().trim()))
    }

    return blocks
}

fun renderInlineMarkdown(text: String) = buildAnnotatedString {
    var index = 0
    val boldPattern = Regex("""\*\*(.*?)\*\*|`(.*?)`""")
    val matches = boldPattern.findAll(text)

    for (match in matches) {
        if (match.range.first > index) {
            append(text.substring(index, match.range.first))
        }
        val boldText = match.groups[1]?.value
        val codeText = match.groups[2]?.value

        if (boldText != null) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(boldText)
            }
        } else if (codeText != null) {
            withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = Color(0x33888888)
                )
            ) {
                append(" $codeText ")
            }
        }
        index = match.range.last + 1
    }

    if (index < text.length) {
        append(text.substring(index))
    }
}
