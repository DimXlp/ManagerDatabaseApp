package com.dimxlp.managerdb;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputLayout;
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
import java.util.stream.Collectors;

import enumeration.CurrencyEnum;
import model.Manager;
import util.UserApi;

public class CreateManagerActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String LOG_TAG = "RAFI|CreateManager";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1001;

    private ProgressBar progressBar;
    private EditText firstNameText, lastNameText, nationalityText, teamText;
    private ImageView badgeImage;
    private Button uploadButton, create_button;
    private TextView selectedCurrencyText;

    private String currentUserId, currentUserName;
    private Uri teamBadgeUri;

    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private StorageReference storageReference;

    private CollectionReference collectionReference = db.collection("Managers");
    private long maxId;
    private NativeAd nativeAdTop;
    private NativeAdView nativeAdViewTop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_manager);
        Log.i(LOG_TAG, "CreateManagerActivity launched.");

        storageReference = FirebaseStorage.getInstance().getReference();

        authStateListener = firebaseAuth -> {
            currentUser = firebaseAuth.getCurrentUser();
            if (currentUser != null) {
                Log.i(LOG_TAG, "User logged in: " + currentUser.getUid());
            } else {
                Log.i(LOG_TAG, "No user logged in.");
            }
        };

        progressBar = findViewById(R.id.create_progress_bar);
        progressBar.setVisibility(View.INVISIBLE);

        firstNameText = findViewById(R.id.first_name_create);
        lastNameText = findViewById(R.id.last_name_create);
        nationalityText = findViewById(R.id.nationality_create);
        teamText = findViewById(R.id.team_create);
        badgeImage = findViewById(R.id.team_badge_image_create);
        uploadButton = findViewById(R.id.upload_button_create);
        selectedCurrencyText = findViewById(R.id.selected_currency);
        create_button = findViewById(R.id.manager_create_button);

        MobileAds.initialize(this, initializationStatus -> Log.d(LOG_TAG, "Mobile Ads SDK initialized."));
        nativeAdViewTop = findViewById(R.id.native_ad_view_top);
        loadNativeAd("ca-app-pub-3940256099942544/2247696110", nativeAdViewTop);

        selectedCurrencyText.setOnClickListener(v -> showCurrencyBottomSheet());
        uploadButton.setOnClickListener(this);
        create_button.setOnClickListener(this);

        firebaseAuth = FirebaseAuth.getInstance();

        if (UserApi.getInstance() != null) {
            currentUserId = UserApi.getInstance().getUserId();
            currentUserName = UserApi.getInstance().getUsername();
            Log.d(LOG_TAG, "UserApi initialized: userId=" + currentUserId + ", username=" + currentUserName);
        }
    }

    private void showCurrencyBottomSheet() {
        List<String> currencies = Arrays.stream(CurrencyEnum.values())
                .map(CurrencyEnum::getDescription)
                .collect(Collectors.toList());

        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_currency, null);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(sheetView);

        ListView currencyList = sheetView.findViewById(R.id.currency_list);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_currency_dark, currencies);
        currencyList.setAdapter(adapter);

        currencyList.setOnItemClickListener((parent, view, position, id) -> {
            selectedCurrencyText.setText(currencies.get(position));
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }


    private void loadNativeAd(String adUnitId, NativeAdView nativeAdView) {
        AdLoader adLoader = new AdLoader.Builder(this, adUnitId)
                .forNativeAd(ad -> {
                    if (isDestroyed()) {
                        ad.destroy();
                        return;
                    }
                    nativeAdTop = ad;
                    populateNativeAdView(ad, nativeAdView);
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
        TextView headlineView = nativeAdView.findViewById(R.id.ad_headline_top);
        nativeAdView.setHeadlineView(headlineView);

        if (nativeAd.getHeadline() != null) {
            headlineView.setText(nativeAd.getHeadline());
            headlineView.setVisibility(View.VISIBLE);
        } else {
            headlineView.setVisibility(View.GONE);
        }

        nativeAdView.setNativeAd(nativeAd);
    }

    @Override
    public void onClick(View v) {
        String permissionToRequest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? android.Manifest.permission.READ_MEDIA_IMAGES
                : android.Manifest.permission.READ_EXTERNAL_STORAGE;

        if (v.getId() == R.id.manager_create_button) {
            saveManager();
        } else if (v.getId() == R.id.upload_button_create) {
            if (ContextCompat.checkSelfPermission(this, permissionToRequest)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{permissionToRequest},
                        STORAGE_PERMISSION_REQUEST_CODE);
            } else {
                openFileChooser();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFileChooser();
            } else {
                Toast.makeText(this, "Permission denied. Cannot open gallery.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.i(LOG_TAG, "CreateManagerActivity started.");
        currentUser = firebaseAuth.getCurrentUser();
        firebaseAuth.addAuthStateListener(authStateListener);

        collectionReference.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Log.d(LOG_TAG, "Managers data retrieved successfully.");
                        List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                        List<Manager> managers = new ArrayList<>();
                        for (DocumentSnapshot doc : docs) {
                            Manager manager = doc.toObject(Manager.class);
                            if (manager != null) {
                                managers.add(manager);
                            }
                        }
                        findMaxId(managers);

                        for (DocumentSnapshot ds : docs) {
                            Manager manager = ds.toObject(Manager.class);
                            if (manager != null && manager.getId() == 0) {
                                manager.setId(maxId + 1);
                                collectionReference.document(ds.getId()).update("id", manager.getId());
                                maxId++;
                                Log.d(LOG_TAG, "Assigned new ID to manager: " + manager.getId());
                            }
                        }
                    } else {
                        maxId = 0;
                        Log.w(LOG_TAG, "No managers data found. Starting from ID 1.");
                    }
                });
    }

    private void findMaxId(List<Manager> managers) {
        maxId = managers.isEmpty() ? 0 : managers.get(0).getId();
        for (Manager mng : managers) {
            if (mng.getId() > maxId) {
                maxId = mng.getId();
            }
        }
        Log.d(LOG_TAG, "Max ID found: " + maxId);
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void saveManager() {
        String firstName = firstNameText.getText().toString().trim();
        String lastName = lastNameText.getText().toString().trim();
        String nationality = nationalityText.getText().toString().trim();
        String team = teamText.getText().toString().trim();
        String currencyDescription = selectedCurrencyText.getText().toString().trim();
        String currency = currencyDescription.split(" - ")[0];

        boolean isValid = true;

        TextInputLayout firstNameLayout = (TextInputLayout) firstNameText.getParent().getParent();
        TextInputLayout lastNameLayout = (TextInputLayout) lastNameText.getParent().getParent();
        TextInputLayout nationalityLayout = (TextInputLayout) nationalityText.getParent().getParent();
        TextInputLayout teamLayout = (TextInputLayout) teamText.getParent().getParent();

        firstNameLayout.setError(null);
        lastNameLayout.setError(null);
        nationalityLayout.setError(null);
        teamLayout.setError(null);

        if (TextUtils.isEmpty(firstName)) {
            firstNameLayout.setError("Required");
            isValid = false;
        }
        if (TextUtils.isEmpty(lastName)) {
            lastNameLayout.setError("Required");
            isValid = false;
        }
        if (TextUtils.isEmpty(nationality)) {
            nationalityLayout.setError("Required");
            isValid = false;
        }
        if (TextUtils.isEmpty(team)) {
            teamLayout.setError("Required");
            isValid = false;
        }
        if (TextUtils.isEmpty(currency) || currency.equals("Choose currency")) {
            selectedCurrencyText.setError("Required");
            isValid = false;
        } else {
            selectedCurrencyText.setError(null);
        }

        if (!isValid) {
            progressBar.setVisibility(View.INVISIBLE);
            return;
        }


        progressBar.setVisibility(View.VISIBLE);

        StorageReference filepath = storageReference.child("team_badges")
                .child(team + "_" + Timestamp.now().getSeconds());

        Manager manager = new Manager();
        if (teamBadgeUri != null) {
            filepath.putFile(teamBadgeUri)
                    .addOnSuccessListener(taskSnapshot -> filepath.getDownloadUrl()
                            .addOnSuccessListener(uri -> manager.setTeamBadgeUrl(uri.toString())));
        }

        manager.setId(maxId + 1);
        manager.setFirstName(firstName);
        manager.setLastName(lastName);
        manager.setFullName(firstName + " " + lastName);
        manager.setNationality(nationality);
        manager.setTeam(team);
        manager.setCurrency(currency);
        manager.setTimeAdded(new Timestamp(new Date()));
        manager.setUserId(currentUserId);

        collectionReference.add(manager)
                .addOnSuccessListener(documentReference -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    Intent intent = new Intent(CreateManagerActivity.this, ManageTeamActivity.class);
                    intent.putExtra("team", team);
                    intent.putExtra("managerId", manager.getId());
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    Log.e(LOG_TAG, "Failed to save manager: " + e.getMessage(), e);
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            teamBadgeUri = data.getData();
            badgeImage.setImageURI(teamBadgeUri);
            Picasso.get().load(teamBadgeUri).into(badgeImage);
        }
    }

    @Override
    protected void onDestroy() {
        if (nativeAdTop != null) nativeAdTop.destroy();
        Log.i(LOG_TAG, "CreateManagerActivity destroyed.");
        super.onDestroy();
    }
}