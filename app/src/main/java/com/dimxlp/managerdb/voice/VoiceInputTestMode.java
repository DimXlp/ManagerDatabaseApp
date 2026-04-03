package com.dimxlp.managerdb.voice;

import android.app.Activity;
import android.util.Log;

import com.dimxlp.managerdb.ai.PlayerDataParser;

/**
 * Voice input test mode - bypasses actual voice recognition for testing AI parsing
 * Use this when emulator audio is not working (common issue with Android emulators)
 * 
 * IMPORTANT: Android emulators often have microphone issues due to audio hardware limitations.
 * This test mode allows you to test the AI parsing functionality without voice recognition.
 * For real voice input, use a physical Android device.
 */
public class VoiceInputTestMode {
    private static final String TAG = "VoiceInputTestMode";
    
    /**
     * Test samples for different scenarios - covering various input formats
     */
    public static final String[] TEST_SAMPLES = {
            // Format 1: Full details with all fields
            "Kylian Mbappe, 91 overall, 93 potential, striker, 27 years old, France, 150 million value, 500k wage",
            
            // Format 2: Abbreviated style
            "Erling Haaland, 91 overall, 95 potential, striker, Norway, 24 years old, 180M, 400K",
            
            // Format 3: Different order
            "Vinicius Junior, winger, Brazil, 89 OVR, 95 POT, 160 million",
            
            // Format 4: Natural speech style
            "Jude Bellingham, central midfielder, England, 87 overall, 94 potential, 22, 120M, 250K",
            
            // Format 5: More casual
            "Harry Kane from England, striker, 90 OVR, worth 90 million, wage 350 thousand",
            
            // Format 6: Minimal info
            "Luka Modric, 88 overall, 88 potential, central midfielder, 39, Croatia",
            
            // Format 7: With "years" variations
            "Pedri, 85 OVR, 94 POT, CM, 22 years, Spain, 100M value, 200K per week",
            
            // Format 8: Goalkeeper example
            "Thibaut Courtois, goalkeeper, Belgium, 89 overall, 89 potential, 32, 50M, 200K"
    };
    
    /**
     * Sample descriptions for UI display
     */
    public static final String[] SAMPLE_DESCRIPTIONS = {
            "Complete format: Mbappe",
            "Abbreviated: Haaland",
            "Different order: Vinicius",
            "Natural speech: Bellingham",
            "Casual style: Kane",
            "Minimal info: Modric",
            "With variations: Pedri",
            "Goalkeeper: Courtois"
    };
    
    /**
     * Test AI parsing with a sample transcript (no voice recognition)
     */
    public static void testWithSample(Activity activity, int sampleIndex, PlayerDataParser.PlayerDataCallback callback) {
        if (sampleIndex < 0 || sampleIndex >= TEST_SAMPLES.length) {
            sampleIndex = 0;
        }
        
        String transcript = TEST_SAMPLES[sampleIndex];
        Log.d(TAG, "Testing with sample #" + sampleIndex + ": " + transcript);
        
        PlayerDataParser parser = new PlayerDataParser(activity);
        parser.parsePlayerData(transcript, callback);
    }
    
    /**
     * Test with custom transcript
     */
    public static void testWithCustom(Activity activity, String transcript, PlayerDataParser.PlayerDataCallback callback) {
        Log.d(TAG, "Testing with custom: " + transcript);
        
        PlayerDataParser parser = new PlayerDataParser(activity);
        parser.parsePlayerData(transcript, callback);
    }
    
    /**
     * Get total number of test samples
     */
    public static int getSampleCount() {
        return TEST_SAMPLES.length;
    }
    
    /**
     * Get a random test sample index
     */
    public static int getRandomSampleIndex() {
        return (int) (Math.random() * TEST_SAMPLES.length);
    }
}


