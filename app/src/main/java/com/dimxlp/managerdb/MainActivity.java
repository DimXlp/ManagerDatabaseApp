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
import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.onesignal.OneSignal;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "RAFI|Main";
    private Button getStartedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_main);
        Log.i(LOG_TAG, "MainActivity launched.");

        // Subscribe to app_updates topic
        FirebaseMessaging.getInstance().subscribeToTopic("app_updates")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(LOG_TAG, "Successfully subscribed to app_updates topic.");
                    } else {
                        Log.e(LOG_TAG, "Failed to subscribe to app_updates topic.");
                    }
                });

        // Enable Crashlytics collection
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.setCrashlyticsCollectionEnabled(true);
        Log.d(LOG_TAG, "Crashlytics collection enabled.");

        // Check for a stored message in SharedPreferences
        SharedPreferences preferences = getSharedPreferences("AppMessages", MODE_PRIVATE);

        boolean isFirstLaunch = preferences.getBoolean("isFirstLaunch", false);
        Log.d(LOG_TAG, "isFirstLaunch: " + isFirstLaunch);

        if (isFirstLaunch) {
            // Tag the user as a first-time launcher
            OneSignal.sendTag("first_launch", "true");

            // Update SharedPreferences to mark the first launch as complete
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isFirstLaunch", false);
            editor.apply();
        }

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> Log.d(LOG_TAG, "Mobile Ads SDK initialized."));

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
        Log.d(LOG_TAG, "Checking if feedback popup should be shown.");
        // SharedPreferences to store the last shown timestamp
        SharedPreferences sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
        long lastShown = sharedPreferences.getLong("FeedbackPopupLastShown", 0L);
        long currentTime = System.currentTimeMillis();

        // Check if a week (7 days) has passed
        if (currentTime - lastShown >= 7 * 24 * 60 * 60 * 1000) {
            Log.d(LOG_TAG, "A week has passed since the last feedback popup. Showing popup.");
            // Show the popup
            showFeedbackPopup();

            // Update the last shown time
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong("FeedbackPopupLastShown", currentTime);
            editor.apply();
        } else {
            Log.d(LOG_TAG, "Feedback popup not shown. Less than a week since last display.");
        }
    }

    private void showFeedbackPopup() {
        Log.d(LOG_TAG, "Displaying feedback popup.");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Got Feedback?");
        builder.setMessage("Help us improve ManagerDB! Email us at managerdbapp@gmail.com.");
        builder.setPositiveButton("Send Feedback", (dialog, which) -> {
            Log.d(LOG_TAG, "Feedback popup: Send Feedback clicked.");
            // Open email app
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:managerdbapp@gmail.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback for ManagerDB");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Log.w(LOG_TAG, "No email apps installed to handle feedback.");
                Toast.makeText(MainActivity.this, "No email apps installed", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Dismiss", (dialog, which) -> {
            Log.d(LOG_TAG, "Feedback popup: Dismiss clicked.");
            dialog.dismiss();
        });

        // Show the dialog
        builder.create().show();
    }

    private void showUpdateDialog(String title, String message) {
        Log.d(LOG_TAG, "Displaying update dialog with title: " + title + " and message: " + message);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    Log.d(LOG_TAG, "Update dialog dismissed.");
                    dialog.dismiss();
                })                .create()
                .show();
    }
}