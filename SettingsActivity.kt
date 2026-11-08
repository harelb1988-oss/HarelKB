package com.harelkb.keyboard

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 28)
            addView(TextView(this@SettingsActivity).apply {
                textSize = 20f
                text = "HarelKB – הגדרות"
            })
            addView(TextView(this@SettingsActivity).apply {
                textSize = 15f
                text = "\nמצב פרטי מונע בקשות רשת מתוך המקלדת.\n" +
                    "לשימוש ב-AI מרוחק יש להגדיר AI_ENDPOINT ו-AI_API_KEY ב-gradle.properties או ב-secrets של סביבת הבנייה.\n\n" +
                    "אין כאן חיבור מובנה ל-Meta AI הרשמי. יש להשתמש רק בספק ובמפתח שהשימוש בהם מורשה."
            })
        })
    }
}
