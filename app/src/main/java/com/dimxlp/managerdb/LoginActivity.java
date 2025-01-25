package com.dimxlp.managerdb;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Objects;
import java.util.concurrent.Executor;

import util.UserApi;

public class LoginActivity extends AppCompatActivity {

    private static final String LOG_TAG = "RAFI|Login";
    private AutoCompleteTextView email;
    private EditText password;
    private Button loginButton;
    private Button createAccountButton;
    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUser;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference userColReference = db.collection("Users");
    private CollectionReference managersColReference = db.collection("Managers");
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private boolean isBiometricsSupported = false;
    private NativeAd nativeAdTop, nativeAdBottom;
    private NativeAdView nativeAdViewTop, nativeAdViewBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Log.i(LOG_TAG, "LoginActivity launched.");

        firebaseAuth = FirebaseAuth.getInstance();

        email = findViewById(R.id.login_email);
        password = findViewById(R.id.password_login);
        loginButton = findViewById(R.id.login_button);
        createAccountButton = findViewById(R.id.create_account_button);
        progressBar = findViewById(R.id.login_progress_bar);

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> Log.d(LOG_TAG, "Mobile Ads SDK initialized."));

        // Load Native Ads
        nativeAdViewTop = findViewById(R.id.native_ad_view_top);
        nativeAdViewBottom = findViewById(R.id.native_ad_view_bottom);
        loadNativeAd("ca-app-pub-8349697523222717/1143352431", nativeAdViewTop);
        loadNativeAd("ca-app-pub-8349697523222717/3330532077", nativeAdViewBottom);

        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, CreateAccountActivity.class));
            }
        });

        if (isBiometricsSupported) {
            setupBiometricPrompt();

            // Check if biometric login is available and prompt if it is
            BiometricManager biometricManager = BiometricManager.from(this);
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                showBiometricPrompt();
            }
        }

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Login button clicked.");
                loginUser(email.getText().toString().trim(), password.getText().toString().trim());
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

    private void setupBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(LoginActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(LoginActivity.this, "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                // Biometric authentication succeeded, proceed with login
                checkAccountStatusAndLogin();
                finish();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Login")
                .setSubtitle("Use your fingerprint to login")
                .setNegativeButtonText("Cancel")
                .build();
    }

    private void checkAccountStatusAndLogin() {
        SharedPreferences sharedPreferences = getSharedPreferences("com.dimxlp.managerdb", MODE_PRIVATE);
        boolean hasAccount = sharedPreferences.getBoolean("hasAccount", false);
        String userId = sharedPreferences.getString("userId", null);

        if (hasAccount && userId != null) {
            // User has an account, proceed to SelectManagerActivity
            managersColReference.whereEqualTo("userId", userId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                if (!Objects.requireNonNull(task.getResult()).isEmpty()) {
                                    startActivity(new Intent(LoginActivity.this, SelectManagerActivity.class));
                                } else {
                                    startActivity(new Intent(LoginActivity.this, CreateManagerActivity.class));
                                }
                            }
                        }
                    });

        } else {
            // No account exists, redirect to CreateAccountActivity
            Toast.makeText(LoginActivity.this, "No account found. Please create an account first.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(LoginActivity.this, CreateAccountActivity.class));
        }
    }

    private void showBiometricPrompt() {
        biometricPrompt.authenticate(promptInfo);
    }

    private void loginUser(String emailText, String pwdText) {

        progressBar.setVisibility(View.VISIBLE);
        if (!TextUtils.isEmpty(emailText) &&
            !TextUtils.isEmpty(pwdText)) {

            Log.d(LOG_TAG, "Attempting to log in user with email: " + emailText);
            firebaseAuth.signInWithEmailAndPassword(emailText, pwdText)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if (task.isSuccessful()) {
                                Log.i(LOG_TAG, "User successfully logged in.");
                                // Sign in success, update UI with the signed-in user's information
                                FirebaseUser user = firebaseAuth.getCurrentUser();
                                assert user != null;
                                String userId = user.getUid();

                                if (isBiometricsSupported) {
                                    SharedPreferences sharedPreferences = getSharedPreferences("com.dimxlp.managerdb", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putBoolean("hasAccount", true);
                                    editor.putString("userId", userId);
                                    editor.apply();
                                }

                                userColReference.whereEqualTo("userId", userId)
                                        .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                            @Override
                                            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                                                if (error != null) {
                                                    Log.e(LOG_TAG, "Error fetching user data.", error);
                                                    return;
                                                }
                                                assert value != null;
                                                if (!value.isEmpty()) {
                                                    progressBar.setVisibility(View.INVISIBLE);
                                                    Log.d(LOG_TAG, "User data fetched successfully.");

                                                    for (QueryDocumentSnapshot snapshot: value) {
                                                        UserApi userApi = UserApi.getInstance();
                                                        userApi.setUsername(snapshot.getString("username"));
                                                        userApi.setUserId(snapshot.getString("userId"));

                                                        managersColReference.whereEqualTo("userId", userApi.getUserId())
                                                                .get()
                                                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                        if (task.isSuccessful()) {
                                                                            Log.d(LOG_TAG, "Manager data fetched. Size: " + task.getResult().size());
                                                                            if (Objects.requireNonNull(task.getResult()).size() > 0) {
                                                                                startActivity(new Intent(LoginActivity.this, SelectManagerActivity.class));
                                                                            } else {
                                                                                startActivity(new Intent(LoginActivity.this, CreateManagerActivity.class));
                                                                            }
                                                                        }
                                                                    }
                                                                });
                                                    }
                                                }
                                            }
                                        });
                            } else {
                                Log.w(LOG_TAG, "Email or password is empty. Login aborted.");
                                // If sign in fails, display a message to the user.
                                Toast.makeText(LoginActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                            }


                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    });

        } else {
            progressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "onStart called.");
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
        Log.d(LOG_TAG, "LoginActivity destroyed.");
    }
}