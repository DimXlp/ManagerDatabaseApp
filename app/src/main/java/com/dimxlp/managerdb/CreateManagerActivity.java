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
import android.widget.TextView;

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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import enumeration.CurrencyEnum;
import model.FirstTeamPlayer;
import model.Manager;
import util.UserApi;

public class CreateManagerActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String LOG_TAG = "RAFI|CreateManager";
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
    private NativeAd nativeAdTop, nativeAdBottom;
    private NativeAdView nativeAdViewTop, nativeAdViewBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_manager);

        Log.i(LOG_TAG, "CreateManagerActivity launched.");

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

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> Log.d(LOG_TAG, "Mobile Ads SDK initialized."));

        nativeAdViewBottom = findViewById(R.id.native_ad_view_bottom);
        loadNativeAd("ca-app-pub-8349697523222717/4244653333", nativeAdViewBottom);

        List<String> currencies = Arrays.stream(CurrencyEnum.values())
                .map(CurrencyEnum::getSymbol)
                .collect(Collectors.toList());

        ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(CreateManagerActivity.this, android.R.layout.simple_spinner_item, currencies);
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinner.setAdapter(currencyAdapter);

        uploadButton.setOnClickListener(this);
        create_button.setOnClickListener(this);

        firebaseAuth = FirebaseAuth.getInstance();

        if (UserApi.getInstance() != null) {
            currentUserId = UserApi.getInstance().getUserId();
            currentUserName = UserApi.getInstance().getUsername();
            Log.d(LOG_TAG, "UserApi initialized: userId=" + currentUserId + ", username=" + currentUserName);
        }

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                currentUser = firebaseAuth.getCurrentUser();
                if (currentUser != null) {
                    Log.i(LOG_TAG, "User logged in: " + currentUser.getUid());
                } else {
                    Log.i(LOG_TAG, "No user logged in.");
                }
            }
        };
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
                    Log.d(LOG_TAG, "Native ad loaded successfully.");
                })
                .withAdListener(new com.google.android.gms.ads.AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        Log.e(LOG_TAG, "Native ad failed to load: " + adError.getMessage());
                    }
                })
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private void populateNativeAdView(NativeAd nativeAd, NativeAdView nativeAdView) {
        Log.d(LOG_TAG, "Populating native ad view.");
        // Dynamically assign IDs
        int headlineId = nativeAdView == nativeAdViewTop ? R.id.ad_headline_top : R.id.ad_headline_bottom;
        int bodyId = nativeAdView == nativeAdViewTop ? R.id.ad_body_top : R.id.ad_body_bottom;
        int callToActionId = nativeAdView == nativeAdViewTop ? R.id.ad_call_to_action_top : R.id.ad_call_to_action_bottom;

        // Set views for the NativeAdView
        nativeAdView.setHeadlineView(nativeAdView.findViewById(headlineId));
        nativeAdView.setBodyView(nativeAdView.findViewById(bodyId));
        nativeAdView.setCallToActionView(nativeAdView.findViewById(callToActionId));

        // Populate the Headline
        ((TextView) nativeAdView.getHeadlineView()).setText(nativeAd.getHeadline());

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

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(LOG_TAG, "CreateManagerActivity started.");
        currentUser = firebaseAuth.getCurrentUser();
        firebaseAuth.addAuthStateListener(authStateListener);

        collectionReference.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            Log.d(LOG_TAG, "Managers data retrieved successfully.");
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
                                    Log.d(LOG_TAG, "Assigned new ID to manager: " + manager.getId());
                                }
                            }
                        } else {
                            Log.w(LOG_TAG, "No managers data found.");
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
        Log.d(LOG_TAG, "Max ID found: " + maxId);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (firebaseAuth != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
        Log.i(LOG_TAG, "CreateManagerActivity stopped.");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.manager_create_button:
                Log.d(LOG_TAG, "Create Manager button clicked.");
                saveManager();
                break;
            case R.id.upload_button_create:
                Log.d(LOG_TAG, "Upload button clicked.");
                openFileChooser();
                break;
        }
    }

    private void openFileChooser() {
        Log.d(LOG_TAG, "Opening file chooser for image selection.");
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void saveManager() {
        Log.d(LOG_TAG, "Attempting to save manager.");
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

            Log.i(LOG_TAG, "All fields validated. Preparing to save manager: " + firstName + " " + lastName);

            final StorageReference filepath = storageReference
                    .child("team_badges")
                    .child(team + "_" + Timestamp.now().getSeconds());

            final Manager manager = new Manager();

            if (teamBadgeUri != null) {
                filepath.putFile(teamBadgeUri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        String imageUrl = uri.toString();
                                        Log.d(LOG_TAG, "Image uploaded successfully. URL: " + imageUrl);

                                        manager.setTeamBadgeUrl(imageUrl);
                                    }
                                });
                            }
                        })
                        .addOnFailureListener(e -> Log.e(LOG_TAG, "Image upload failed: " + e.getMessage(), e));
            }

            manager.setId(maxId+1);
            manager.setFirstName(firstName);
            manager.setLastName(lastName);
            manager.setFullName(firstName + " " + lastName);
            manager.setNationality(nationality);
            manager.setTeam(team);
            manager.setCurrency(currency);
            manager.setTimeAdded(new Timestamp(new Date()));
            manager.setUserId(currentUserId);
            Log.d(LOG_TAG, "Manager object prepared: " + manager.getFullName());

            collectionReference.add(manager)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.i(LOG_TAG, "Manager saved successfully. ID: " + manager.getId());
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
                            Log.e(LOG_TAG, "Failed to save manager: " + e.getMessage(), e);
                        }
                    });

        } else {
            Log.w(LOG_TAG, "Validation failed: One or more fields are empty.");
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
            Log.d(LOG_TAG, "Image selected successfully: " + teamBadgeUri.toString());
        }
    }

    @Override
    protected void onDestroy() {
        if (nativeAdTop != null) nativeAdTop.destroy();
        if (nativeAdBottom != null) nativeAdBottom.destroy();
        Log.i(LOG_TAG, "CreateManagerActivity destroyed.");
        super.onDestroy();
    }
}