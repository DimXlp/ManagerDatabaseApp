package com.dimxlp.managerdb.ai;

import android.content.Context;
import android.util.Log;

import com.dimxlp.managerdb.BuildConfig;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONObject;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Helper class to parse player information from voice input using Google Gemini AI
 */
public class PlayerDataParser {
    private static final String TAG = "PlayerDataParser";
    private final GenerativeModelFutures model;
    private final Executor executor;

    public interface PlayerDataCallback {
        void onSuccess(PlayerData playerData);
        void onError(String error);
    }

    public static class PlayerData {
        public String name;
        public String overall;
        public String potential;
        public String position;
        public String age;
        public String nationality;
        public String value;
        public String wage;

        public PlayerData() {
            // Initialize with empty strings to avoid null values
            name = "";
            overall = "";
            potential = "";
            position = "";
            age = "";
            nationality = "";
            value = "";
            wage = "";
        }

        @Override
        public String toString() {
            return "PlayerData{" +
                    "name='" + name + '\'' +
                    ", overall='" + overall + '\'' +
                    ", potential='" + potential + '\'' +
                    ", position='" + position + '\'' +
                    ", age='" + age + '\'' +
                    ", nationality='" + nationality + '\'' +
                    ", value='" + value + '\'' +
                    ", wage='" + wage + '\'' +
                    '}';
        }
    }

    public PlayerDataParser(Context context) {
        // Initialize Gemini AI model
        GenerativeModel gm = new GenerativeModel(
                "gemini-1.5-flash",
                BuildConfig.GEMINI_API_KEY
        );
        this.model = GenerativeModelFutures.from(gm);
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Parse player information from voice transcript using Gemini AI
     * @param transcript The voice input transcript
     * @param callback Callback to receive parsed player data or error
     */
    public void parsePlayerData(String transcript, PlayerDataCallback callback) {
        String prompt = buildPrompt(transcript);

        Content content = new Content.Builder()
                .addText(prompt)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    String responseText = result.getText();
                    Log.d(TAG, "Gemini response: " + responseText);
                    
                    // Parse the JSON response
                    PlayerData playerData = parseJsonResponse(responseText);
                    callback.onSuccess(playerData);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing Gemini response", e);
                    callback.onError("Failed to parse player data: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Gemini API error", t);
                callback.onError("AI processing failed: " + t.getMessage());
            }
        }, executor);
    }

    /**
     * Build the prompt for Gemini AI with context about expected fields
     */
    private String buildPrompt(String transcript) {
        return "You are a football/soccer player data extraction assistant. " +
                "Parse the following voice input and extract player information. " +
                "Return ONLY a valid JSON object with these exact fields: " +
                "name (string), overall (string with just the number), potential (string with just the number), " +
                "position (string - abbreviation like ST, CAM, CM, CB, GK, etc.), " +
                "age (string with just the number), nationality (string - full country name), " +
                "value (string with number and unit like '150M' or '5.5M'), " +
                "wage (string with number and unit like '500K' or '350K'). " +
                "\n\nIf any field is not mentioned, use an empty string. " +
                "Do not include any markdown formatting, explanations, or additional text - ONLY the JSON object. " +
                "\n\nVoice input: \"" + transcript + "\"" +
                "\n\nJSON:";
    }

    /**
     * Parse the JSON response from Gemini AI
     */
    private PlayerData parseJsonResponse(String responseText) throws Exception {
        // Clean up the response text - remove markdown code blocks if present
        String cleanJson = responseText.trim();
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.substring(7);
        }
        if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.substring(3);
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
        }
        cleanJson = cleanJson.trim();

        // Parse JSON
        JSONObject json = new JSONObject(cleanJson);
        
        PlayerData playerData = new PlayerData();
        playerData.name = json.optString("name", "");
        playerData.overall = json.optString("overall", "");
        playerData.potential = json.optString("potential", "");
        playerData.position = json.optString("position", "");
        playerData.age = json.optString("age", "");
        playerData.nationality = json.optString("nationality", "");
        playerData.value = json.optString("value", "");
        playerData.wage = json.optString("wage", "");

        return playerData;
    }
}

