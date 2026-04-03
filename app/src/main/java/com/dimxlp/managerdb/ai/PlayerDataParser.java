package com.dimxlp.managerdb.ai;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.dimxlp.managerdb.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Helper class to parse player information from voice input using Google Gemini AI
 * Uses direct REST API calls for better compatibility
 */
public class PlayerDataParser {
    private static final String TAG = "PlayerDataParser";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
    private final Context context;
    private final Executor executor;
    private final Handler mainHandler;

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
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Parse player information from voice transcript using Gemini AI REST API
     */
    public void parsePlayerData(String transcript, PlayerDataCallback callback) {
        executor.execute(() -> {
            try {
                String prompt = buildPrompt(transcript);
                String response = callGeminiAPI(prompt);
                PlayerData playerData = parseJsonResponse(response);
                
                mainHandler.post(() -> callback.onSuccess(playerData));
            } catch (Exception e) {
                Log.e(TAG, "Error calling Gemini API", e);
                mainHandler.post(() -> callback.onError("AI processing failed: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Call Gemini REST API directly
     */
    private String callGeminiAPI(String prompt) throws Exception {
        URL url = new URL(GEMINI_API_URL + "?key=" + BuildConfig.GEMINI_API_KEY);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            
            // Build request body
            JSONObject requestBody = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", prompt);
            parts.put(part);
            content.put("parts", parts);
            contents.put(content);
            requestBody.put("contents", contents);
            
            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            // Read response
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), "utf-8"));
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorResponse.append(line);
                }
                throw new Exception("API Error " + responseCode + ": " + errorResponse.toString());
            }
            
            BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "utf-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
            
            // Extract text from response
            JSONObject responseJson = new JSONObject(response.toString());
            JSONArray candidates = responseJson.getJSONArray("candidates");
            JSONObject candidate = candidates.getJSONObject(0);
            JSONObject contentObj = candidate.getJSONObject("content");
            JSONArray partsArray = contentObj.getJSONArray("parts");
            JSONObject partObj = partsArray.getJSONObject(0);
            String text = partObj.getString("text");
            
            Log.d(TAG, "Gemini API response: " + text);
            return text;
            
        } finally {
            conn.disconnect();
        }
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

