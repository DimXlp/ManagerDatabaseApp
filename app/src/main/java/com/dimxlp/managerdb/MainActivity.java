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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.onesignal.OneSignal;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "RAFI|Main";
    private Button getStartedButton;
    private NativeAd nativeAdTop, nativeAdBottom;
    private NativeAdView nativeAdViewTop, nativeAdViewBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(LOG_TAG, "MainActivity launched.");

        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
        );

        // Install App Check Debug Token Provider
//        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
//        firebaseAppCheck.installAppCheckProviderFactory(
//                DebugAppCheckProviderFactory.getInstance()
//        );
//        Log.d(LOG_TAG, "Debug Token: " + DebugAppCheckProviderFactory);

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);  // Default instance uses restricted API key
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();  // Firestore uses restricted API key
        Log.d(LOG_TAG, "Firestore initialized with default API Key: " + FirebaseApp.getInstance().getOptions().getApiKey());

        FirebaseAppCheck.getInstance().getAppCheckToken(false)
                .addOnSuccessListener(tokenResult -> Log.d(LOG_TAG, "App Check Token: " + tokenResult.getToken()))
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Failed to get App Check token", e));

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

        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        var consentInformation = UserMessagingPlatform.getConsentInformation(this);
        consentInformation.requestConsentInfoUpdate(this, params,
                () -> {
                    if (consentInformation.isConsentFormAvailable()) {
                        loadConsentForm();
                    }
                },
                formError -> {
                    Log.d(LOG_TAG, "Error: " + formError.getMessage());
                });

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> Log.d(LOG_TAG, "Mobile Ads SDK initialized."));

        // Load Native Ads
        nativeAdViewTop = findViewById(R.id.native_ad_view_top);
        nativeAdViewBottom = findViewById(R.id.native_ad_view_bottom);
        loadNativeAd("ca-app-pub-8349697523222717/4110897751", nativeAdViewTop);
        loadNativeAd("ca-app-pub-8349697523222717/6858713549", nativeAdViewBottom);

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

    private void loadConsentForm() {
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(this,
                formError -> {
                    if (formError != null) {
                        System.out.println("Error loading form: " + formError.getMessage());
                    }
                }
        );
    }

    private void loadNativeAd(String adUnitId, NativeAdView nativeAdView) {
        AdLoader adLoader = new AdLoader.Builder(this, adUnitId)
                .forNativeAd(ad -> {
                    if (isDestroyed()) {
                        ad.destroy();
                        return;
                    }
                    if (nativeAdView == nativeAdViewTop) {
                        nativeAdTop = ad;
                    } else {
                        nativeAdBottom = ad;
                    }
                    populateNativeAdView(ad, nativeAdView);
                })
                .withAdListener(new com.google.android.gms.ads.AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {

                    }
                })
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private void populateNativeAdView(NativeAd nativeAd, NativeAdView nativeAdView) {

        // Dynamically identify view IDs based on the nativeAdView
        int headlineId = nativeAdView == nativeAdViewTop ? R.id.ad_headline_top : R.id.ad_headline_bottom;
        int bodyId = nativeAdView == nativeAdViewTop ? R.id.ad_body_top : R.id.ad_body_bottom;
        int callToActionId = nativeAdView == nativeAdViewTop ? R.id.ad_call_to_action_top : R.id.ad_call_to_action_bottom;

        // Set the views for the NativeAdView
        nativeAdView.setHeadlineView(nativeAdView.findViewById(headlineId));
        nativeAdView.setBodyView(nativeAdView.findViewById(bodyId));
        nativeAdView.setCallToActionView(nativeAdView.findViewById(callToActionId));

        // Populate the Headline
        if (nativeAd.getHeadline() != null) {
            ((TextView) nativeAdView.getHeadlineView()).setText(nativeAd.getHeadline());
            nativeAdView.getHeadlineView().setVisibility(View.VISIBLE);
        } else {
            nativeAdView.getHeadlineView().setVisibility(View.GONE);
        }

        // Populate the Body
        if (nativeAd.getBody() != null) {
            ((TextView) nativeAdView.getBodyView()).setText(nativeAd.getBody());
            nativeAdView.getBodyView().setVisibility(View.VISIBLE);
        } else {
            nativeAdView.getBodyView().setVisibility(View.GONE);
        }

        // Populate the Call-to-Action
        if (nativeAd.getCallToAction() != null) {
            ((Button) nativeAdView.getCallToActionView()).setText(nativeAd.getCallToAction());
            nativeAdView.getCallToActionView().setVisibility(View.VISIBLE);
        } else {
            nativeAdView.getCallToActionView().setVisibility(View.GONE);
        }

        // Bind the NativeAd object to the NativeAdView
        nativeAdView.setNativeAd(nativeAd);
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

    @Override
    protected void onDestroy() {
        if (nativeAdTop != null) {
            nativeAdTop.destroy();
        }
        if (nativeAdBottom != null) {
            nativeAdBottom.destroy();
        }
        super.onDestroy();
        Log.d(LOG_TAG, "MainActivity destroyed.");
    }
}