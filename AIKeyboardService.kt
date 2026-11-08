package com.harelkb.keyboard

import android.Manifest
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputConnection
import android.inputmethodservice.InputMethodService
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class AIKeyboardService : InputMethodService() {
    private val scope = MainScope()
    private val local = LocalEngine()
    private lateinit var ai: MetaAIEngine
    private var privateMode = true
    private var english = false
    private var root: LinearLayout? = null
    private var predictionBar: LinearLayout? = null
    private val darkKey = Color.rgb(43, 43, 43)
    private val blue = Color.rgb(38, 116, 210)

    override fun onCreate() {
        super.onCreate()
        ai = MetaAIEngine()
        privateMode = getSharedPreferences("harelkb", MODE_PRIVATE).getBoolean("private", true)
    }

    override fun onCreateInputView(): View {
        val screenHeight = resources.displayMetrics.heightPixels
        val desiredHeight = (screenHeight * 0.40f).toInt().coerceAtLeast(dp(220))
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(5), dp(4), dp(5))
            setBackgroundColor(Color.rgb(24, 24, 24))
            layoutParams = LinearLayout.LayoutParams(-1, desiredHeight)
        }
        buildToolbar(root!!)
        predictionBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        root!!.addView(predictionBar, LinearLayout.LayoutParams(-1, dp(42)))
        refreshPredictions()
        val rows = if (english) listOf("qwertyuiop", "asdfghjkl", "zxcvbnm") else
            listOf("פםוזטארקי", "ףרלחועכגדש", "ץתצמנהבסז")
        rows.forEachIndexed { index, row -> addKeyRow(root!!, row.toList().map { it.toString() }, index) }
        addBottomRow(root!!)
        return root!!
    }

    private fun buildToolbar(parent: LinearLayout) {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        bar.addView(toolButton("✨ Ask Meta AI") { showAskSheet() }, weighted())
        bar.addView(toolButton(if (privateMode) "🔒 פרטי" else "🧠 Hybrid") {
            privateMode = !privateMode
            getSharedPreferences("harelkb", MODE_PRIVATE).edit().putBoolean("private", privateMode).apply()
            recreateKeyboard()
        }, weighted())
        bar.addView(toolButton("⌘ קיצור") { showCommandSheet() }, weighted())
        bar.addView(toolButton("Clipboard") { pasteClipboard() }, weighted())
        bar.addView(toolButton("⚙") {
            startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }, weighted())
        parent.addView(bar, LinearLayout.LayoutParams(-1, dp(42)))
    }

    private fun addKeyRow(parent: LinearLayout, keys: List<String>, row: Int) {
        val line = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        keys.forEach { key ->
            val button = keyButton(key) { commit(key) }
            val lp = LinearLayout.LayoutParams(0, dp(43), 1f).apply {
                setMargins(dp(2), dp(3), dp(2), dp(3))
            }
            line.addView(button, lp)
        }
        parent.addView(line, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    private fun addBottomRow(parent: LinearLayout) {
        val line = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        line.addView(keyButton("123") { commit("123") }, weighted(0.8f))
        line.addView(keyButton("🎙") { Toast.makeText(this, "הקלדה קולית תלויה בשירות Android נפרד", Toast.LENGTH_SHORT).show() }, weighted(0.8f))
        line.addView(keyButton(if (english) "English" else "עברית") {
            english = !english
            recreateKeyboard()
        }, weighted(2.8f))
        line.addView(keyButton("😊") { commit("😊") }, weighted(0.8f))
        line.addView(keyButton("↵") { currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER)) }, weighted(0.8f))
        parent.addView(line, LinearLayout.LayoutParams(-1, dp(48)))
        val actions = LinearLayout(this).apply { gravity = Gravity.CENTER }
        actions.addView(toolButton("⌫") { currentInputConnection?.deleteSurroundingText(1, 0) }, weighted())
        actions.addView(toolButton("רווח") { commit(" ") }, weighted(2f))
        actions.addView(toolButton("⌫ מחיקה") { currentInputConnection?.deleteSurroundingText(1, 0) }, weighted())
        parent.addView(actions, LinearLayout.LayoutParams(-1, dp(34)))
    }

    private fun refreshPredictions() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        val suggestions = local.getPredictions(before)
        // Remote predictions are intentionally requested asynchronously only in Hybrid mode.
        if (!privateMode) {
            scope.launch {
                val remote = ai.getPredictions(before)
                if (remote.isNotEmpty()) {
                    predictionBar?.removeAllViews()
                    remote.take(3).forEachIndexed { i, word ->
                        val tv = TextView(this@AIKeyboardService).apply {
                            text = word
                            gravity = Gravity.CENTER
                            setTextColor(Color.WHITE)
                            textSize = 14f
                            if (i == 1) {
                                setBackground(round(blue, 8))
                                setTypeface(null, Typeface.BOLD)
                            }
                            setOnClickListener { commit(word + " ") }
                        }
                        predictionBar?.addView(tv, weighted())
                    }
                }
            }
        }
        predictionBar?.removeAllViews()
        suggestions.take(3).forEachIndexed { i, word ->
            val tv = TextView(this).apply {
                text = word
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 14f
                if (i == 1) {
                    setBackground(round(blue, 8))
                    setTypeface(null, Typeface.BOLD)
                }
                setOnClickListener { commit(word + " ") }
            }
            predictionBar?.addView(tv, weighted())
        }
    }

    private fun commit(value: String) {
        if (value == "123") {
            Toast.makeText(this, "סימני מספרים – ניתן להרחיב בשלב הבא", Toast.LENGTH_SHORT).show()
        } else {
            currentInputConnection?.commitText(value, 1)
            refreshPredictions()
        }
    }

    fun handleCommand(text: String) {
        val parts = text.trim().split(Regex("\\s+"), limit = 2)
        val command = parts.firstOrNull().orEmpty()
        val payload = parts.getOrNull(1).orEmpty()
        when {
            command.startsWith("wa:") -> {
                val name = command.removePrefix("wa:").ifBlank { payload.substringBefore(" ") }
                val message = if (command.removePrefix("wa:").isNotBlank()) payload else payload.substringAfter(" ", "")
                val phone = if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                    == PackageManager.PERMISSION_GRANTED) ContactResolver(this).findPhone(name) else null
                if (phone == null) {
                    Toast.makeText(this, "לא נמצא איש קשר או שאין הרשאת אנשי קשר", Toast.LENGTH_LONG).show()
                    return
                }
                val digits = phone.filter { it.isDigit() || it == '+' }
                val url = "https://wa.me/${digits.replace("+", "")}" +
                    if (message.isNotBlank()) "?text=${Uri.encode(message)}" else ""
                launchExternal(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            command.startsWith("ts:") -> {
                if (packageIntent("com.microsoft.teams") != null) launchPackage("com.microsoft.teams")
                else launchPackage("com.microsoft.teams2")
            }
            command.startsWith("em:") -> launchPackage("com.microsoft.office.outlook")
            command.startsWith("od:") -> launchPackage("com.microsoft.skydrive")
            command.startsWith("cal:") -> {
                val intent = packageIntent("com.microsoft.office.outlook")
                if (intent != null) launchExternal(intent) else openAppSettings()
            }
            else -> Toast.makeText(this, "קיצור לא מוכר", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchPackage(pkg: String) {
        val intent = packageIntent(pkg)
        if (intent != null) launchExternal(intent)
        else Toast.makeText(this, "האפליקציה אינה מותקנת", Toast.LENGTH_SHORT).show()
    }

    private fun packageIntent(pkg: String): Intent? =
        packageManager.getLaunchIntentForPackage(pkg)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun launchExternal(intent: Intent) {
        try { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(intent) }
        catch (_: Exception) { Toast.makeText(this, "לא ניתן לפתוח את היעד", Toast.LENGTH_SHORT).show() }
    }

    private fun openAppSettings() {
        launchExternal(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:com.microsoft.office.outlook")))
    }

    private fun pasteClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val value = clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
        if (!value.isNullOrBlank()) commit(value)
    }


    private fun showCommandSheet() {
        val dialog = Dialog(this)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(20))
            setBackground(round(Color.rgb(35, 35, 35), 18))
        }
        panel.addView(TextView(this).apply {
            text = "הפעל קיצור"
            textSize = 19f
            setTextColor(Color.WHITE)
        })
        panel.addView(TextView(this).apply {
            text = "דוגמאות: wa:מרינה היי | ts: | em: | od: | cal:"
            textSize = 13f
            setTextColor(Color.LTGRAY)
        })
        val command = EditText(this).apply {
            hint = "הקלד קיצור ופרטים"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
        }
        panel.addView(command, LinearLayout.LayoutParams(-1, dp(54)))
        val buttons = LinearLayout(this)
        buttons.addView(toolButton("הפעל") {
            val value = command.text.toString()
            dialog.dismiss()
            handleCommand(value)
        }, weighted())
        buttons.addView(toolButton("ביטול") { dialog.dismiss() }, weighted())
        panel.addView(buttons)
        dialog.setContentView(panel)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(-1, WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
        }
        dialog.show()
        dialog.window?.setLayout(-1, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    private fun showAskSheet() {
        val dialog = Dialog(this)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(20))
            setBackground(round(Color.rgb(35, 35, 35), 18))
        }
        panel.addView(TextView(this).apply {
            text = "✨ Ask Meta AI"
            textSize = 20f
            setTextColor(Color.WHITE)
        })
        val query = EditText(this).apply {
            hint = "כתוב שאלה..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
        }
        panel.addView(query, LinearLayout.LayoutParams(-1, dp(54)))
        val answer = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 15f
            setPadding(0, dp(12), 0, dp(12))
        }
        panel.addView(answer)
        val buttons = LinearLayout(this)
        buttons.addView(toolButton("שאל") {
            val q = query.text.toString()
            scope.launch {
                answer.text = if (privateMode) local.askAI(q) else ai.askAI(q)
            }
        }, weighted())
        buttons.addView(toolButton("Insert") {
            if (answer.text.isNotBlank()) currentInputConnection?.commitText(answer.text, 1)
            dialog.dismiss()
        }, weighted())
        panel.addView(buttons)
        dialog.setContentView(panel)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(-1, WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
        }
        dialog.show()
        dialog.window?.setLayout(-1, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    private fun toolButton(label: String, action: () -> Unit) = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER
        textSize = 11f
        setTextColor(Color.WHITE)
        setPadding(dp(3), dp(7), dp(3), dp(7))
        setOnClickListener { action() }
    }

    private fun keyButton(label: String, action: () -> Unit) = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER
        textSize = 17f
        setTextColor(Color.WHITE)
        background = round(darkKey, 8)
        setOnClickListener { action() }
    }

    private fun round(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun weighted(weight: Float = 1f) =
        LinearLayout.LayoutParams(0, -1, weight)

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun recreateKeyboard() {
        setInputView(onCreateInputView())
    }

    override fun onFinishInputView(finishingInput: Boolean) { super.onFinishInputView(finishingInput) }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
