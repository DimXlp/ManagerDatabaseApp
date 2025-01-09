package com.dimxlp.managerdb;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private Button getStartedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Subscribe to app_updates topic
        FirebaseMessaging.getInstance().subscribeToTopic("app_updates")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FCM", "Subscribed to app_updates topic");
                    }
                });

        // Check for a stored message in SharedPreferences
        SharedPreferences preferences = getSharedPreferences("AppMessages", MODE_PRIVATE);
        String title = preferences.getString("title", null);
        String message = preferences.getString("message", null);

        if (title != null && message != null) {
            showUpdateDialog(title, message);

            // Clear the message after displaying it
            preferences.edit().clear().apply();
        }

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> {});

        // Load Banner Ad
        AdView mainBanner1 = findViewById(R.id.main_banner_1);
        AdRequest adBannerRequest1 = new AdRequest.Builder().build();
        mainBanner1.loadAd(adBannerRequest1);

        AdView mainBanner2 = findViewById(R.id.main_banner_2);
        AdRequest adBannerRequest2 = new AdRequest.Builder().build();
        mainBanner2.loadAd(adBannerRequest2);

        getStartedButton = findViewById(R.id.get_started_button);
        getStartedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();

            }
        });
    }

    private void showUpdateDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
}