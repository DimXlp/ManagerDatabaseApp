package com.dimxlp.managerdb;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import enumeration.CurrencyEnum;
import model.Manager;
import util.UserApi;

public class ProfileActivity extends AppCompatActivity {

    private static final String LOG_TAG = "RAFI|Profile";
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navView;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("Managers");
    private StorageReference storageReference;

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView badgeImage;
    private TextView fullName;
    private TextView teamText;
    private TextView nationality;
    private Button editButton;

    private Uri imageUri;

    private EditText firstNameEdit;
    private EditText lastNameEdit;
    private EditText teamEdit;
    private EditText nationalityEdit;
    private ImageView badgeImageEdit;
    private Button removeBadgeEdit;
    private Button uploadBadgeEdit;
    private Spinner currencySpinnerEdit;
    private Button saveManagerButton;
    private Uri teamBadgeUriEdit;
    private LinearLayout selectManagerButton;
    private AlertDialog.Builder builder;
    private AlertDialog dialog;

    // Make sure to be using androidx.appcompat.app.ActionBarDrawerToggle version.
    private ActionBarDrawerToggle drawerToggle;
    private boolean ftPlayersExist;
    private boolean ytPlayersExist;
    private boolean shPlayersExist;
    private long managerId;
    private String team;

    private TextView managerNameHeader;
    private TextView teamHeader;
    private NativeAd nativeAdTop, nativeAdBottom;
    private NativeAdView nativeAdViewTop, nativeAdViewBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Log.i(LOG_TAG, "ProfileActivity launched.");

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        storageReference = FirebaseStorage.getInstance().getReference();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            managerId = extras.getLong("managerId");
            team = extras.getString("team");
            Log.d(LOG_TAG, "Extras received: managerId = " + managerId + ", team = " + team);
        } else {
            Log.w(LOG_TAG, "No extras received in intent.");
        }

        // Load Native Ads
        nativeAdViewTop = findViewById(R.id.native_ad_view_top);
        nativeAdViewBottom = findViewById(R.id.native_ad_view_bottom);

        loadNativeAd("ca-app-pub-3940256099942544/2247696110", nativeAdViewTop);
        loadNativeAd("ca-app-pub-3940256099942544/2247696110", nativeAdViewBottom);

        // Load Interstitial Ad
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        interstitialAd.show(ProfileActivity.this);
                        Log.d(LOG_TAG, "Interstitial ad loaded and displayed.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(LOG_TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
                    }
                });

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_menu);

        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nvView);
        setUpDrawerContent(navView);

        View headerLayout = null;
        if (navView.getHeaderCount() > 0) {
            headerLayout = navView.getHeaderView(0);
        }
        managerNameHeader = headerLayout.findViewById(R.id.manager_name_header);
        teamHeader = headerLayout.findViewById(R.id.team_name_header);

        badgeImage = findViewById(R.id.manager_photo_profile);
        fullName = findViewById(R.id.full_name_profile);
        teamText = findViewById(R.id.team_profile);
        nationality = findViewById(R.id.nationality_profile);
        editButton = findViewById(R.id.edit_button_profile);
        Log.d(LOG_TAG, "Profile fields initialized.");

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Edit Profile button clicked.");
                createPopupDialog();
            }
        });

        selectManagerButton = findViewById(R.id.select_manager_section);
        selectManagerButton.setOnClickListener(v -> {
            Log.d(LOG_TAG, "Select Manager Button clicked.");
            Intent intent = new Intent(ProfileActivity.this, SelectManagerActivity.class);
            startActivity(intent);
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
        int headlineId = nativeAdView == nativeAdViewTop ? R.id.ad_headline_top : R.id.ad_headline_bottom;
        nativeAdView.setHeadlineView(nativeAdView.findViewById(headlineId));
        TextView headlineView = (TextView) nativeAdView.getHeadlineView();

        if (nativeAd.getHeadline() != null) {
            headlineView.setText(nativeAd.getHeadline());
            headlineView.setVisibility(View.VISIBLE);
        } else {
            headlineView.setVisibility(View.GONE);
        }

        // Remove body and CTA for compact layout
        nativeAdView.setBodyView(null);
        nativeAdView.setCallToActionView(null);

        nativeAdView.setNativeAd(nativeAd);
    }

    private void createPopupDialog() {
        Log.d(LOG_TAG, "Creating popup dialog to edit manager profile.");
        builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.edit_manager_popup, null);

        firstNameEdit = view.findViewById(R.id.first_name_edit);
        lastNameEdit = view.findViewById(R.id.last_name_edit);
        teamEdit = view.findViewById(R.id.team_edit);
        nationalityEdit = view.findViewById(R.id.nationality_edit);
        badgeImageEdit = view.findViewById(R.id.team_badge_image_edit);
        uploadBadgeEdit = view.findViewById(R.id.upload_button_edit);
        currencySpinnerEdit = view.findViewById(R.id.currency_spinner_edit);
        saveManagerButton = view.findViewById(R.id.save_manager_button);

        teamEdit.setEnabled(false);
        teamEdit.setTextColor(Color.GRAY);

        List<String> currencies = Arrays.stream(CurrencyEnum.values())
                .map(CurrencyEnum::getSymbol)
                .collect(Collectors.toList());

        final ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(ProfileActivity.this, android.R.layout.simple_spinner_item, currencies);
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinnerEdit.setAdapter(currencyAdapter);

        Log.d(LOG_TAG, "Fetching manager data for popup dialog.");
        collectionReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("id", managerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            List<Manager> managerList = new ArrayList<>();
                            for (QueryDocumentSnapshot doc: queryDocumentSnapshots) {
                                Manager manager = doc.toObject(Manager.class);
                                managerList.add(manager);
                            }
                            Manager theManager = managerList.get(0);
                            Log.d(LOG_TAG, "Manager data fetched: " + theManager.getFullName());

                            firstNameEdit.setText(theManager.getFirstName());
                            lastNameEdit.setText(theManager.getLastName());
                            teamEdit.setText(theManager.getTeam());
                            nationalityEdit.setText(theManager.getNationality());
                            String imageUrlEdit = theManager.getTeamBadgeUrl();
                            Picasso.get().load(imageUrlEdit).into(badgeImageEdit);
                            currencySpinnerEdit.setSelection(currencyAdapter.getPosition(theManager.getCurrency()));
                            Log.d(LOG_TAG, "Popup dialog fields populated with manager data.");
                        } else {
                            Log.w(LOG_TAG, "No manager data found for userId=" + UserApi.getInstance().getUserId());
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error fetching manager data.", e));

        uploadBadgeEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });

        saveManagerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!firstNameEdit.getText().toString().isEmpty() &&
                    !lastNameEdit.getText().toString().isEmpty() &&
                    !teamEdit.getText().toString().isEmpty() &&
                    !nationalityEdit.getText().toString().isEmpty() &&
                    !currencySpinnerEdit.getSelectedItem().toString().isEmpty()) {
                    Log.d(LOG_TAG, "Validation successful. Saving manager data.");
                    saveManager();
                } else {
                    Log.w(LOG_TAG, "Validation failed: One or more fields are empty.");
                    Toast.makeText(ProfileActivity.this, "First Name, Last Name, Team, Nationality and Currency are required!", Toast.LENGTH_LONG)
                            .show();
                }
            }
        });

        builder.setView(view);
        dialog = builder.create();
        dialog.show();
    }

    private void openFileChooser() {
        Log.d(LOG_TAG, "Opening file chooser for badge image selection.");
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            teamBadgeUriEdit = data.getData();
            badgeImage.setImageURI(teamBadgeUriEdit);
            Picasso.get().load(teamBadgeUriEdit).into(badgeImageEdit);
            Log.d(LOG_TAG, "Badge image updated in popup dialog.");
        } else {
            Log.w(LOG_TAG, "Image selection canceled or failed.");
        }
    }

    private void saveManager() {
        Log.d(LOG_TAG, "Saving manager updates.");

        collectionReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("id", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull final Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                            Log.d(LOG_TAG, "Manager data found. Preparing to update.");
                            final StorageReference filepath = storageReference
                                    .child("team_badges")
                                    .child(team + "_" + Timestamp.now().getSeconds());

                            List<DocumentSnapshot> doc =  Objects.requireNonNull(task.getResult()).getDocuments();
                            DocumentReference documentReference = collectionReference.document(doc.get(0).getId());

                            if (teamBadgeUriEdit != null) {
                                filepath.putFile(teamBadgeUriEdit)
                                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                            @Override
                                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                    @Override
                                                    public void onSuccess(Uri uri) {
                                                        String imageUrl = uri.toString();
                                                        documentReference.update("teamBadgeUrl", imageUrl);
                                                        Log.d(LOG_TAG, "Badge image updated successfully: " + imageUrl);
                                                    }
                                                });
                                            }
                                        })
                                        .addOnFailureListener(e -> Log.e(LOG_TAG, "Error uploading badge image.", e));
                            }

                            documentReference.update("firstName", firstNameEdit.getText().toString().trim(),
                                            "lastName", lastNameEdit.getText().toString().trim(),
                                            "fullName", firstNameEdit.getText().toString().trim() + " " + lastNameEdit.getText().toString().trim(),
                                            "team", teamEdit.getText().toString().trim(),
                                            "nationality", nationalityEdit.getText().toString().trim(),
                                            "currency", currencySpinnerEdit.getSelectedItem().toString().trim())
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d(LOG_TAG, "Manager details updated successfully.");
                                            Intent intent = new Intent(ProfileActivity.this, ProfileActivity.class);
                                            intent.putExtra("managerId", managerId);
                                            intent.putExtra("team", team);
                                            startActivity(intent);
                                            finish();
                                            Toast.makeText(ProfileActivity.this, "Manager updated!", Toast.LENGTH_LONG)
                                                    .show();
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.e(LOG_TAG, "Error updating manager details.", e));
                        } else {
                            Log.w(LOG_TAG, "No manager data found for update.");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error fetching manager data.", e));


        dialog.dismiss();

        String fullNameText = firstNameEdit.getText().toString().trim() + " " + lastNameEdit.getText().toString().trim();
        fullName.setText(fullNameText);
        teamText.setText(teamEdit.getText().toString().trim());
        nationality.setText(nationalityEdit.getText().toString().trim());
        Log.d(LOG_TAG, "UI updated with new manager details.");
    }

    private void setUpDrawerContent(NavigationView navView) {
        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                selectDrawerItem(item);
                return false;
            }
        });
    }

    private void selectDrawerItem(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.dr_home:
                Intent homeIntent = new Intent(ProfileActivity.this, ManageTeamActivity.class);
                homeIntent.putExtra("managerId", managerId);
                homeIntent.putExtra("team", team);
                startActivity(homeIntent);
                finish();
                break;
            case R.id.dr_manager_selection:
                Intent managerSelectionIntent = new Intent(ProfileActivity.this, SelectManagerActivity.class);
                startActivity(managerSelectionIntent);
                break;
            case R.id.dr_profile:
                break;
            case R.id.dr_first_team:
                if (ftPlayersExist) {
                    Intent firstIntent = new Intent(ProfileActivity.this, FirstTeamListActivity.class);
                    firstIntent.putExtra("managerId", managerId);
                    firstIntent.putExtra("team", team);
                    startActivity(firstIntent);
                    finish();
                } else {
                    Intent firstIntent = new Intent(ProfileActivity.this, FirstTeamActivity.class);
                    firstIntent.putExtra("managerId", managerId);
                    firstIntent.putExtra("team", team);
                    startActivity(firstIntent);
                    finish();
                }
                break;
            case R.id.dr_youth_team:
                if (ytPlayersExist) {
                    Intent youthIntent = new Intent(ProfileActivity.this, YouthTeamListActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", team);
                    startActivity(youthIntent);
                    finish();
                } else {
                    Intent youthIntent = new Intent(ProfileActivity.this, YouthTeamActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", team);
                    startActivity(youthIntent);
                    finish();
                }
                break;
            case R.id.dr_former_players:
                Intent formerIntent = new Intent(ProfileActivity.this, FormerPlayersListActivity.class);
                formerIntent.putExtra("managerId", managerId);
                formerIntent.putExtra("team", team);
                startActivity(formerIntent);
                finish();
                break;
            case R.id.dr_shortlist:
                if (shPlayersExist) {
                    Intent shortlistIntent = new Intent(ProfileActivity.this, ShortlistPlayersActivity.class);
                    shortlistIntent.putExtra("managerId", managerId);
                    shortlistIntent.putExtra("team", team);
                    startActivity(shortlistIntent);
                    finish();
                } else {
                    Intent shortlistIntent = new Intent(ProfileActivity.this, ShortlistActivity.class);
                    shortlistIntent.putExtra("managerId", managerId);
                    shortlistIntent.putExtra("team", team);
                    startActivity(shortlistIntent);
                    finish();
                }
                break;
            case R.id.dr_loaned_out_players:
                Intent loanIntent = new Intent(ProfileActivity.this, LoanedOutPlayersActivity.class);
                loanIntent.putExtra("managerId", managerId);
                loanIntent.putExtra("team", team);
                startActivity(loanIntent);
                finish();
                break;
            case R.id.dr_transfer_deals:
                Intent transferIntent = new Intent(ProfileActivity.this, TransferDealsActivity.class);
                transferIntent.putExtra("managerId", managerId);
                transferIntent.putExtra("team", team);
                startActivity(transferIntent);
                finish();
                break;
            case R.id.dr_support_info:
                Intent supportIntent = new Intent(ProfileActivity.this, SupportActivity.class);
                supportIntent.putExtra("managerId", managerId);
                supportIntent.putExtra("team", team);
                startActivity(supportIntent);
                finish();
                break;
            case R.id.dr_logout:
                if (user != null && firebaseAuth != null) {
                    firebaseAuth.signOut();
                    startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                    finishAffinity();
                } else {
                    Log.w(LOG_TAG, "Logout attempt failed: currentUser or firebaseAuth is null.");
                }
                break;
            default:
                Log.w(LOG_TAG, "Unhandled drawer item selected: " + item.getTitle());
                break;
        }

        item.setChecked(true);
        setTitle(item.getTitle());
        drawerLayout.closeDrawers();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "onStart called: Fetching manager and player data.");

        db.collection("ShortlistedPlayers").whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            shPlayersExist = Objects.requireNonNull(task.getResult()).size() > 0;
                            Log.d(LOG_TAG, "Shortlisted players exist: " + shPlayersExist);
                        } else {
                            Log.e(LOG_TAG, "Error fetching Shortlisted Players.", task.getException());
                        }
                    }
                });

        collectionReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("id", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<DocumentSnapshot> documentList = task.getResult().getDocuments();
                            List<Manager> managerList = new ArrayList<>();
                            for (DocumentSnapshot doc: documentList) {
                                Manager manager = doc.toObject(Manager.class);
                                managerList.add(manager);
                            }
                            Manager theManager = managerList.get(0);
                            Log.d(LOG_TAG, "Manager data fetched: " + theManager.getFullName());
                            fullName.setText(theManager.getFullName());
                            teamText.setText(theManager.getTeam());
                            nationality.setText(theManager.getNationality());
                            String imageUrl = theManager.getTeamBadgeUrl();
                            Picasso.get().load(imageUrl).into(badgeImage);
                            Log.d(LOG_TAG, "UI updated with manager details.");
                        } else {
                            Log.e(LOG_TAG, "Error fetching manager data.", task.getException());
                        }
                    }
                });

        db.collection("FirstTeamPlayers").whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            ftPlayersExist = Objects.requireNonNull(task.getResult()).size() > 0;
                            Log.d(LOG_TAG, "First Team players exist: " + ftPlayersExist);
                        } else {
                            Log.e(LOG_TAG, "Error fetching First Team players.", task.getException());
                        }

                        db.collection("YouthTeamPlayers").whereEqualTo("userId", UserApi.getInstance().getUserId())
                                .whereEqualTo("managerId", managerId)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            ytPlayersExist = Objects.requireNonNull(task.getResult()).size() > 0;
                                            Log.d(LOG_TAG, "Youth Team players exist: " + ytPlayersExist);
                                        } else {
                                            Log.e(LOG_TAG, "Error fetching Youth Team players.", task.getException());
                                        }
                                    }
                                });
                    }
                });

        collectionReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("id", managerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            List<Manager> managerList = new ArrayList<>();
                            for (QueryDocumentSnapshot doc: queryDocumentSnapshots) {
                                Manager manager = doc.toObject(Manager.class);
                                managerList.add(manager);
                                Log.d(LOG_TAG, "Header Manager data fetched: " + manager.getFullName());
                            }
                            Manager theManager = managerList.get(0);
                            managerNameHeader.setText(theManager.getFullName());
                            teamHeader.setText(theManager.getTeam());
                            Log.d(LOG_TAG, "Header UI updated with manager details.");
                        } else {
                            Log.w(LOG_TAG, "No manager data found for header.");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error fetching header manager data.", e));
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "onDestroy called: Cleaning up resources.");
        if (nativeAdTop != null) nativeAdTop.destroy();
        if (nativeAdBottom != null) nativeAdBottom.destroy();
        super.onDestroy();
    }
}