package com.example.panoramakotlin

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.TextView

class Lenses: Activity() {

    var currentSelectedLens: View? = null
    var selectedColor = Color.rgb(217, 217, 217)
    var selectedLensId: String? = null
    var selectedLensName: String? = null
    var prefLensKey = "LensSelected"
    var prefLensNameKey = "LensSelectedName"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_lenses)

        val lens = findViewById<View>(R.id.none) as RelativeLayout
        lens.setOnClickListener { v ->
            selectLens(v) }

        val txtCancel = findViewById<View>(R.id.cancel) as TextView
        txtCancel.setOnClickListener { finish() }

        val txtDone = findViewById<View>(R.id.done) as TextView
        txtDone.setOnClickListener {
            val pref = applicationContext.getSharedPreferences(
                "MyPref",
                MODE_PRIVATE
            )
            val editor = pref.edit()
            editor.putString(prefLensKey, selectedLensId)
            editor.putString(prefLensNameKey, selectedLensName)
            editor.commit()
            val data = Intent()
            data.putExtra("lensId", selectedLensId)
            data.putExtra("lensName", selectedLensName)
            setResult(RESULT_OK, data)
            finish()
        }

        selectedLensId = intent.getStringExtra("CurrentLens")
        val resID = resources.getIdentifier(selectedLensId, "id", packageName)
        val lensRow = findViewById<View>(resID)
        selectLens(lensRow)
    }
    fun selectLens(selectedLens: View) {
        if (currentSelectedLens != null) currentSelectedLens!!.setBackgroundColor(Color.TRANSPARENT)
        selectedLens.setBackgroundColor(selectedColor)
        currentSelectedLens = selectedLens
        selectedLensId = resources.getResourceEntryName(selectedLens.id)
        selectedLensName = selectedLens.tag.toString()
    }
}