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
import com.google.android.material.textfield.TextInputLayout;
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

import model.Manager;
import model.ShortlistedPlayer;
import util.NationalityFlagUtil;
import util.UserApi;
import util.ValueFormatter;

public class ShortlistActivity extends AppCompatActivity {

    private static final String LOG_TAG = "RAFI|Shortlist";
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navView;

    private AlertDialog.Builder builder;
    private AlertDialog dialog;

    private FloatingActionButton addPlayerFab;
    private TextView addPlayerText;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference shortlistColRef = db.collection("ShortlistedPlayers");
    private CollectionReference ftPlayersColRef = db.collection("FirstTeamPlayers");
    private CollectionReference ytPlayersColRef = db.collection("YouthTeamPlayers");
    private CollectionReference managersColRef = db.collection("Managers");
    private boolean ftPlayersExist;
    private boolean ytPlayersExist;
    private long managerId;
    private String myTeam;

    private EditText firstName;
    private EditText lastName;
    private TextView positionPicker;
    private EditText team;
    private AutoCompleteTextView nationality;
    private EditText overall;
    private EditText potLow;
    private EditText potHigh;
    private TextInputLayout valueTil;
    private EditText value;
    private TextInputLayout wageTil;
    private EditText wage;
    private EditText comments;
    private Button createButton;
    private String currency;

    private TextView managerNameHeader;
    private TextView teamHeader;
    private String barPosition;
    private NativeAd nativeAdTop, nativeAdBottom;
    private NativeAdView nativeAdViewTop, nativeAdViewBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shortlist);
        Log.i(LOG_TAG, "ShortlistActivity launched.");

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        if (user != null) {
            Log.d(LOG_TAG, "User authenticated: " + user.getUid());
        } else {
            Log.w(LOG_TAG, "No authenticated user.");
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            managerId = extras.getLong("managerId");
            myTeam = extras.getString("team");
            Log.d(LOG_TAG, "Extras received: managerId = " + managerId + ", team = " + myTeam);
        } else {
            Log.w(LOG_TAG, "No extras received in intent.");
        }


        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_menu);

        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nvView_sh);
        setUpDrawerContent(navView);

        View headerLayout = null;
        if (navView.getHeaderCount() > 0) {
            headerLayout = navView.getHeaderView(0);
        }
        managerNameHeader = headerLayout.findViewById(R.id.manager_name_header);
        teamHeader = headerLayout.findViewById(R.id.team_name_header);

        addPlayerFab = findViewById(R.id.add_player_button_sh);
        addPlayerText = findViewById(R.id.add_player_text_sh);

        addPlayerFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Add player FAB clicked.");
                createPopupDialog();
            }
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
                        interstitialAd.show(ShortlistActivity.this);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {

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
        Log.d(LOG_TAG, "Creating popup dialog for adding a shortlisted player.");
        BottomSheetDialog createDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.create_shortlisted_player_popup, null);
        createDialog.setContentView(view);

        firstName = view.findViewById(R.id.first_name_shp_create);
        lastName = view.findViewById(R.id.last_name_shp_create);
        positionPicker = view.findViewById(R.id.position_picker_shp_create);
        team = view.findViewById(R.id.team_shp_create);
        nationality = view.findViewById(R.id.nationality_shp_create);
        overall = view.findViewById(R.id.overall_shp_create);
        potLow = view.findViewById(R.id.potential_low_shp_create);
        potHigh = view.findViewById(R.id.potential_high_shp_create);
        valueTil = view.findViewById(R.id.value_til_shp_create);
        value = view.findViewById(R.id.value_shp_create);
        wageTil = view.findViewById(R.id.wage_til_shp_create);
        wage = view.findViewById(R.id.wage_shp_create);
        comments = view.findViewById(R.id.comments_shp_create);
        createButton = view.findViewById(R.id.create_sh_player_button);

        ValueFormatter.formatValue(value);
        ValueFormatter.formatValue(wage);

        Log.d(LOG_TAG, "Fetching manager data for currency.");
        managersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
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
                            currency = managerList.get(0).getCurrency();
                            valueTil.setHint("Value (in " + currency + ")");
                            wageTil.setHint("Wage (in " + currency + ")");
                            Log.d(LOG_TAG, "Currency fetched and hints updated: " + currency);
                        } else {
                            Log.w(LOG_TAG, "No manager data found for currency update.");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error fetching manager data for currency.", e));

        String[] countrySuggestions = getResources().getStringArray(R.array.nationalities);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, countrySuggestions);

        nationality.setAdapter(adapter);

        nationality.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) nationality.showDropDown();
        });

        String[] positions = this.getResources().getStringArray(R.array.position_array);
        positionPicker.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Select Position")
                    .setItems(positions, (pickerDialog, which) -> positionPicker.setText(positions[which]))
                    .show();
        });

        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Create player button clicked.");
                if (!lastName.getText().toString().isEmpty() &&
                        !nationality.getText().toString().isEmpty() &&
                        !positionPicker.getText().toString().isEmpty() &&
                        !team.getText().toString().isEmpty() &&
                        !overall.getText().toString().isEmpty()) {
                    Log.d(LOG_TAG, "Validation successful. Proceeding to create player.");
                    createPlayer();
                } else {
                    Log.w(LOG_TAG, "Validation failed: Required fields are missing.");
                    Toast.makeText(ShortlistActivity.this, "Last Name/Nickname, Nationality, Position, Team and Overall are required", Toast.LENGTH_LONG)
                            .show();
                }
            }
        });

        createDialog.show();
    }

    private void createPlayer() {
        Log.d(LOG_TAG, "Creating a new shortlisted player.");

        String firstNamePlayer = firstName.getText().toString().trim();
        String lastNamePlayer = lastName.getText().toString().trim();
        String fullNamePlayer;
        if (!firstNamePlayer.isEmpty()) {
            fullNamePlayer = firstNamePlayer + " " + lastNamePlayer;
        } else {
            fullNamePlayer = lastNamePlayer;
        }
        final String positionPlayer = positionPicker.getText().toString().trim();
        String nationalityPlayer = nationality.getText().toString().trim();
        Map<String, String> variantMap = NationalityFlagUtil.getVariantToStandardMap();
        String nationalityInput = variantMap.getOrDefault(nationalityPlayer, nationalityPlayer);

        String overallPlayer = overall.getText().toString().trim();
        String potLowPlayer = potLow.getText().toString().trim();
        String potHighPlayer = potHigh.getText().toString().trim();
        String teamPlayer = team.getText().toString().trim();
        String valuePlayer = value.getText().toString().trim().replaceAll(",", "");
        String wagePlayer = wage.getText().toString().trim().replaceAll(",", "");
        String commentsPlayer = comments.getText().toString().trim();
        Log.d(LOG_TAG, "Player details collected: Full Name = " + fullNamePlayer + ", Position = " + positionPlayer);

        final ShortlistedPlayer player = new ShortlistedPlayer();

        player.setId(1);
        player.setFirstName(firstNamePlayer);
        player.setLastName(lastNamePlayer);
        player.setFullName(fullNamePlayer);
        player.setPosition(positionPlayer);
        player.setNationality(nationalityInput);
        player.setOverall(Integer.parseInt(overallPlayer));
        if (!potLowPlayer.isEmpty()) {
            player.setPotentialLow(Integer.parseInt(potLowPlayer));
        }
        if (!potHighPlayer.isEmpty()) {
            player.setPotentialHigh(Integer.parseInt(potHighPlayer));
        }
        player.setTeam(teamPlayer);
        if (!valuePlayer.isEmpty()) {
            player.setValue(Integer.parseInt(valuePlayer));
        }
        if (!wagePlayer.isEmpty()) {
            player.setWage(Integer.parseInt(wagePlayer));
        }
        player.setComments(commentsPlayer);
        player.setManagerId(managerId);
        player.setUserId(UserApi.getInstance().getUserId());
        player.setTimeAdded(new Timestamp(new Date()));
        Log.d(LOG_TAG, "Player object created: " + player);

        shortlistColRef.add(player)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.i(LOG_TAG, "Player successfully added to Firestore: " + documentReference.getId());
                        Intent intent = new Intent(ShortlistActivity.this, ShortlistPlayersActivity.class);
                        intent.putExtra("managerId", managerId);
                        intent.putExtra("team", myTeam);
                        switch (positionPlayer) {
                            case "GK":
                                barPosition = "Goalkeepers";
                                break;
                            case "CB":
                                barPosition = "Center Backs";
                                break;
                            case "RB", "RWB":
                                barPosition = "Right Backs";
                                break;
                            case "LB", "LWB":
                                barPosition = "Left Backs";
                                break;
                            case "CDM":
                                barPosition = "Center Defensive Mids";
                                break;
                            case "CM":
                                barPosition = "Center Midfielders";
                                break;
                            case "CAM":
                                barPosition = "Center Attacking Mids";
                                break;
                            case "RM", "RW":
                                barPosition = "Right Wingers";
                                break;
                            case "LM", "LW":
                                barPosition = "Left Wingers";
                                break;
                            case "ST", "CF", "RF", "LF":
                                barPosition = "Strikers";
                                break;

                        }
                        Log.d(LOG_TAG, "Bar position determined: " + barPosition);
                        intent.putExtra("barPosition", barPosition);
                        startActivity(intent);
                        finish();
                        Log.d(LOG_TAG, "ShortlistPlayersActivity started, and ShortlistActivity finished.");
                    }
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error adding player to Firestore.", e));
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
                Intent homeIntent = new Intent(ShortlistActivity.this, ManageTeamActivity.class);
                homeIntent.putExtra("managerId", managerId);
                homeIntent.putExtra("team", myTeam);
                startActivity(homeIntent);
                finish();
                break;
            case R.id.dr_manager_selection:
                Intent managerSelectionIntent = new Intent(ShortlistActivity.this, SelectManagerActivity.class);
                startActivity(managerSelectionIntent);
                break;
            case R.id.dr_profile:
                Intent profileIntent = new Intent(ShortlistActivity.this, ProfileActivity.class);
                profileIntent.putExtra("managerId", managerId);
                profileIntent.putExtra("team", myTeam);
                startActivity(profileIntent);
                finish();
                break;
            case R.id.dr_first_team:
                if (ftPlayersExist) {
                    Intent firstIntent = new Intent(ShortlistActivity.this, FirstTeamListActivity.class);
                    firstIntent.putExtra("managerId", managerId);
                    firstIntent.putExtra("team", myTeam);
                    startActivity(firstIntent);
                    finish();
                } else {
                    Intent firstIntent = new Intent(ShortlistActivity.this, FirstTeamActivity.class);
                    firstIntent.putExtra("managerId", managerId);
                    firstIntent.putExtra("team", myTeam);
                    startActivity(firstIntent);
                    finish();
                }
                break;
            case R.id.dr_youth_team:
                if (ytPlayersExist) {
                    Intent youthIntent = new Intent(ShortlistActivity.this, YouthTeamListActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", myTeam);
                    startActivity(youthIntent);
                    finish();
                } else {
                    Intent youthIntent = new Intent(ShortlistActivity.this, YouthTeamActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", myTeam);
                    startActivity(youthIntent);
                    finish();
                }
                break;
            case R.id.dr_former_players:
                Intent formerIntent = new Intent(ShortlistActivity.this, FormerPlayersListActivity.class);
                formerIntent.putExtra("managerId", managerId);
                formerIntent.putExtra("team", myTeam);
                startActivity(formerIntent);
                finish();
                break;
            case R.id.dr_shortlist:
                break;
            case R.id.dr_loaned_out_players:
                Intent loanIntent = new Intent(ShortlistActivity.this, LoanedOutPlayersActivity.class);
                loanIntent.putExtra("managerId", managerId);
                loanIntent.putExtra("team", myTeam);
                startActivity(loanIntent);
                finish();
                break;
            case R.id.dr_transfer_deals:
                Intent transferIntent = new Intent(ShortlistActivity.this, TransferDealsActivity.class);
                transferIntent.putExtra("managerId", managerId);
                transferIntent.putExtra("team", myTeam);
                startActivity(transferIntent);
                finish();
                break;
            case R.id.dr_support_info:
                Intent supportIntent = new Intent(ShortlistActivity.this, SupportActivity.class);
                supportIntent.putExtra("managerId", managerId);
                supportIntent.putExtra("team", myTeam);
                startActivity(supportIntent);
                finish();
                break;
            case R.id.dr_logout:
                if (user != null && firebaseAuth != null) {
                    firebaseAuth.signOut();
                    startActivity(new Intent(ShortlistActivity.this, MainActivity.class));
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
        Log.d(LOG_TAG, "onStart called: Fetching data for First Team, Youth Team, and Manager details.");

        ftPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            ftPlayersExist = Objects.requireNonNull(task.getResult()).size() > 0;
                            Log.d(LOG_TAG, "First Team players existence: " + ftPlayersExist);
                        } else {
                            Log.e(LOG_TAG, "Error fetching First Team players.", task.getException());
                        }
                    }
                });

        ytPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            ytPlayersExist = Objects.requireNonNull(task.getResult()).size() > 0;
                            Log.d(LOG_TAG, "Youth Team players existence: " + ytPlayersExist);
                        } else {
                            Log.e(LOG_TAG, "Error fetching Youth Team players.", task.getException());
                        }
                    }
                });

        managersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
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
                                Log.d(LOG_TAG, "Manager data fetched: " + manager.getFullName());
                            }
                            Manager theManager = managerList.get(0);
                            managerNameHeader.setText(theManager.getFullName());
                            teamHeader.setText(theManager.getTeam());
                            Log.d(LOG_TAG, "UI updated with manager details.");
                        } else {
                            Log.w(LOG_TAG, "No manager data found for managerId=" + managerId);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error fetching manager data.", e));
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "onDestroy called: Cleaning up resources.");

        if (nativeAdTop != null) {
            nativeAdTop.destroy();
        }
        if (nativeAdBottom != null) {
            nativeAdBottom.destroy();
        }
        super.onDestroy();
        Log.d(LOG_TAG, "ShortlistActivity destroyed.");
    }
}