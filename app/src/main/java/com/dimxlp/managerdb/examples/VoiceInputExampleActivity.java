package com.dimxlp.managerdb.examples;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dimxlp.managerdb.ai.PlayerDataParser;
import com.dimxlp.managerdb.voice.VoicePlayerDataManager;

/**
 * Example Activity showing how to integrate Voice-Powered Player Data Entry
 * 
 * This is a reference implementation. Copy the integration pattern to your actual
 * player creation activity.
 */
public class VoiceInputExampleActivity extends AppCompatActivity {

    // Voice manager
    private VoicePlayerDataManager voiceManager;

    // UI Components
    private Button microphoneButton;
    private ProgressBar progressBar;

    // Form fields
    private EditText nameEditText;
    private EditText overallEditText;
    private EditText potentialEditText;
    private EditText positionEditText;
    private EditText ageEditText;
    private EditText nationalityEditText;
    private EditText valueEditText;
    private EditText wageEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize voice manager
        voiceManager = new VoicePlayerDataManager(this);
        
        // Setup UI
        setupViews();
        setupMicrophoneButton();
    }

    private void setupViews() {
        // Initialize your views here
        // microphoneButton = findViewById(R.id.microphoneButton);
        // progressBar = findViewById(R.id.progressBar);
        // nameEditText = findViewById(R.id.nameEditText);
        // etc...
    }

    private void setupMicrophoneButton() {
        if (microphoneButton != null) {
            microphoneButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startVoiceInput();
                }
            });
        }
    }

    /**
     * Start the voice input process
     */
    private void startVoiceInput() {
        voiceManager.startVoiceInput(new VoicePlayerDataManager.PlayerDataResultCallback() {
            @Override
            public void onPlayerDataParsed(PlayerDataParser.PlayerData playerData) {
                // Fill form fields with parsed data
                fillFormWithPlayerData(playerData);
                
                // Show success message
                Toast.makeText(VoiceInputExampleActivity.this,
                        "Player data loaded! Please review and confirm.",
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String error) {
                // Show error message
                Toast.makeText(VoiceInputExampleActivity.this,
                        "Error: " + error,
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onListeningStarted() {
                // Update UI to show listening state
                if (microphoneButton != null) {
                    microphoneButton.setText("🎤 Listening...");
                    microphoneButton.setEnabled(false);
                }
                if (progressBar != null) {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onListeningEnded() {
                // Reset UI
                if (microphoneButton != null) {
                    microphoneButton.setText("🎤 Voice Input");
                    microphoneButton.setEnabled(true);
                }
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    /**
     * Fill form fields with parsed player data
     */
    private void fillFormWithPlayerData(PlayerDataParser.PlayerData playerData) {
        // Only fill non-empty fields
        if (nameEditText != null && !playerData.name.isEmpty()) {
            nameEditText.setText(playerData.name);
        }
        
        if (overallEditText != null && !playerData.overall.isEmpty()) {
            overallEditText.setText(playerData.overall);
        }
        
        if (potentialEditText != null && !playerData.potential.isEmpty()) {
            potentialEditText.setText(playerData.potential);
        }
        
        if (positionEditText != null && !playerData.position.isEmpty()) {
            positionEditText.setText(playerData.position);
        }
        
        if (ageEditText != null && !playerData.age.isEmpty()) {
            ageEditText.setText(playerData.age);
        }
        
        if (nationalityEditText != null && !playerData.nationality.isEmpty()) {
            nationalityEditText.setText(playerData.nationality);
        }
        
        if (valueEditText != null && !playerData.value.isEmpty()) {
            valueEditText.setText(playerData.value);
        }
        
        if (wageEditText != null && !playerData.wage.isEmpty()) {
            wageEditText.setText(playerData.wage);
        }
    }

    /**
     * Handle permission results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        voiceManager.onPermissionResult(requestCode, permissions, grantResults);
    }

    /**
     * Clean up resources
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceManager != null) {
            voiceManager.cleanup();
        }
    }
}

