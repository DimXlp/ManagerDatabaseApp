package com.dimxlp.managerdb.voice;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dimxlp.managerdb.ai.PlayerDataParser;

/**
 * Orchestrates voice recognition and AI parsing for player data entry
 */
public class VoicePlayerDataManager {
    private static final String TAG = "VoicePlayerDataManager";
    private static final int RECORD_AUDIO_PERMISSION_REQUEST_CODE = 1001;

    private final Activity activity;
    private final VoiceRecognitionHelper voiceRecognitionHelper;
    private final PlayerDataParser playerDataParser;
    private final Handler mainHandler;
    private PlayerDataResultCallback resultCallback;

    public interface PlayerDataResultCallback {
        void onPlayerDataParsed(PlayerDataParser.PlayerData playerData);
        void onError(String error);
        void onListeningStarted();
        void onListeningEnded();
    }

    public VoicePlayerDataManager(Activity activity) {
        this.activity = activity;
        this.voiceRecognitionHelper = new VoiceRecognitionHelper(activity);
        this.playerDataParser = new PlayerDataParser(activity);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Start the voice-to-data process
     * @param callback Callback to receive the parsed player data
     */
    public void startVoiceInput(PlayerDataResultCallback callback) {
        this.resultCallback = callback;

        // Check if speech recognition is available
        if (!VoiceRecognitionHelper.isAvailable(activity)) {
            notifyError("Voice recognition is not available on this device");
            return;
        }

        // Check for microphone permission
        if (!hasRecordAudioPermission()) {
            requestRecordAudioPermission();
            return;
        }

        // Initialize and start voice recognition
        initializeAndStartVoiceRecognition();
    }

    /**
     * Check if RECORD_AUDIO permission is granted
     */
    private boolean hasRecordAudioPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request RECORD_AUDIO permission
     */
    private void requestRecordAudioPermission() {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.RECORD_AUDIO},
                RECORD_AUDIO_PERMISSION_REQUEST_CODE
        );
    }

    /**
     * Handle permission request result - call this from Activity's onRequestPermissionsResult
     */
    public void onPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start voice recognition
                initializeAndStartVoiceRecognition();
            } else {
                notifyError("Microphone permission is required for voice input");
            }
        }
    }

    /**
     * Initialize voice recognition and start listening
     */
    private void initializeAndStartVoiceRecognition() {
        voiceRecognitionHelper.initialize(new VoiceRecognitionHelper.VoiceRecognitionCallback() {
            @Override
            public void onVoiceInputReceived(String transcript) {
                mainHandler.post(() -> {
                    if (resultCallback != null) {
                        resultCallback.onListeningEnded();
                    }
                    // Process transcript with Gemini AI
                    processTranscriptWithAI(transcript);
                });
            }

            @Override
            public void onVoiceInputError(String error) {
                mainHandler.post(() -> {
                    if (resultCallback != null) {
                        resultCallback.onListeningEnded();
                    }
                    notifyError(error);
                });
            }

            @Override
            public void onReadyForSpeech() {
                mainHandler.post(() -> {
                    if (resultCallback != null) {
                        resultCallback.onListeningStarted();
                    }
                });
            }

            @Override
            public void onSpeechStarted() {
                // Speech detection started
            }
        });

        voiceRecognitionHelper.startListening();
    }

    /**
     * Process the transcript using Gemini AI
     */
    private void processTranscriptWithAI(String transcript) {
        playerDataParser.parsePlayerData(transcript, new PlayerDataParser.PlayerDataCallback() {
            @Override
            public void onSuccess(PlayerDataParser.PlayerData playerData) {
                mainHandler.post(() -> {
                    if (resultCallback != null) {
                        resultCallback.onPlayerDataParsed(playerData);
                    }
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> notifyError(error));
            }
        });
    }

    /**
     * Notify error to callback
     */
    private void notifyError(String error) {
        if (resultCallback != null) {
            resultCallback.onError(error);
        }
    }

    /**
     * Clean up resources - call this from Activity's onDestroy
     */
    public void cleanup() {
        voiceRecognitionHelper.destroy();
    }

    /**
     * Get the permission request code for handling in Activity
     */
    public static int getPermissionRequestCode() {
        return RECORD_AUDIO_PERMISSION_REQUEST_CODE;
    }
}

