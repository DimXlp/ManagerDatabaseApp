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
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        Log.i(LOG_TAG, "CreateAccountActivity launched.");

        firebaseAuth = FirebaseAuth.getInstance();

        usernameText = findViewById(R.id.username_register);
        emailText = findViewById(R.id.email_register);
        passwordText = findViewById(R.id.password_register);
        create_account_button = findViewById(R.id.create_button_register);
        progressBar = findViewById(R.id.create_progress_bar);

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> Log.d(LOG_TAG, "Mobile Ads SDK initialized."));

        // Load Banner Ad
        AdView mainBanner1 = findViewById(R.id.create_account_banner_1);
        AdRequest adBannerRequest1 = new AdRequest.Builder().build();
        mainBanner1.loadAd(adBannerRequest1);
        Log.d(LOG_TAG, "Main banner ad 1 loaded.");

        AdView mainBanner2 = findViewById(R.id.create_account_banner_2);
        AdRequest adBannerRequest2 = new AdRequest.Builder().build();
        mainBanner2.loadAd(adBannerRequest2);
        Log.d(LOG_TAG, "Main banner ad 2 loaded.");

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

                    createUserEmailAccount(username, email, password);

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

    private void createUserEmailAccount(final String username, String email, String password) {

        Log.d(LOG_TAG, "Starting account creation process.");
        progressBar.setVisibility(View.VISIBLE);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {

                            Log.i(LOG_TAG, "Account creation successful for email: " + email);
                            currentUser = firebaseAuth.getCurrentUser();

                            assert currentUser != null;
                            final String currentUserId = currentUser.getUid();

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
        String[] validDomains = {"gmail.com", "yahoo.com", "outlook.com", "hotmail.com"};
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