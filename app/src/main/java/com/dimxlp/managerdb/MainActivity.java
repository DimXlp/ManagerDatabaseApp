package com.dimxlp.managerdb;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
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

        // Enable Crashlytics collection
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.setCrashlyticsCollectionEnabled(true);

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
        
        // Check and show feedback popup if needed
        checkAndShowFeedbackPopup();
    }

    private void checkAndShowFeedbackPopup() {
        // SharedPreferences to store the last shown timestamp
        SharedPreferences sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
        long lastShown = sharedPreferences.getLong("FeedbackPopupLastShown", 0L);
        long currentTime = System.currentTimeMillis();

        // Check if a week (7 days) has passed
        if (currentTime - lastShown >= 7 * 24 * 60 * 60 * 1000) {
            // Show the popup
            showFeedbackPopup();

            // Update the last shown time
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong("FeedbackPopupLastShown", currentTime);
            editor.apply();
        }
    }

    private void showFeedbackPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Got Feedback?");
        builder.setMessage("Help us improve ManagerDB! Email us at managerdb@gmail.com.");
        builder.setPositiveButton("Send Feedback", (dialog, which) -> {
            // Open email app
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:managerdb@gmail.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback for ManagerDB");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "No email apps installed", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Dismiss", (dialog, which) -> dialog.dismiss());

        // Show the dialog
        builder.create().show();
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