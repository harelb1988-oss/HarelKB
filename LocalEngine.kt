package com.harelkb.keyboard

class LocalEngine {
    private val words = mapOf(
        "שלום" to listOf("שלום", "שלומך", "שלכם"),
        "היי" to listOf("היי", "היי לך", "היי, מה נשמע?"),
        "תודה" to listOf("תודה", "תודה רבה", "תודה על העזרה"),
        "בוקר" to listOf("בוקר טוב", "בוקר אור", "בוקר נפלא"),
        "hello" to listOf("hello", "hello there", "how are you?"),
        "thanks" to listOf("thanks", "thank you", "thanks a lot")
    )

    fun getPredictions(context: String): List<String> {
        val token = context.trim().split(Regex("\\s+")).lastOrNull().orEmpty()
        if (token.isBlank()) return listOf("שלום", "תודה", "בוקר טוב")
        return words.entries.firstOrNull { it.key.startsWith(token, true) }?.value
            ?: listOf(token, "$tokenים", "$tokenית")
    }

    fun translate(text: String, target: String): String {
        val dictionary = if (target.lowercase().startsWith("en")) mapOf(
            "שלום" to "Hello", "תודה" to "Thank you", "בוקר טוב" to "Good morning",
            "להתראות" to "Goodbye", "מה נשמע" to "How are you?"
        ) else mapOf(
            "hello" to "שלום", "thank you" to "תודה", "good morning" to "בוקר טוב",
            "goodbye" to "להתראות", "how are you?" to "מה נשמע?"
        )
        return dictionary[text.trim().lowercase()] ?: text
    }

    fun rewrite(text: String, tone: String): String = when (tone.lowercase()) {
        "formal", "רשמי" -> if (text.isBlank()) text else "שלום,\n$text\nבברכה"
        "friendly", "ידידותי" -> if (text.isBlank()) text else "$text 🙂"
        "short", "קצר" -> text.trim().take(120)
        else -> text
    }

    fun askAI(query: String): String =
        "מצב מקומי פעיל. אין חיבור לספק AI כרגע.\n\nהשאלה שלך: $query\n\nאפשר להגדיר ספק AI בהגדרות הפרויקט."

    fun completeSentence(text: String): String {
        if (text.isBlank()) return "שלום, מה שלומך?"
        return if (text.endsWith(".") || text.endsWith("!") || text.endsWith("?")) text
        else "$text."
    }
}
