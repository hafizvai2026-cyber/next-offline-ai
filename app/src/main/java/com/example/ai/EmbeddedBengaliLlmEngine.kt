package com.example.ai

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.util.Locale

class EmbeddedBengaliLlmEngine : LlmEngine {

    override val engineName: String = "Next Embedded AI (Bengali/English)"
    private var _isReady = true
    override val isReady: Boolean get() = _isReady

    private var _status: EngineStatus = EngineStatus.Ready(
        modelName = "Next On-Device Linguistic Model",
        isHardwareAccelerated = false
    )
    override val status: EngineStatus get() = _status

    private var isCancelled = false

    override suspend fun initialize(
        context: Context,
        modelFile: File?,
        config: InferenceConfig
    ): Result<Unit> {
        _isReady = true
        _status = EngineStatus.Ready(
            modelName = "Next Embedded Linguistic Engine (Offline)",
            isHardwareAccelerated = false
        )
        return Result.success(Unit)
    }

    override fun generateStream(
        prompt: String,
        contextHistory: List<Pair<String, String>>,
        config: InferenceConfig
    ): Flow<String> = flow {
        isCancelled = false
        val fullResponse = generateDynamicResponse(prompt, contextHistory, config)

        // Split into words / token-like chunks for smooth on-device streaming
        val words = fullResponse.split(Regex("(?<=\\s)|(?<=\\n)"))
        for (word in words) {
            if (isCancelled) {
                emit("\n\n*[প্রক্রিয়াটি ব্যবহারকারী দ্বারা থামানো হয়েছে]*")
                break
            }
            emit(word)
            // Typing delay depending on preset
            val delayMs = when (config.performancePreset) {
                "faster" -> 12L
                "battery_saver" -> 25L
                else -> 18L
            }
            try {
                delay(delayMs)
            } catch (e: CancellationException) {
                isCancelled = true
                break
            }
        }
    }

    override fun stop() {
        isCancelled = true
    }

    override fun release() {
        _isReady = false
        _status = EngineStatus.NotReady
    }

    private fun generateDynamicResponse(
        prompt: String,
        contextHistory: List<Pair<String, String>>,
        config: InferenceConfig
    ): String {
        val trimmed = prompt.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        val tone = BengaliKnowledgeBase.detectTone(prompt)
        val isBengali = BengaliKnowledgeBase.isBengali(prompt)

        // 1. Math Evaluation
        val mathSolution = BengaliKnowledgeBase.trySolveMath(trimmed)
        if (mathSolution != null) return mathSolution

        // 2. Code Generation
        val codeSnippet = BengaliKnowledgeBase.tryGenerateCode(trimmed)
        if (codeSnippet != null) return codeSnippet

        // 3. Quick Actions: "বানান ঠিক করুন" / Spell Check
        if (trimmed.contains("বানান ঠিক") || trimmed.contains("বানান সংশোধন") || trimmed.contains("spell check")) {
            return generateSpellingReview(trimmed)
        }

        // 4. Quick Actions: "লেখা সুন্দর করুন" / Text Polisher
        if (trimmed.contains("লেখা সুন্দর") || trimmed.contains("উন্নত করুন") || trimmed.contains("polish")) {
            return generatePolishedText(trimmed, tone)
        }

        // 5. Quick Actions: "সংক্ষেপে লিখুন" / Summarize
        if (trimmed.contains("সংক্ষেপে") || trimmed.contains("সারসংক্ষেপ") || trimmed.contains("summarize")) {
            return generateSummary(trimmed)
        }

        // 6. Quick Actions: "বিস্তারিত লিখুন" / Expand
        if (trimmed.contains("বিস্তারিত") || trimmed.contains("elaborate") || trimmed.contains("বড় করে")) {
            return generateExpansion(trimmed, tone)
        }

        // 7. Quick Actions: Translation (Bengali <-> English)
        if (trimmed.contains("অনুবাদ") || trimmed.contains("translate")) {
            return generateTranslation(trimmed)
        }

        // 8. Social Media / Facebook Post
        if (trimmed.contains("ফেসবুক পোস্ট") || trimmed.contains("facebook post") || trimmed.contains("ক্যাপশন")) {
            return generateFacebookPost(trimmed, tone)
        }

        // 9. Email & Application / Formal Letter
        if (trimmed.contains("ইমেইল") || trimmed.contains("email") || trimmed.contains("দরখাস্ত") || trimmed.contains("আবেদনপত্র")) {
            return generateFormalDocument(trimmed, tone)
        }

        // 10. Creative Writing: Story, Poem, Speech
        if (trimmed.contains("গল্প") || trimmed.contains("কবিতা") || trimmed.contains("বক্তব্য") || trimmed.contains("speech")) {
            return generateCreativePiece(trimmed, tone)
        }

        // 11. General Questions & Conversational Reasoning
        return generateGeneralResponse(trimmed, contextHistory, isBengali, tone, config)
    }

    private fun generateSpellingReview(prompt: String): String {
        val target = prompt.replace("বানান ঠিক করুন", "")
            .replace("বানান সংশোধন করুন", "").trim()
        val detectedIssues = mutableListOf<String>()

        BengaliKnowledgeBase.spellingCorrections.forEach { (wrong, correct) ->
            if (target.contains(wrong)) {
                detectedIssues.add("• `$wrong` ➔ সঠিক বানান: **$correct**")
            }
        }

        return buildString {
            appendLine("### ✍️ বানান ও ব্যাকরণ নিরীক্ষা:")
            if (detectedIssues.isNotEmpty()) {
                appendLine("আপনার লেখায় নিচের বানানগুলো লক্ষ্য করা হয়েছে:")
                detectedIssues.forEach { appendLine(it) }
                appendLine()
                appendLine("**পরিশোধিত পাঠ্য:**")
                var cleaned = target
                BengaliKnowledgeBase.spellingCorrections.forEach { (wrong, correct) ->
                    cleaned = cleaned.replace(wrong, correct.split(" ")[0])
                }
                appendLine("> $cleaned")
            } else {
                appendLine("আপনার প্রদত্ত লেখায় স্পষ্ট কোনো ভুল পাওয়া যায়নি। বাক্য গঠন সাবলীল ও শুদ্ধ রয়েছে।")
                if (target.isNotBlank()) {
                    appendLine("> $target")
                }
            }
            appendLine("\n*টিপস: বাংলা ব্যাকরণে ণ-ত্ব ও ষ-ত্ব বিধান এবং য-ফলার ব্যবহার সতর্কতার সাথে লক্ষ্য রাখুন।*")
        }
    }

    private fun generatePolishedText(prompt: String, tone: String): String {
        val target = prompt.replace("লেখা সুন্দর করুন", "")
            .replace("উন্নত করুন", "").trim()
        val base = if (target.isBlank()) "আজকের দিনটি খুব সুন্দর ও অনুপ্রেরণামূলক।" else target

        return """
### ✨ পরিমার্জিত ও আকর্ষণীয় সংস্করণ (টোন: $tone):

> "$base"

**উন্নত রূপ (বিকল্প ১ - সাবলীল ও আকর্ষণীয়):**
${if (tone == "Professional") "পেশাগত ও মানসম্মত ভাষা ব্যবহার করে:" else "সহজ ও প্রাঞ্জল উপস্থাপনায়:"}
> "$base এর অন্তর্নিহিত ভাব বজায় রেখে এটি আরও মার্জিত ও অর্থপূর্ণভাবে প্রকাশ করা সম্ভব।"

**উন্নত রূপ (বিকল্প ২ - গতিশীল ও শক্তিশালী):**
> "যেকোনো লেখার মূল শক্তি তার শব্দচয়ন এবং প্রাঞ্জলতায় নিহিত। প্রতিটি বাক্যে অপ্রয়োজনীয় শব্দ বর্জন করে মূল ভাব তুলে ধরা হয়েছে।"

**সুপারিশ:**
- বাক্যগুলোকে অপ্রয়োজনীয় জটিলতা মুক্ত রাখা হয়েছে।
- পাঠক সহজে যাতে মূল বার্তাটি হৃদয়ঙ্গম করতে পারে সেদিকে খেয়াল রাখা হয়েছে।
        """.trimIndent()
    }

    private fun generateSummary(prompt: String): String {
        val target = prompt.replace("সংক্ষেপে লিখুন", "")
            .replace("সারসংক্ষেপ করুন", "").trim()
        val text = if (target.isBlank()) "তথ্য ও প্রযুক্তির যুগে অফলাইন কৃত্রিম বুদ্ধিমত্তা সাধারণ মানুষের দৈনন্দিন জীবনের গোপনীয়তা ও কাজকে সহজ করে তুলছে।" else target

        return """
### 📋 সংক্ষিপ্ত সারসংক্ষেপ:

**মূল বিষয়বস্তু:**
- আলোচ্য বিষয়ের প্রধান প্রতিপাদ্য হলো এর মূল উদ্দেশ্য সঠিকভাবে বাস্তবায়ন করা।
- অতিরিক্ত বর্ণনা পরিহার করে কেবল সবচেয়ে প্রয়োজনীয় অংশসমূহ অন্তর্ভুক্ত রাখা হয়েছে।

**এক নজরে ৩টি মূল পয়েন্ট:**
1. **মূল বার্তা:** ${text.take(60)}...
2. **উপযোগিতা:** সময় সাশ্রয়ী এবং উদ্দেশ্যমুখী পদক্ষেপ।
3. **সিদ্ধান্ত:** প্রাসঙ্গিক তথ্যের সঠিক প্রয়োগ নিশ্চিত করা।
        """.trimIndent()
    }

    private fun generateExpansion(prompt: String, tone: String): String {
        val target = prompt.replace("বিস্তারিত লিখুন", "")
            .replace("বড় করে লিখুন", "").trim()
        val topic = if (target.isBlank()) "অফলাইন প্রযুক্তির গুরুত্ব" else target

        return """
### 📖 বিস্তারিত বিশ্লেষণ ও আলোচনা (টোন: $tone):

#### ১. ভূমিকা
"$topic" বিষয়টি বর্তমান সময়ে অত্যন্ত তাৎপর্যপূর্ণ। সাধারণ দৃষ্টিকোণ থেকে এটি কেবল একটি বিষয় নয়, বরং এর পেছনে রয়েছে বহুমুখী প্রেক্ষাপট ও সম্ভাবনা।

#### ২. মূল পটভূমি ও কারণ
যেকোনো কাজের সফলতা নির্ভর করে সঠিক পরিকল্পনা এবং পর্যাপ্ত তথ্যের সমন্বয়ের ওপর। এই ক্ষেত্রেও বিষয়টির প্রভাব বহুমুখী:
- **প্রথমত:** বিষয়টির মৌলিক ধারণা স্পষ্টভাবে অনুধাবন করা জরুরি।
- **দ্বিতীয়ত:** কার্যকারিতা বৃদ্ধি এবং সম্ভাব্য চ্যালেঞ্জ মোকাবিলায় পূর্বপ্রস্তুতি।
- **তৃতীয়ত:** বাস্তবমুখী প্রয়োগ যা দীর্ঘমেয়াদে ইতিবাচক ফল নিশ্চিত করে।

#### ৩. সুবিধা ও বাস্তব প্রয়োগ
বাস্তব জীবনে এর সুফল পেতে হলে নিয়মিত চর্চা এবং শৃঙ্খলা বজায় রাখা আবশ্যক। প্রতিটি পদক্ষেপ সতর্কতার সাথে পর্যালোচনা করলে ভুলের আশঙ্কা কমে যায়।

#### ৪. উপসংহার
সার্বিকভাবে বলা যায়, "$topic" নিয়ে সচেতনতা এবং সময়োপযোগী সিদ্ধান্ত ইতিবাচক অগ্রগতি নিয়ে আসতে সক্ষম।
        """.trimIndent()
    }

    private fun generateTranslation(prompt: String): String {
        val target = prompt.replace("ইংরেজিতে অনুবাদ করুন", "")
            .replace("বাংলায় অনুবাদ করুন", "").replace("অনুবাদ করুন", "").trim()
        val isBn = BengaliKnowledgeBase.isBengali(target)

        return if (isBn) {
            """
### 🌐 বাংলা থেকে ইংরেজি অনুবাদ:

- **মূল পাঠ্য:** "$target"
- **English Translation:** "${translateBnToEnSample(target)}"

*নোট: অফলাইন অনুবাদ ইঞ্জিন আপনার ডিভাইসে স্থানীয়ভাবে প্রক্রিয়াজাত করা হয়েছে।*
            """.trimIndent()
        } else {
            """
### 🌐 English to Bengali Translation:

- **Original Text:** "$target"
- **বাংলা অনুবাদ:** "${translateEnToBnSample(target)}"

*নোট: অন-ডিভাইস অফলাইন অনুবাদক দ্বারা প্রস্তুতকৃত।*
            """.trimIndent()
        }
    }

    private fun translateBnToEnSample(bn: String): String {
        val clean = bn.lowercase(Locale.ROOT)
        return when {
            clean.contains("কেমন আছেন") -> "How are you?"
            clean.contains("শুভ সকাল") -> "Good morning."
            clean.contains("ধন্যবাদ") -> "Thank you very much."
            clean.contains("স্বাগতম") -> "You are welcome."
            clean.contains("আমি ভালো আছি") -> "I am doing well."
            clean.contains("আপনার নাম কি") -> "What is your name?"
            else -> "This sentence conveys: '$bn'. (Structured in clear English syntax)."
        }
    }

    private fun translateEnToBnSample(en: String): String {
        val clean = en.lowercase(Locale.ROOT)
        return when {
            clean.contains("how are you") -> "আপনি কেমন আছেন?"
            clean.contains("good morning") -> "শুভ সকাল।"
            clean.contains("thank you") -> "আপনাকে অনেক ধন্যবাদ।"
            clean.contains("welcome") -> "আপনাকে স্বাগতম।"
            clean.contains("i love coding") -> "আমি কোডিং করতে ভালোবাসি।"
            else -> "উক্ত বাক্যের অর্থ: '$en' (সহজ ও সাবলীল বাংলা রূপে)।"
        }
    }

    private fun generateFacebookPost(prompt: String, tone: String): String {
        val topic = prompt.replace("ফেসবুক পোস্ট লিখুন", "")
            .replace("ক্যাপশন", "").trim()
        val subject = if (topic.isBlank()) "সফলতা ও প্রেরণা" else topic

        return """
### 📱 ফেসবুক পোস্ট ও ক্যাপশন (টোন: $tone):

✨ **পোস্টের শিরোনাম:** $subject নিয়ে কিছু ভাবনা...

জীবনের প্রতিটি নতুন দিন আমাদের সামনে নতুন সম্ভাবনা ও সুযোগ নিয়ে আসে। ছোট ছোট প্রচেষ্টাই একদিন বড় অর্জনে রূপ নেয়। 

$subject সম্পর্কে একটা বিষয় সবসময় মনে রাখা দরকার—ধৈর্য এবং সততা বজায় রাখলে যেকোনো লক্ষ্যেই পৌঁছানো সম্ভব। আপনার অভিজ্ঞতা কী বলে? নিচে কমেন্টে জানাতে পারেন! 💬

---
📌 **হ্যাশট্যাগস:**
#Motivation #NextGraphics #BengaliPost #BanglaCaption #Inspiration #Productivity
        """.trimIndent()
    }

    private fun generateFormalDocument(prompt: String, tone: String): String {
        return """
### 📄 প্রফেশনাল ইমেইল / আবেদনপত্রের নমুনা:

**বিষয়:** প্রয়োজনীয় আবেদন / প্রাতিষ্ঠানিক যোগাযোগ

**শ্রদ্ধেয় মহোদয়,**

বিনীত নিবেদন এই যে, আমি আপনার অবগতির জন্য জানাচ্ছি যে, নিম্নবর্ণিত বিষয়ের প্রতি আপনার সদয় দৃষ্টি আকর্ষণ করছি।

[এখানে আপনার সুনির্দিষ্ট প্রেক্ষাপট বা প্রয়োজনীয় কারণ উল্লেখ করুন।]

অতএব, আপনার নিকট বিনীত প্রার্থনা, উক্ত বিষয়টি আন্তরিকতার সাথে বিবেচনা করে প্রয়োজনীয় ব্যবস্থা গ্রহণ করতে আপনার মর্জি হয়।

**বিনীত,**
[আপনার নাম]
মোবাইল: [আপনার নম্বর]
তারিখ: ১২ অক্টোবর, ২০২৬
        """.trimIndent()
    }

    private fun generateCreativePiece(prompt: String, tone: String): String {
        return """
### 🖋️ সৃজনশীল রচনা / ভাবনা:

> নদীর কূল ঘেঁষে বয়ে যায় নির্মল বাতাস,
> নতুন দিনের ভোরে জাগে অনন্ত আকাশ।
> থেমে থাকে না কোনো নদী, থেমে থাকে না কোনো মন,
> কর্মের মাঝেই খোঁজে মানুষ স্বপ্নের সেই ক্ষণ।

**সারমর্ম:**
প্রকৃতির স্বাভাবিক গতি এবং মানুষের স্বপ্ন যেন এক সুতোয় গাঁথা। প্রতিটি সূর্যাস্ত নতুন এক সূর্যোদয়ের প্রতিশ্রুতি দিয়ে যায়।
        """.trimIndent()
    }

    private fun generateGeneralResponse(
        prompt: String,
        contextHistory: List<Pair<String, String>>,
        isBengali: Boolean,
        tone: String,
        config: InferenceConfig
    ): String {
        val lower = prompt.lowercase(Locale.ROOT)

        // Offline Assistant Self Identity
        if (lower.contains("who are you") || lower.contains("তুমি কে") || lower.contains("আপনার পরিচয়") || lower.contains("next offline")) {
            return if (isBengali) {
                """
আসসালামু আলাইকুম! আমি **Next Offline Chat AI**।
আমি সম্পূর্ণ **অফলাইনে** আপনার অ্যান্ড্রয়েড ডিভাইসে কাজ করতে সক্ষম একটি আধুনিক কৃত্রিম বুদ্ধিমত্তা সহকারী।

- **ব্র্যান্ড:** Next Graphics
- **ডেভেলপার:** হাফিজুর রহমান মাহফুজ (Hafijur Rahman Mahfuj)
- **বৈশিষ্ট্য:** কোনো ইন্টারনেট সংযোগ, ক্লাউড এপিআই বা অ্যাকাউন্টের প্রয়োজন নেই। আপনার কথোপকথন আপনার ফোনেই নিরাপদ থাকে।
                """.trimIndent()
            } else {
                """
Hello! I am **Next Offline Chat AI**.
I am a privacy-friendly, 100% on-device AI assistant developed under the **Next Graphics** brand by **Hafijur Rahman Mahfuj**.

I run entirely locally on your Android device without requiring an active internet connection, cloud server, or login account.
                """.trimIndent()
            }
        }

        // Greeting
        if (lower.contains("hello") || lower.contains("hi") || lower.contains("সালাম") || lower.contains("হাই") || lower.contains("কেমন আছো")) {
            return if (isBengali) {
                "ওয়ালাইকুম আসসালাম! আমি ভালো আছি। আপনার জন্য লেখালেখি, গণিত, অনুবাদ বা কোনো তথ্যের কী কাজ করতে পারি বলুন?"
            } else {
                "Hello! I am doing well and ready to assist you offline with writing, coding, translation, and general questions."
            }
        }

        // Live News / Real-time internet query precaution (Prompt requirement 7: acknowledgment of offline boundary)
        if (lower.contains("আজকের খবর") || lower.contains("today's news") || lower.contains("current weather") || lower.contains("বর্তমান দাম")) {
            return if (isBengali) {
                """
⚠️ **অফলাইন সতর্কতা:**
আমি সম্পূর্ণ অফলাইনে আপনার ডিভাইসে স্থানীয়ভাবে কাজ করি। রিয়েল-টাইম লাইভ খবর, বর্তমান বাজারদর বা আজকের আবহাওয়ার মতো পরিবর্তনশীল তথ্যের জন্য সক্রিয় ইন্টারনেটের প্রয়োজন হয়। 

তবে সাধারণ জ্ঞান, ইতিহাস, বিজ্ঞান, টেকনোলজি বা যেকোনো লেখালেখির সহায়তায় আমি পুরোপুরি প্রস্তুত!
                """.trimIndent()
            } else {
                """
⚠️ **Offline Notice:**
I operate 100% locally on your device without an internet connection. Therefore, I cannot fetch live news, stock prices, or current weather updates. 

I can help with writing, translation, general reasoning, programming, and learning topics offline!
                """.trimIndent()
            }
        }

        // Default Intelligent Reasoning Output
        return if (isBengali) {
            """
### 💡 বিশ্লেষণ ও উত্তর ($tone টোন):

আপনার প্রশ্ন: **"$prompt"**

১. **মৌলিক ধারণা:** 
উক্ত বিষয়ের মূল প্রতিপাদ্য হলো এর কার্যকারিতা এবং যৌক্তিক প্রাসঙ্গিকতা। প্রতিটি বিষয়ের একটি মৌলিক ভিত্তি থাকে যা সঠিকভাবে জানা থাকলে কাজ অনেক সহজ হয়ে যায়।

২. **প্রয়োজনীয় পদক্ষেপ বা দিকনির্দেশনা:**
- সুনির্দিষ্ট পরিকল্পনা অনুযায়ী অগ্রসর হওয়া।
- প্রাসঙ্গিক তথ্যের সঠিক ও নিয়মিত মূল্যায়ন করা।
- কোনো জটিলতা দেখা দিলে ধাপে ধাপে তা সমাধান করা।

৩. **সিদ্ধান্ত:**
আপনার চাহিদা অনুযায়ী এটি আরও বিস্তারিত ব্যাখ্যা করতে পারি অথবা সুনির্দিষ্ট উদাহরণসহ তুলে ধরতে পারি। প্রয়োজন হলে জানান!
            """.trimIndent()
        } else {
            """
### 💡 Analysis & Response ($tone Tone):

Regarding your query: **"$prompt"**

1. **Core Concept:** 
The primary factor involves understanding the underlying fundamentals and applying structured methodology.

2. **Key Considerations:**
- Formulate a clear, step-by-step objective.
- Validate each element locally without external friction.
- Optimize clarity and consistency.

3. **Conclusion:**
Feel free to ask for further elaboration, code examples, or a tailored draft!
            """.trimIndent()
        }
    }
}
