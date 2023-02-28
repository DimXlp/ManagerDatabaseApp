package com.dimxlp.managerdb;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import model.FirstTeamPlayer;
import model.Manager;
import util.UserApi;

public class CreateManagerActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ProgressBar progressBar;
    private EditText firstNameText;
    private EditText lastNameText;
    private EditText nationalityText;
    private EditText teamText;
    private ImageView badgeImage;
    private Button uploadButton;
    private Spinner currencySpinner;
    private Button create_button;
    private int imageId;

    private String currentUserId;
    private String currentUserName;

    private Uri managerImageUri;
    private Uri teamBadgeUri;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUser;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private StorageReference storageReference;

    private CollectionReference collectionReference = db.collection("Managers");
    private long maxId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_manager);

        storageReference = FirebaseStorage.getInstance().getReference();

        progressBar = findViewById(R.id.create_progress_bar);
        progressBar.setVisibility(View.INVISIBLE);

        firstNameText = findViewById(R.id.first_name_create);
        lastNameText = findViewById(R.id.last_name_create);
        nationalityText = findViewById(R.id.nationality_create);
        teamText = findViewById(R.id.team_create);
        badgeImage = findViewById(R.id.team_badge_image_create);
        uploadButton = findViewById(R.id.upload_button_create);
        currencySpinner = findViewById(R.id.currency_spinner_create);
        create_button = findViewById(R.id.manager_create_button);

        String[] currencyArray = {"€", "£", "$"};

        ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(CreateManagerActivity.this, android.R.layout.simple_spinner_item, currencyArray);
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinner.setAdapter(currencyAdapter);

        uploadButton.setOnClickListener(this);
        create_button.setOnClickListener(this);

        firebaseAuth = FirebaseAuth.getInstance();

        if (UserApi.getInstance() != null) {
            currentUserId = UserApi.getInstance().getUserId();
            currentUserName = UserApi.getInstance().getUsername();
        }

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                currentUser = firebaseAuth.getCurrentUser();
                if (currentUser != null) {

                } else {

                }
            }
        };
    }


    @Override
    protected void onStart() {
        super.onStart();
        currentUser = firebaseAuth.getCurrentUser();
        firebaseAuth.addAuthStateListener(authStateListener);

        collectionReference.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                            List<Manager> managers = new ArrayList<>();
                            for (DocumentSnapshot doc : docs) {
                                Manager manager = doc.toObject(Manager.class);
                                managers.add(manager);
                            }
                            findMaxId(managers);
                            for (DocumentSnapshot ds: docs) {
                                Manager manager = ds.toObject(Manager.class);
                                assert manager != null;
                                if (manager.getId() == 0) {
                                    manager.setId(maxId+1);
                                    collectionReference.document(ds.getId()).update("id", manager.getId());
                                    maxId++;
                                }
                            }
                        }
                    }
                });
    }

    private void findMaxId(List<Manager> managers) {
        maxId = managers.get(0).getId();
        for (Manager mng: managers) {
            if (mng.getId() > maxId) {
                maxId = mng.getId();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (firebaseAuth != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.manager_create_button:
                saveManager();
                break;
            case R.id.upload_button_create:
                openFileChooser();
                break;
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void saveManager() {
        final String firstName = firstNameText.getText().toString().trim();
        final String lastName = lastNameText.getText().toString().trim();
        final String nationality = nationalityText.getText().toString().trim();
        final String team = teamText.getText().toString().trim();
        final String currency = currencySpinner.getSelectedItem().toString().trim();

        if (!TextUtils.isEmpty(firstName) &&
            !TextUtils.isEmpty(lastName) &&
            !TextUtils.isEmpty(nationality) &&
            !TextUtils.isEmpty(team) &&
            !TextUtils.isEmpty(currency)) {

            final StorageReference filepath = storageReference
                    .child("team_badges")
                    .child(team + "_" + Timestamp.now().getSeconds());

            filepath.putFile(teamBadgeUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    String imageUrl = uri.toString();
                                    Log.d("RAFI", "onSuccess: " + imageUrl);

                                    final Manager manager = new Manager();

                                    manager.setId(maxId+1);
                                    manager.setFirstName(firstName);
                                    manager.setLastName(lastName);
                                    manager.setFullName(firstName + " " + lastName);
                                    manager.setNationality(nationality);
                                    manager.setTeam(team);
                                    manager.setCurrency(currency);
                                    manager.setTeamBadgeUrl(imageUrl);
                                    manager.setTimeAdded(new Timestamp(new Date()));
                                    Log.d("currentUserId", "saveManager: " + currentUserId);
                                    manager.setUserId(currentUserId);

                                    collectionReference.add(manager)
                                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                @Override
                                                public void onSuccess(DocumentReference documentReference) {
                                                    progressBar.setVisibility(View.INVISIBLE);

                                                    Intent intent = new Intent(CreateManagerActivity.this, ManageTeamActivity.class);
                                                    intent.putExtra("team", team);
                                                    intent.putExtra("managerId", manager.getId());
                                                    startActivity(intent);
                                                    finish();
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {

                                                }
                                            });
                                }
                            });
                        }
                    });

        } else {
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            teamBadgeUri = data.getData();
            badgeImage.setImageURI(teamBadgeUri);
            Picasso.get().load(teamBadgeUri).into(badgeImage);
        }
    }
}