package com.dimxlp.managerdb;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import model.FirstTeamPlayer;
import model.Manager;
import util.NationalityFlagUtil;
import util.UserApi;

public class FirstTeamActivity extends AppCompatActivity {

    private static final String LOG_TAG = "RAFI|FirstTeam";
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navView;

    private FloatingActionButton addPlayerFab;
    private TextView addPlayerText;

    private AlertDialog.Builder builder;
    private AlertDialog dialog;

    private EditText firstName;
    private EditText lastName;
    private TextView positionPicker;
    private EditText number;
    private AutoCompleteTextView nationality;
    private EditText overall;
    private EditText potentialLow;
    private EditText potentialHigh;
    private TextView yearSigned;
    private TextView yearScouted;
    private SwitchMaterial loanSwitch;
    private Button createPlayerButton;

    private String currentUserId;
    private String currentUserName;

    private int playerId = 1;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUser;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("FirstTeamPlayers");
    private boolean ytPlayersExist;
    private boolean shPlayersExist;
    private long managerId;
    private String team;

    private TextView managerNameHeader;
    private TextView teamHeader;
    private NativeAd nativeAdTop, nativeAdBottom;
    private NativeAdView nativeAdViewTop, nativeAdViewBottom;

    private BottomSheetDialog createDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_team);
        Log.i(LOG_TAG, "FirstTeamActivity launched.");

        if (UserApi.getInstance() != null) {
            currentUserId = UserApi.getInstance().getUserId();
            currentUserName = UserApi.getInstance().getUsername();
            Log.d(LOG_TAG, "UserApi initialized: userId=" + currentUserId + ", username=" + currentUserName);
        } else {
            Log.w(LOG_TAG, "UserApi instance is null.");
        }

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(LOG_TAG, "Current user: " + currentUser.getUid());
        } else {
            Log.w(LOG_TAG, "No user is currently authenticated.");
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            managerId = extras.getLong("managerId");
            team = extras.getString("team");
            Log.d(LOG_TAG, "Extras received: managerId=" + managerId + ", team=" + team);
        } else {
            Log.w(LOG_TAG, "No extras received in intent.");
        }

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_menu);

        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nvView);
        setUpDrawerContent(navView);

        addPlayerFab = findViewById(R.id.add_player_button);

        View headerLayout = null;
        if (navView.getHeaderCount() > 0) {
            headerLayout = navView.getHeaderView(0);
        }
        managerNameHeader = headerLayout.findViewById(R.id.manager_name_header);
        teamHeader = headerLayout.findViewById(R.id.team_name_header);

        addPlayerFab.setOnClickListener(v -> {
            Log.d(LOG_TAG, "Add player button clicked.");
            createPopupDialog();
        });

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> Log.d(LOG_TAG, "Mobile Ads SDK initialized."));

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
                        interstitialAd.show(FirstTeamActivity.this);
                        Log.d(LOG_TAG, "Interstitial ad loaded and displayed.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(LOG_TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
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
        Log.d(LOG_TAG, "Creating popup dialog for adding a new player.");
        createDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.create_first_team_player_popup, null);
        createDialog.setContentView(view);

        firstName = view.findViewById(R.id.first_name_ftp_create);
        lastName = view.findViewById(R.id.last_name_ftp_create);
        positionPicker = view.findViewById(R.id.position_picker_ftp_create);
        number = view.findViewById(R.id.number_ftp_create);
        nationality = view.findViewById(R.id.nationality_ftp_create);
        overall = view.findViewById(R.id.overall_ftp_create);
        potentialLow = view.findViewById(R.id.potential_low_ftp_create);
        potentialHigh = view.findViewById(R.id.potential_high_ftp_create);
        yearSigned = view.findViewById(R.id.year_signed_picker_ftp_create);
        yearScouted = view.findViewById(R.id.year_scouted_picker_ftp_create);
        loanSwitch = view.findViewById(R.id.loan_player_switch_ftp_create);
        createPlayerButton = view.findViewById(R.id.create_ft_player_button);

        String[] countrySuggestions = getResources().getStringArray(R.array.nationalities);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, countrySuggestions);

        nationality.setAdapter(adapter);

        nationality.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) nationality.showDropDown();
        });

        String[] positions = this.getResources().getStringArray(R.array.position_array);
        String[] years = this.getResources().getStringArray(R.array.years_array);

        positionPicker.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Select Position")
                    .setItems(positions, (pickerDialog, which) -> positionPicker.setText(positions[which]))
                    .show();
        });

        yearSigned.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Select Year Signed")
                    .setItems(years, (pickerDialog, which) -> yearSigned.setText(years[which]))
                    .show();
        });

        yearScouted.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Select Year Scouted")
                    .setItems(years, (pickerDialog, which) -> yearScouted.setText(years[which]))
                    .show();
        });

        createPlayerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Create player button clicked in popup dialog.");
                if (!lastName.getText().toString().isEmpty() &&
                    !nationality.getText().toString().isEmpty() &&
                    !positionPicker.getText().toString().isEmpty() &&
                    !overall.getText().toString().isEmpty() &&
                    !yearSigned.getText().toString().isEmpty()) {
                    createPlayer();
                } else {
                    Log.w(LOG_TAG, "Validation failed: Required fields are missing.");
                    Toast.makeText(FirstTeamActivity.this, "Last Name/Nickname, Nationality, Position, Overall and Year Signed are required", Toast.LENGTH_LONG)
                            .show();
                }
            }
        });

        createDialog.show();
    }

    private void createPlayer() {
        Log.d(LOG_TAG, "Creating a new player.");
        String firstNamePlayer = firstName.getText().toString().trim();
        String lastNamePlayer = lastName.getText().toString().trim();
        String fullNamePlayer;
        if (!firstNamePlayer.isEmpty()) {
            fullNamePlayer = firstNamePlayer + " " + lastNamePlayer;
        } else {
            fullNamePlayer = lastNamePlayer;
        }
        String positionPlayer = positionPicker.getText().toString().trim();
        String numberPlayer = number.getText().toString().trim();
        String nationalityPlayer = nationality.getText().toString().trim();
        Map<String, String> variantMap = NationalityFlagUtil.getVariantToStandardMap();
        String nationalityInput = variantMap.getOrDefault(nationalityPlayer, nationalityPlayer);

        String overallPlayer = overall.getText().toString().trim();
        String potentialLowPlayer = potentialLow.getText().toString().trim();
        String potentialHiPlayer = potentialHigh.getText().toString().trim();
        final String ySignedPlayer = yearSigned.getText().toString().trim();
        String yScoutedPlayer = yearScouted.getText().toString().trim();

        FirstTeamPlayer player = new FirstTeamPlayer();

        player.setId(1);
        player.setFirstName(firstNamePlayer);
        player.setLastName(lastNamePlayer);
        player.setFullName(fullNamePlayer);
        player.setPosition(positionPlayer);
        if (!numberPlayer.isEmpty()) {
            player.setNumber(Integer.parseInt(numberPlayer));
        } else {
            player.setNumber(99);
        }
        player.setTeam(team);
        player.setNationality(nationalityInput);
        player.setOverall(Integer.parseInt(overallPlayer));
        if (!potentialLowPlayer.isEmpty()) {
            player.setPotentialLow(Integer.parseInt(potentialLowPlayer));
        }
        if (!potentialHiPlayer.isEmpty()) {
            player.setPotentialHigh(Integer.parseInt(potentialHiPlayer));
        }
        player.setYearSigned(ySignedPlayer);
        if (!yScoutedPlayer.isEmpty()) {
            player.setYearScouted(yScoutedPlayer);
        }
        player.setUserId(currentUserId);
        player.setTimeAdded(new Timestamp(new Date()));
        player.setManagerId(managerId);
        player.setLoanPlayer(loanSwitch.isChecked());

        Log.d(LOG_TAG, "Player object created: " + player);
        playerId++;

        collectionReference.add(player)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.i(LOG_TAG, "Player successfully added to Firestore. Document ID: " + documentReference.getId());
                        if (createDialog != null && createDialog.isShowing()) {
                            createDialog.dismiss();
                        }
                        Intent intent = new Intent(FirstTeamActivity.this, FirstTeamListActivity.class);
                        intent.putExtra("managerId", managerId);
                        intent.putExtra("team", team);
                        intent.putExtra("barYear", ySignedPlayer);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Failed to add player to Firestore: " + e.getMessage(), e));
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
                Intent homeIntent = new Intent(FirstTeamActivity.this, ManageTeamActivity.class);
                homeIntent.putExtra("managerId", managerId);
                homeIntent.putExtra("team", team);
                startActivity(homeIntent);
                finish();
                break;
            case R.id.dr_manager_selection:
                Intent managerSelectionIntent = new Intent(FirstTeamActivity.this, SelectManagerActivity.class);
                startActivity(managerSelectionIntent);
                break;
            case R.id.dr_profile:
                Intent profileIntent = new Intent(FirstTeamActivity.this, ProfileActivity.class);
                profileIntent.putExtra("managerId", managerId);
                profileIntent.putExtra("team", team);
                startActivity(profileIntent);
                finish();
                break;
            case R.id.dr_first_team:
                break;
            case R.id.dr_youth_team:
                if (ytPlayersExist) {
                    Intent youthIntent = new Intent(FirstTeamActivity.this, YouthTeamListActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", team);
                    startActivity(youthIntent);
                    finish();
                } else {
                    Intent youthIntent = new Intent(FirstTeamActivity.this, YouthTeamActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", team);
                    startActivity(youthIntent);
                    finish();
                }
                break;
            case R.id.dr_former_players:
                Intent formerIntent = new Intent(FirstTeamActivity.this, FormerPlayersListActivity.class);
                formerIntent.putExtra("managerId", managerId);
                formerIntent.putExtra("team", team);
                startActivity(formerIntent);
                finish();
                break;
            case R.id.dr_shortlist:
                if (shPlayersExist) {
                    Intent shortlistIntent = new Intent(FirstTeamActivity.this, ShortlistPlayersActivity.class);
                    shortlistIntent.putExtra("managerId", managerId);
                    shortlistIntent.putExtra("team", team);
                    startActivity(shortlistIntent);
                    finish();
                } else {
                    Intent shortlistIntent = new Intent(FirstTeamActivity.this, ShortlistActivity.class);
                    shortlistIntent.putExtra("managerId", managerId);
                    shortlistIntent.putExtra("team", team);
                    startActivity(shortlistIntent);
                    finish();
                }
                break;
            case R.id.dr_loaned_out_players:
                Intent loanIntent = new Intent(FirstTeamActivity.this, LoanedOutPlayersActivity.class);
                loanIntent.putExtra("managerId", managerId);
                loanIntent.putExtra("team", team);
                startActivity(loanIntent);
                finish();
                break;
            case R.id.dr_transfer_deals:
                Intent transferIntent = new Intent(FirstTeamActivity.this, TransferDealsActivity.class);
                transferIntent.putExtra("managerId", managerId);
                transferIntent.putExtra("team", team);
                startActivity(transferIntent);
                finish();
                break;
            case R.id.dr_support_info:
                Intent supportIntent = new Intent(FirstTeamActivity.this, SupportActivity.class);
                supportIntent.putExtra("managerId", managerId);
                supportIntent.putExtra("team", team);
                startActivity(supportIntent);
                finish();
                break;
            case R.id.dr_logout:
                if (currentUser != null && firebaseAuth != null) {
                    firebaseAuth.signOut();
                    Log.i(LOG_TAG, "User logged out successfully.");
                    startActivity(new Intent(FirstTeamActivity.this, MainActivity.class));
                    finishAffinity();
                } else {
                    Log.w(LOG_TAG, "Logout attempt failed: currentUser or firebaseAuth is null.");
                }
                break;
            default:
                Log.w(LOG_TAG, "Unhandled drawer item selected: " + item.getTitle());
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
        Log.d(LOG_TAG, "FirstTeamActivity started.");

        db.collection("YouthTeamPlayers").whereEqualTo("userId", currentUserId)
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (Objects.requireNonNull(task.getResult()).size() > 0) {
                                ytPlayersExist = true;
                            } else {
                                ytPlayersExist = false;
                            }
                        }
                    }
                });

        db.collection("ShortlistedPlayers").whereEqualTo("userId", currentUserId)
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (Objects.requireNonNull(task.getResult()).size() > 0) {
                                shPlayersExist = true;
                            } else {
                                shPlayersExist = false;
                            }
                        }
                    }
                });

        db.collection("Managers").whereEqualTo("userId", currentUserId)
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
                            managerNameHeader.setText(theManager.getFullName());
                            teamHeader.setText(theManager.getTeam());
                        }
                    }
                });
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
        Log.d(LOG_TAG, "FirstTeamActivity destroyed.");
    }

}