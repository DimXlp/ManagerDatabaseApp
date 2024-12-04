package com.dimxlp.managerdb;

import androidx.annotation.NonNull;
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
import android.widget.Toast;

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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import util.UserApi;

public class CreateAccountActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        firebaseAuth = FirebaseAuth.getInstance();

        usernameText = findViewById(R.id.username_register);
        emailText = findViewById(R.id.email_register);
        passwordText = findViewById(R.id.password_register);
        create_account_button = findViewById(R.id.create_button_register);
        progressBar = findViewById(R.id.create_progress_bar);

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                currentUser = firebaseAuth.getCurrentUser();

                if (currentUser != null) {
                    // user is already logged in
                } else {
                    // new user
                }

            }
        };




        create_account_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!TextUtils.isEmpty(usernameText.getText().toString())
                    && !TextUtils.isEmpty(emailText.getText().toString())
                    && !TextUtils.isEmpty(passwordText.getText().toString())) {

                   // Log.d("CreateAccount", "onComplete: 111111111" );

                    String username = usernameText.getText().toString().trim();
                    String email = emailText.getText().toString().trim();
                    String password = passwordText.getText().toString().trim();

                    createUserEmailAccount(username, email, password);

                } else {
                    Toast.makeText(CreateAccountActivity.this, "Empty Fields Not Allowed", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    private void createUserEmailAccount(final String username, String email, String password) {

        if (!TextUtils.isEmpty(username) &&
            !TextUtils.isEmpty(email) &&
            !TextUtils.isEmpty(password)) {

            progressBar.setVisibility(View.VISIBLE);


            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if (task.isSuccessful()) {

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
                                                documentReference.get()
                                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                if (Objects.requireNonNull(task.getResult()).exists()) {


                                                                    progressBar.setVisibility(View.INVISIBLE);
                                                                    String name = task.getResult().getString("username");

                                                                    UserApi userApi = UserApi.getInstance();
                                                                    userApi.setUserId(currentUserId);
                                                                    userApi.setUsername(name);

                                                                    Intent intent = new Intent(CreateAccountActivity.this, CreateManagerActivity.class);
                                                                    intent.putExtra("username", name);
                                                                    intent.putExtra("userId", currentUserId);
                                                                    startActivity(intent);

                                                                } else {
                                                                    progressBar.setVisibility(View.INVISIBLE);
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

    }

    @Override
    protected void onStart() {
        super.onStart();

        currentUser = firebaseAuth.getCurrentUser();
        firebaseAuth.addAuthStateListener(authStateListener);
    }
}