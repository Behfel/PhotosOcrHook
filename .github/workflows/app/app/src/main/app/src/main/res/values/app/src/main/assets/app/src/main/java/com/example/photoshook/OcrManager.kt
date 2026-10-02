package com.example.photoshook

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object OcrManager {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    fun processBitmap(context: Context, bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val mainHandler = Handler(Looper.getMainLooper())

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text = visionText.text.trim()
                mainHandler.post {
                    if (text.isNotEmpty()) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Photos OCR", text))
                        Toast.makeText(context, "Copied text to clipboard!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No text detected", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                mainHandler.post {
                    Toast.makeText(context, "OCR failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}