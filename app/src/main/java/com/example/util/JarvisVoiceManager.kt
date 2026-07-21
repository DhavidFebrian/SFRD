package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class JarvisVoiceManager(
    private val context: Context,
    private val onOptionRecognized: (String) -> Unit,
    private val onStateChanged: (isListening: Boolean, text: String?) -> Unit
) {
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsReady = false
    private val handler = Handler(Looper.getMainLooper())
    private var stopListeningRunnable: Runnable? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setPitch(0.85f) // Cool Jarvis-like pitch
                    tts?.setSpeechRate(1.0f)
                    isTtsReady = true
                }
            }
        }

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    onStateChanged(true, "Mendengarkan suara Anda...")
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    Log.d("JarvisVoiceManager", "Speech recognition error code: $error")
                    onStateChanged(false, null)
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    processSpokenText(matches)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    processSpokenText(matches)
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    fun startJarvisCommandFlow() {
        startListening()
    }

    fun startListening() {
        if (speechRecognizer == null) {
            onStateChanged(false, "Speech recognizer tidak tersedia")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }

        try {
            speechRecognizer?.startListening(intent)
            onStateChanged(true, "Mendengarkan (5s)...")

            stopListeningRunnable?.let { handler.removeCallbacks(it) }
            stopListeningRunnable = Runnable {
                stopListening()
            }
            handler.postDelayed(stopListeningRunnable!!, 5000)
        } catch (e: Exception) {
            Log.e("JarvisVoiceManager", "Error starting listening", e)
            onStateChanged(false, null)
        }
    }

    fun stopListening() {
        stopListeningRunnable?.let { handler.removeCallbacks(it) }
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("JarvisVoiceManager", "Error stopping listening", e)
        }
        onStateChanged(false, null)
    }

    private fun processSpokenText(matches: ArrayList<String>?) {
        if (matches.isNullOrEmpty()) return
        
        for (text in matches) {
            val lower = text.lowercase()
            Log.d("JarvisVoiceManager", "Recognized text: $lower")
            
            if (lower.contains("hot") || lower.contains("property")) {
                onOptionRecognized("HOT PROPERTY")
                onStateChanged(false, "Terpilih: HOT PROPERTY")
                stopListeningRunnable?.let { handler.removeCallbacks(it) }
                stopListening()
                return
            } else if (lower.contains("ig") || lower.contains("instagram")) {
                onOptionRecognized("IG")
                onStateChanged(false, "Terpilih: Instagram")
                stopListeningRunnable?.let { handler.removeCallbacks(it) }
                stopListening()
                return
            } else if (lower.contains("foto") || lower.contains("ulang")) {
                onOptionRecognized("FOTO ULANG")
                onStateChanged(false, "Terpilih: Foto Ulang")
                stopListeningRunnable?.let { handler.removeCallbacks(it) }
                stopListening()
                return
            }
        }
    }

    fun destroy() {
        stopListeningRunnable?.let { handler.removeCallbacks(it) }
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) { e.printStackTrace() }
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) { e.printStackTrace() }
    }
}
