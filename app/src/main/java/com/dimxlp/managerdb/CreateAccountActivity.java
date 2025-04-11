package com.dimxlp.managerdb;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.recaptcha.Recaptcha;
import com.google.android.recaptcha.RecaptchaAction;
import com.google.android.recaptcha.RecaptchaTasksClient;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import util.UserApi;

public class CreateAccountActivity extends AppCompatActivity {

    private static final String LOG_TAG = "RAFI|CreateAccount";
    private EditText usernameText;
    private AutoCompleteTextView emailText;
    private EditText passwordText;
    private Button create_account_button;
    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUser;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private CollectionReference collectionReference = db.collection("Users");
    private boolean isBiometricsSupported = false;
    private NativeAd nativeAdTop, nativeAdBottom;
    private NativeAdView nativeAdViewTop, nativeAdViewBottom;
    private RecaptchaTasksClient recaptchaClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        Log.i(LOG_TAG, "CreateAccountActivity launched.");

//        // Force Firebase to use the unrestricted API key for authentication
//        FirebaseOptions options = new FirebaseOptions.Builder()
//                .setApplicationId(FirebaseApp.getInstance().getOptions().getApplicationId())
//                .setApiKey(BuildConfig.FIREBASE_AUTH_API_KEY)  // Force correct API Key
//                .setProjectId(FirebaseApp.getInstance().getOptions().getProjectId())
//                .build();
//
//        FirebaseApp authApp;
//        try {
//            authApp = FirebaseApp.getInstance("authApp");
//        } catch (IllegalStateException e) {
//            authApp = FirebaseApp.initializeApp(this, options, "authApp");
//        }
//
//        firebaseAuth = FirebaseAuth.getInstance(authApp);

        firebaseAuth = FirebaseAuth.getInstance();

        Log.d(LOG_TAG, "Using Firebase Authentication API Key: " + firebaseAuth.getApp().getOptions().getApiKey());
        
        usernameText = findViewById(R.id.username_register);
        emailText = findViewById(R.id.email_register);
        passwordText = findViewById(R.id.password_register);
        create_account_button = findViewById(R.id.create_button_register);
        progressBar = findViewById(R.id.create_progress_bar);

        Recaptcha.getTasksClient(getApplication(), BuildConfig.RECAPTCHA_API_KEY)
                .addOnSuccessListener(client -> recaptchaClient = client)
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Failed to initialize reCAPTCHA.", e));

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> Log.d(LOG_TAG, "Mobile Ads SDK initialized."));

        // Load Native Ads
        nativeAdViewTop = findViewById(R.id.native_ad_view_top);
        nativeAdViewBottom = findViewById(R.id.native_ad_view_bottom);
        loadNativeAd("ca-app-pub-3940256099942544/2247696110", nativeAdViewTop);
        loadNativeAd("ca-app-pub-3940256099942544/2247696110", nativeAdViewBottom);

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                currentUser = firebaseAuth.getCurrentUser();

                if (currentUser != null) {
                    Log.i(LOG_TAG, "User already logged in: " + currentUser.getUid());
                } else {
                    Log.i(LOG_TAG, "No user logged in.");
                }
            }
        };

        create_account_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Create Account button clicked.");

                String usernameValue = usernameText.getText().toString();
                String emailValue = emailText.getText().toString();
                String passwordValue = passwordText.getText().toString();

                if (!isValidEmailDomain(emailValue)) {
                    Log.w(LOG_TAG, "Invalid email domain entered: " + emailValue);
                    emailText.setError("Please use a valid email domain.");
                    return;
                }

                if (!TextUtils.isEmpty(usernameValue) &&
                    !TextUtils.isEmpty(emailValue) &&
                    !TextUtils.isEmpty(passwordValue)) {

                    Log.i(LOG_TAG, "Valid input detected. Proceeding to create account.");

                    String username = usernameValue.trim();
                    String email = emailValue.trim();
                    String password = passwordValue.trim();

                    verifyRecaptchaAndCreateAccount(username, email, password);

                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        String userId = user.getUid();
                        Log.i(LOG_TAG, "Current user retrieved: " + userId);

                        if (isBiometricsSupported) {
                            SharedPreferences sharedPreferences = getSharedPreferences("com.dimxlp.managerdb", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean("hasAccount", true);
                            editor.putString("userId", userId);
                            editor.apply();
                            Log.d(LOG_TAG, "Biometrics preference saved for user: " + userId);
                        }
                    }

                } else {
                    Log.w(LOG_TAG, "Empty fields detected.");
                    Toast.makeText(CreateAccountActivity.this, "Empty Fields Not Allowed", Toast.LENGTH_LONG).show();
                }
            }
        });

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

    private void verifyRecaptchaAndCreateAccount(final String username, String email, String password) {

        Log.d(LOG_TAG, "Starting account creation process.");
        progressBar.setVisibility(View.VISIBLE);

        if (recaptchaClient == null) {
            Log.e(LOG_TAG, "reCAPTCHA Client is not initialized.");
            Toast.makeText(CreateAccountActivity.this, "Error: reCAPTCHA not initialized.", Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.INVISIBLE);
            return;
        }

        // **Execute reCAPTCHA with "signup_attempt" action**
        recaptchaClient.executeTask(RecaptchaAction.SIGNUP)
                .addOnSuccessListener(token -> {
                    Log.d(LOG_TAG, "reCAPTCHA verified successfully. Token: " + token);
                    createUserEmailAccount(username, email, password);
                })
                .addOnFailureListener(e -> {
                    Log.e(LOG_TAG, "reCAPTCHA failed.", e);
                    Toast.makeText(CreateAccountActivity.this, "reCAPTCHA verification failed. Try again.", Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.INVISIBLE);
                });

    }

    private void createUserEmailAccount(final String username, String email, String password) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {

                            Log.i(LOG_TAG, "Account creation successful for email: " + email);

                            currentUser = firebaseAuth.getCurrentUser();
                            assert currentUser != null;
                            final String currentUserId = currentUser.getUid();

                            currentUser.sendEmailVerification()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> emailTask) {
                                            if (emailTask.isSuccessful()) {
                                                Log.i(LOG_TAG, "Verification email sent to " + email);
                                                Toast.makeText(CreateAccountActivity.this,
                                                        "Verification email sent. Please check your inbox.",
                                                        Toast.LENGTH_LONG).show();
                                            } else {
                                                Log.e(LOG_TAG, "Failed to send verification email.", emailTask.getException());
                                                Toast.makeText(CreateAccountActivity.this,
                                                        "Failed to send verification email. Try again later.",
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });

                            Map<String, String> userObj = new HashMap<>();
                            userObj.put("userId", currentUserId);
                            userObj.put("username", username);

                            collectionReference.add(userObj)
                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                        @Override
                                        public void onSuccess(DocumentReference documentReference) {
                                            Log.i(LOG_TAG, "User document created successfully: " + documentReference.getId());
                                            documentReference.get()
                                                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                            if (Objects.requireNonNull(task.getResult()).exists()) {
                                                                progressBar.setVisibility(View.INVISIBLE);
                                                                String name = task.getResult().getString("username");

                                                                Log.i(LOG_TAG, "Document snapshot retrieved. Username: " + name);

                                                                UserApi userApi = UserApi.getInstance();
                                                                userApi.setUserId(currentUserId);
                                                                userApi.setUsername(name);

                                                                Intent intent = new Intent(CreateAccountActivity.this, CreateManagerActivity.class);
                                                                intent.putExtra("username", name);
                                                                intent.putExtra("userId", currentUserId);
                                                                startActivity(intent);

                                                            } else {
                                                                progressBar.setVisibility(View.INVISIBLE);
                                                                Log.w(LOG_TAG, "Document snapshot does not exist.");
                                                            }

                                                        }
                                                    });
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {

                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    private boolean isValidEmailDomain(String email) {
        String[] validDomains = {
                "gmail.com",
                "yahoo.com",
                "outlook.com",
                "hotmail.com",
                "icloud.com",
                "aol.com",
                "protonmail.com",
                "zoho.com",
                "mail.com",
                "yandex.com",
                "gmx.com",
                "me.com",
                "live.com",
                "att.net",
                "fastmail.com"
        };
        String domain = email.substring(email.indexOf("@") + 1);
        boolean isValid = Arrays.stream(validDomains).anyMatch(domain::equalsIgnoreCase);
        Log.d(LOG_TAG, "Email domain validation for " + email + ": " + isValid);
        return isValid;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(LOG_TAG, "CreateAccountActivity started.");
        currentUser = firebaseAuth.getCurrentUser();
        firebaseAuth.addAuthStateListener(authStateListener);
    }
}