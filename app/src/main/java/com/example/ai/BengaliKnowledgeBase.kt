package com.example.ai

import java.util.Locale

object BengaliKnowledgeBase {

    // Common Bengali spelling and grammar corrections
    val spellingCorrections = mapOf(
        "বিদায়" to "বিদায়",
        "ধন্যবাদ" to "ধন্যবাদ",
        "স্বাগতম" to "স্বাগতম",
        "অনিশ্চিত" to "অনিশ্চিত",
        "পুস্তক" to "বই / পুস্তক",
        "পুরষ্কার" to "পুরস্কার",
        "দুরবস্থা" to "দুরবস্থা (দূরবস্থা নয়)",
        "শিরচ্ছেদ" to "শিরশ্ছেদ",
        "উপরোক্ত" to "উপর্যুক্ত",
        "মুহুর্ত" to "মুহূর্ত",
        "শহীদ" to "শহিদ / শহীদ",
        "প্রতিযোগীতা" to "প্রতিযোগিতা",
        "দারিদ্রতা" to "দরিদ্রতা বা দারিদ্র্য",
        "সৌজন্যতা" to "সৌজন্য",
        "বিদ্যান" to "বিদ্বান"
    )

    fun detectTone(prompt: String): String {
        val lower = prompt.lowercase(Locale.ROOT)
        return when {
            lower.contains("প্রফেশনাল") || lower.contains("professional") || lower.contains("পেশাদার") -> "Professional"
            lower.contains("আনুষ্ঠানিক") || lower.contains("formal") || lower.contains("সরকারি") -> "Formal"
            lower.contains("বন্ধুত্বপূর্ণ") || lower.contains("friendly") || lower.contains("বন্ধুসুলভ") -> "Friendly"
            lower.contains("আবেগপূর্ণ") || lower.contains("emotional") || lower.contains("অনুভূতি") -> "Emotional"
            lower.contains("সহজ") || lower.contains("simple") || lower.contains("সহজ ভাষায়") -> "Simple"
            lower.contains("শিক্ষামূলক") || lower.contains("educational") -> "Educational"
            lower.contains("সৃজনশীল") || lower.contains("creative") || lower.contains("সাহিত্যিক") -> "Creative"
            lower.contains("সংক্ষেপে") || lower.contains("short") || lower.contains("concise") -> "Concise"
            lower.contains("বিস্তারিত") || lower.contains("detail") -> "Detailed"
            else -> "Balanced"
        }
    }

    fun isBengali(text: String): Boolean {
        // Bengali Unicode range: \u0980 - \u09FF
        return text.any { it in '\u0980'..'\u09FF' }
    }

    fun convertBengaliDigitsToAscii(text: String): String {
        return text.map { char ->
            when (char) {
                '০' -> '0'
                '১' -> '1'
                '২' -> '2'
                '৩' -> '3'
                '৪' -> '4'
                '৫' -> '5'
                '৬' -> '6'
                '৭' -> '7'
                '৮' -> '8'
                '৯' -> '9'
                else -> char
            }
        }.joinToString("")
    }

    fun convertAsciiDigitsToBengali(text: String): String {
        return text.map { char ->
            when (char) {
                '0' -> '০'
                '1' -> '১'
                '2' -> '২'
                '3' -> '৩'
                '4' -> '৪'
                '5' -> '৫'
                '6' -> '৬'
                '7' -> '৭'
                '8' -> '৮'
                '9' -> '৯'
                else -> char
            }
        }.joinToString("")
    }

    // Step-by-step Math calculation solver
    fun trySolveMath(prompt: String): String? {
        val normalized = convertBengaliDigitsToAscii(prompt)
        val clean = normalized.replace("কত", "").replace("যোগ", "+")
            .replace("বিয়োগ", "-").replace("গুণ", "*").replace("ভাগ", "/")
            .replace("হিসাব কর", "").replace("সমাধান কর", "")
            .replace("?", "").trim()

        val mathPattern = Regex("""(-?\d+\.?\d*)\s*([\+\-\*\/])\s*(-?\d+\.?\d*)""")
        val match = mathPattern.find(clean) ?: return null

        val (num1Str, op, num2Str) = match.destructured
        val n1 = num1Str.toDoubleOrNull() ?: return null
        val n2 = num2Str.toDoubleOrNull() ?: return null

        val (res, explanation) = when (op) {
            "+" -> (n1 + n2) to "যোগফল"
            "-" -> (n1 - n2) to "বিয়োগফল"
            "*" -> (n1 * n2) to "গুণফল"
            "/" -> {
                if (n2 == 0.0) return "⚠️ শূন্য (0) দিয়ে কোনো সংখ্যাকে ভাগ করা যায় না (অসংজ্ঞায়িত)।"
                (n1 / n2) to "ভাগফল"
            }
            else -> return null
        }

        val formattedRes = if (res % 1.0 == 0.0) res.toLong().toString() else "%.4f".format(res)
        val bnRes = convertAsciiDigitsToBengali(formattedRes)
        val bnNum1 = convertAsciiDigitsToBengali(num1Str)
        val bnNum2 = convertAsciiDigitsToBengali(num2Str)

        return """
### 🧮 গাণিতিক সমাধান (ধাপে ধাপে):
- **প্রদত্ত সমস্যা:** `$num1Str $op $num2Str` ($bnNum1 $op $bnNum2)
- **গাণিতিক প্রক্রিয়া:** $explanation
- **গণনা:** $num1Str $op $num2Str = **$formattedRes** ($bnRes)

**ফলাফল:** **$formattedRes** ($bnRes)
        """.trimIndent()
    }

    // Code Generation & Analysis
    fun tryGenerateCode(prompt: String): String? {
        val lower = prompt.lowercase(Locale.ROOT)
        return when {
            lower.contains("python") && (lower.contains("hello") || lower.contains("শুরু")) -> """
### 🐍 পাইথন (Python) কোড উদাহরণ:

```python
# Next Offline Chat AI - Python Starter
def greet(name: str):
    print(f"আসসালামু আলাইকুম, {name}! পাইথনে স্বাগতম।")

if __name__ == "__main__":
    greet("বন্ধু")
```

**ব্যাখ্যা:**
1. `def` দিয়ে ফাংশন তৈরি করা হয়েছে।
2. `f-string` ব্যবহার করে সহজে টেক্সট প্রিন্ট করা হয়েছে।
3. সম্পূর্ণ অফলাইনে কোডটি রান করতে পারেন।
            """.trimIndent()

            lower.contains("kotlin") || lower.contains("android") -> """
### 📱 কোটলিন (Kotlin / Android) কোড:

```kotlin
// Next Offline Chat AI - Kotlin Helper
fun main() {
    val message = "Next Offline Chat AI অফলাইনে সচল!"
    println(message)
}
```

**টিপস:** কোটলিনে নাল-সেফটি এবং করোoutine ব্যবহার করে লাইটওয়েট ব্যাকগ্রাউন্ড প্রসেসিং করা যায়।
            """.trimIndent()

            lower.contains("html") || lower.contains("ওয়েবসাইট") || lower.contains("ওয়েব") -> """
### 🌐 এইচটিএমএল (HTML5) কাঠামো:

```html
<!DOCTYPE html>
<html lang="bn">
<head>
    <meta charset="UTF-8">
    <title>Next Graphics - অফলাইন পেজ</title>
    <style>
        body { font-family: 'Segoe UI', Tahoma, sans-serif; background: #070e22; color: #fff; text-align: center; padding: 40px; }
        .card { background: #162344; border-radius: 12px; padding: 24px; max-width: 500px; margin: auto; }
    </style>
</head>
<body>
    <div class="card">
        <h2>Next Offline Chat AI</h2>
        <p>ইন্টারনেট ছাড়াই স্বয়ংক্রিয় অন-ডিভাইস বুদ্ধিমত্তা।</p>
    </div>
</body>
</html>
```
            """.trimIndent()

            else -> null
        }
    }
}
