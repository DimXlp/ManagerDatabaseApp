package com.dimxlp.managerdb.voice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Helper class to handle voice recognition for player data entry
 */
public class VoiceRecognitionHelper {
    private static final String TAG = "VoiceRecognitionHelper";
    private final Activity activity;
    private SpeechRecognizer speechRecognizer;
    private VoiceRecognitionCallback callback;

    public interface VoiceRecognitionCallback {
        void onVoiceInputReceived(String transcript);
        void onVoiceInputError(String error);
        void onReadyForSpeech();
        void onSpeechStarted();
    }

    public VoiceRecognitionHelper(Activity activity) {
        this.activity = activity;
    }

    /**
     * Check if speech recognition is available on the device
     */
    public static boolean isAvailable(Activity activity) {
        return SpeechRecognizer.isRecognitionAvailable(activity);
    }

    /**
     * Initialize the speech recognizer
     */
    public void initialize(VoiceRecognitionCallback callback) {
        this.callback = callback;
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "Ready for speech");
                if (callback != null) {
                    callback.onReadyForSpeech();
                }
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "Speech started");
                if (callback != null) {
                    callback.onSpeechStarted();
                }
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // Audio level changed
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // Buffer received
            }

            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "Speech ended");
            }

            @Override
            public void onError(int error) {
                String errorMessage = getErrorText(error);
                Log.e(TAG, "Speech recognition error: " + errorMessage);
                if (callback != null) {
                    callback.onVoiceInputError(errorMessage);
                }
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String transcript = matches.get(0);
                    Log.d(TAG, "Transcript: " + transcript);
                    if (callback != null) {
                        callback.onVoiceInputReceived(transcript);
                    }
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                // Partial results available
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // Reserved for future use
            }
        });
    }

    /**
     * Start listening for voice input
     */
    public void startListening() {
        if (speechRecognizer == null) {
            Log.e(TAG, "Speech recognizer not initialized");
            if (callback != null) {
                callback.onVoiceInputError("Voice recognition not initialized");
            }
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say player details: name, overall, potential, position, age, nationality, value, wage");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);

        speechRecognizer.startListening(intent);
    }

    /**
     * Stop listening for voice input
     */
    public void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    /**
     * Clean up resources
     */
    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    /**
     * Convert error codes to readable messages
     */
    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions. Please enable microphone access.";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No speech match. Please try again.";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognition service busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech input detected";
            default:
                return "Unknown error";
        }
    }
}

