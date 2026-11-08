package com.harelkb.keyboard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class SetupActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }
        root.addView(TextView(this).apply {
            text = "HarelKB\n\nכדי להתחיל, הפעל את המקלדת בהגדרות Android. הרשאת אנשי קשר נדרשת רק לקיצורי wa:."
            textSize = 18f
        })
        root.addView(Button(this).apply {
            text = "1. הפעל HarelKB"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        })
        root.addView(Button(this).apply {
            text = "2. בחר HarelKB כמקלדת"
            setOnClickListener {
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showInputMethodPicker()
            }
        })
        root.addView(Button(this).apply {
            text = "3. אפשר גישה לאנשי קשר"
            setOnClickListener {
                if (ContextCompat.checkSelfPermission(this@SetupActivity, Manifest.permission.READ_CONTACTS)
                    != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this@SetupActivity, arrayOf(Manifest.permission.READ_CONTACTS), 41
                    )
                }
            }
        })
        root.addView(Button(this).apply {
            text = "הגדרות HarelKB"
            setOnClickListener { startActivity(Intent(this@SetupActivity, SettingsActivity::class.java)) }
        })
        setContentView(root)
    }
}
