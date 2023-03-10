package com.dimxlp.managerdb;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

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

import util.UserApi;

public class LoginActivity extends AppCompatActivity {

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
    private boolean managersExist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

       //Objects.requireNonNull(getSupportActionBar()).setElevation(0);

        firebaseAuth = FirebaseAuth.getInstance();

        email = findViewById(R.id.login_email);
        password = findViewById(R.id.password_login);
        loginButton = findViewById(R.id.login_button);
        createAccountButton = findViewById(R.id.create_account_button);
        progressBar = findViewById(R.id.login_progress_bar);

        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, CreateAccountActivity.class));
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser(email.getText().toString().trim(), password.getText().toString().trim());
            }
        });

    }

    private void loginUser(String emailText, String pwdText) {

        progressBar.setVisibility(View.VISIBLE);
        if (!TextUtils.isEmpty(emailText) &&
            !TextUtils.isEmpty(pwdText)) {

            firebaseAuth.signInWithEmailAndPassword(emailText, pwdText)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            assert user != null;
                            String userId = user.getUid();

                            userColReference.whereEqualTo("userId", userId)
                                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                        @Override
                                        public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                                            if (error != null) {
                                                return;
                                            }
                                            assert value != null;
                                            if (!value.isEmpty()) {
                                                progressBar.setVisibility(View.INVISIBLE);

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
                                                                        Log.d("RAFI", "onComplete: size = " + task.getResult().size());
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
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    });

        }
    }

    @Override
    protected void onStart() {
        super.onStart();


    }
}