package com.example.quickwork

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator

class ScanActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val integrator = IntentIntegrator(this)
        integrator.setPrompt("Scan QR Code")
        integrator.setBeepEnabled(true)
        integrator.setOrientationLocked(true)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            val intent = Intent()
            intent.putExtra("qr_result", result.contents)
            setResult(Activity.RESULT_OK, intent)
            finish()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
