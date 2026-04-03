package com.dimxlp.managerdb;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dimxlp.managerdb.ai.PlayerDataParser;
import com.dimxlp.managerdb.voice.VoicePlayerDataManager;
import com.dimxlp.managerdb.voice.VoiceInputTestMode;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import enumeration.PositionEnum;
import model.Manager;
import model.ShortlistedPlayer;
import ui.ShortlistedPlayerRecAdapter;
import util.NationalityFlagUtil;
import util.UserApi;
import util.ValueFormatter;

public class ShortlistPlayersActivity extends AppCompatActivity {

    private static final String LOG_TAG = "RAFI|ShortlistPlayers";
    private FirebaseAuth firebaseAuth;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference shPlayersColRef = db.collection("ShortlistedPlayers");
    private CollectionReference ftPlayersColRef = db.collection("FirstTeamPlayers");
    private CollectionReference ytPlayersColRef = db.collection("YouthTeamPlayers");
    private CollectionReference managersColRef = db.collection("Managers");

    private String currentUserId;
    private String currentUserName;

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navView;

    private List<ShortlistedPlayer> playerList;
    private RecyclerView recyclerView;
    private ShortlistedPlayerRecAdapter shortlistedPlayerRecAdapter;

    private Button prevPositionButton;
    private Button nextPositionButton;
    private TextView positionText;
    private TextView positionPlayerCount;
    private Button addPlayerButton;
    private boolean ftPlayersExist;
    private boolean ytPlayersExist;

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
    private long maxId;
    private long managerId;
    private String myTeam;
    private String barPosition;

    // Voice input components
    private VoicePlayerDataManager voiceManager;
    private LinearLayout voiceInputContainer;
    private ImageView voiceIcon;
    private TextView voiceStatusText;
    private ProgressBar voiceProgress;
    private ObjectAnimator pulseAnimator;

    private TextView managerNameHeader;
    private TextView teamHeader;
    // private NativeAd nativeAdBottom;
    // private NativeAdView nativeAdViewBottom;

    private BottomSheetDialog createDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shortlist_players);
        Log.i(LOG_TAG, "ShortlistPlayersActivity launched.");

        if (UserApi.getInstance() != null) {
            currentUserId = UserApi.getInstance().getUserId();
            currentUserName = UserApi.getInstance().getUsername();
            Log.d(LOG_TAG, "UserApi initialized: userId=" + currentUserId + ", username=" + currentUserName);
        } else {
            Log.w(LOG_TAG, "UserApi instance is null.");
        }

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        if (user != null) {
            Log.d(LOG_TAG, "Authenticated user: " + user.getUid());
        } else {
            Log.w(LOG_TAG, "No authenticated user.");
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            managerId = extras.getLong("managerId");
            myTeam = extras.getString("team");
            barPosition = extras.getString("barPosition");
            Log.d(LOG_TAG, "Extras received: managerId=" + managerId + ", team=" + myTeam + ", barPosition=" + barPosition);
        } else {
            Log.w(LOG_TAG, "No extras received in intent.");
        }

        playerList = new ArrayList<>();
        recyclerView = findViewById(R.id.rec_view_shp);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Initialize adapter once here
        shortlistedPlayerRecAdapter = new ShortlistedPlayerRecAdapter(this, playerList, managerId, myTeam, PositionEnum.GK.getCategory(), 0);
        recyclerView.setAdapter(shortlistedPlayerRecAdapter);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_menu);

        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nvView_shp);
        setUpDrawerContent(navView);

        View headerLayout = null;
        if (navView.getHeaderCount() > 0) {
            headerLayout = navView.getHeaderView(0);
        }
        managerNameHeader = headerLayout.findViewById(R.id.manager_name_header);
        teamHeader = headerLayout.findViewById(R.id.team_name_header);

        LinearLayout positionPickerLayout = findViewById(R.id.position_picker_container_shp);

        prevPositionButton = findViewById(R.id.prev_position_button_shp);
        nextPositionButton = findViewById(R.id.next_position_button_shp);
        positionText = findViewById(R.id.position_text_shp);
        addPlayerButton = findViewById(R.id.add_player_button_shp);
        positionText.setText(PositionEnum.GK.getCategory());
        positionPlayerCount = findViewById(R.id.position_player_count_shp);

        List<String> positionCategories = new ArrayList<>(
                new LinkedHashSet<>(getAllPositionCategories()) // removes duplicates while preserving order
        );
        positionPickerLayout.setOnClickListener(v -> {
            if (!positionCategories.isEmpty()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Select Position");
                builder.setItems(positionCategories.toArray(new String[0]), (dialog, which) -> {
                    String category = positionCategories.get(which);
                    positionText.setText(category);
                    listPlayers(category, 0);
                });
                builder.show();
            }
        });

        // Initialize Mobile Ads SDK
        // MobileAds.initialize(this, initializationStatus -> Log.d(LOG_TAG, "Mobile Ads SDK initialized."));
        //
        // nativeAdViewBottom = findViewById(R.id.native_ad_view_bottom);
        // loadNativeAd("ca-app-pub-3940256099942544/2247696110", nativeAdViewBottom);

        addPlayerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Add player FAB clicked.");
                createPopupDialog();
            }
        });

        View.OnClickListener prevPositionListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animatePositionButtons(v);
                changePositionForPreviousButton();
            }
        };

        View.OnClickListener nextPositionListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animatePositionButtons(v);
                changePositionForNextButton();
            }
        };

        prevPositionButton.setOnClickListener(prevPositionListener);
        nextPositionButton.setOnClickListener(nextPositionListener);
    }

    private List<String> getAllPositionCategories() {
        return Arrays.stream(PositionEnum.values())
                .map(p -> p.getCategory())
                .collect(Collectors.toList());
    }

    // private void loadNativeAd(String adUnitId, NativeAdView nativeAdView) {
    //     AdLoader adLoader = new AdLoader.Builder(this, adUnitId)
    //             .forNativeAd(ad -> {
    //                 if (isDestroyed()) {
    //                     ad.destroy();
    //                     return;
    //                 }
    //                 populateNativeAdView(ad, nativeAdView);
    //                 Log.d(LOG_TAG, "Native ad loaded successfully.");
    //             })
    //             .withAdListener(new com.google.android.gms.ads.AdListener() {
    //                 @Override
    //                 public void onAdFailedToLoad(LoadAdError adError) {
    //                     Log.e(LOG_TAG, "Native ad failed to load: " + adError.getMessage());
    //                 }
    //             })
    //             .build();
    //
    //     adLoader.loadAd(new AdRequest.Builder().build());
    // }
    //
    // private void populateNativeAdView(NativeAd nativeAd, NativeAdView nativeAdView) {
    //     int headlineId =  R.id.ad_headline_bottom;
    //     nativeAdView.setHeadlineView(nativeAdView.findViewById(headlineId));
    //     TextView headlineView = (TextView) nativeAdView.getHeadlineView();
    //
    //     if (nativeAd.getHeadline() != null) {
    //         headlineView.setText(nativeAd.getHeadline());
    //         headlineView.setVisibility(View.VISIBLE);
    //     } else {
    //         headlineView.setVisibility(View.GONE);
    //     }
    //
    //     // Remove body and CTA for compact layout
    //     nativeAdView.setBodyView(null);
    //     nativeAdView.setCallToActionView(null);
    //
    //     nativeAdView.setNativeAd(nativeAd);
    // }

    private void changePositionForNextButton() {
        String pos = positionText.getText().toString();
        Log.d(LOG_TAG, "Next position button clicked. Current position: " + pos);
        switch (pos) {
            case "Goalkeepers":
                positionText.setText(PositionEnum.CB.getCategory());
                break;
            case "Center Backs":
                positionText.setText(PositionEnum.RB.getCategory());
                break;
            case "Right Backs":
                positionText.setText(PositionEnum.LB.getCategory());
                break;
            case "Left Backs":
                positionText.setText(PositionEnum.CDM.getCategory());
                break;
            case "Center Defensive Mids":
                positionText.setText(PositionEnum.CM.getCategory());
                break;
            case "Center Midfielders":
                positionText.setText(PositionEnum.CAM.getCategory());
                break;
            case "Center Attacking Mids":
                positionText.setText(PositionEnum.RW.getCategory());
                break;
            case "Right Wingers":
                positionText.setText(PositionEnum.LW.getCategory());
                break;
            case "Left Wingers":
                positionText.setText(PositionEnum.ST.getCategory());
                break;
            case "Strikers":
                positionText.setText(PositionEnum.GK.getCategory());
                break;
            default:
                Log.w(LOG_TAG, "Unexpected position: " + pos);
                return;
        }
        pos = positionText.getText().toString();
        Log.d(LOG_TAG, "Position updated to next: " + pos);
        listPlayers(pos, 2);
    }

    private void changePositionForPreviousButton() {
        String pos = positionText.getText().toString();
        Log.d(LOG_TAG, "Previous position button clicked. Current position: " + pos);
        switch (pos) {
            case "Goalkeepers":
                positionText.setText(PositionEnum.ST.getCategory());
                break;
            case "Center Backs":
                positionText.setText(PositionEnum.GK.getCategory());
                break;
            case "Right Backs":
                positionText.setText(PositionEnum.CB.getCategory());
                break;
            case "Left Backs":
                positionText.setText(PositionEnum.RB.getCategory());
                break;
            case "Center Defensive Mids":
                positionText.setText(PositionEnum.LB.getCategory());
                break;
            case "Center Midfielders":
                positionText.setText(PositionEnum.CDM.getCategory());
                break;
            case "Center Attacking Mids":
                positionText.setText(PositionEnum.CM.getCategory());
                break;
            case "Right Wingers":
                positionText.setText(PositionEnum.CAM.getCategory());
                break;
            case "Left Wingers":
                positionText.setText(PositionEnum.RW.getCategory());
                break;
            case "Strikers":
                positionText.setText(PositionEnum.LW.getCategory());
                break;
            default:
                Log.w(LOG_TAG, "Unexpected position: " + pos);
                return;
        }
        pos = positionText.getText().toString();
        Log.d(LOG_TAG, "Position updated to previous: " + pos);
        listPlayers(pos, 1);
    }

    private void animatePositionButtons(View v) {
        v.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(100)
                .withEndAction(() -> {
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100);
                });
    }

    private void createPopupDialog() {
        Log.d(LOG_TAG, "Creating popup dialog for adding a shortlisted player.");
        createDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.create_shortlisted_player_popup, null);
        createDialog.setContentView(view);

        // Initialize voice manager
        voiceManager = new VoicePlayerDataManager(this);

        // Initialize voice UI components
        voiceInputContainer = view.findViewById(R.id.voice_input_container);
        voiceIcon = view.findViewById(R.id.voice_icon);
        voiceStatusText = view.findViewById(R.id.voice_status_text);
        voiceProgress = view.findViewById(R.id.voice_progress);

        // Setup voice input button click
        voiceInputContainer.setOnClickListener(v -> startVoiceInput());
        
        // Setup long-press for test mode (bypasses voice recognition)
        voiceInputContainer.setOnLongClickListener(v -> {
            startTestMode();
            return true;
        });

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
                            String currency = managerList.get(0).getCurrency();
                            valueTil.setHint("Value (in " + currency + ")");
                            wageTil.setHint("Wage (in " + currency + ")");
                            Log.d(LOG_TAG, "Currency fetched and hints updated: " + currency);
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
                    // Disable to prevent duplicate taps
                    createButton.setEnabled(false);
                    createButton.setText("Saving...");
                    createPlayer();
                } else {
                    Log.w(LOG_TAG, "Validation failed: Required fields are missing.");
                    Toast.makeText(ShortlistPlayersActivity.this, "Last Name/Nickname, Nationality, Position, Team and Overall are required", Toast.LENGTH_LONG)
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
        player.setId(maxId+1);
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

        shPlayersColRef.add(player)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.i(LOG_TAG, "Player successfully added to Firestore: " + documentReference.getId());
                        try {
                            if (createDialog != null && createDialog.isShowing()) {
                                Log.d(LOG_TAG, "Dismissing create dialog.");
                                createDialog.dismiss();
                            }
                            // Determine bar position for refresh
                            switch (positionPlayer) {
                                case "GK":
                                    barPosition = PositionEnum.GK.getCategory();
                                    break;
                                case "CB":
                                    barPosition = PositionEnum.CB.getCategory();
                                    break;
                                case "RB", "RWB":
                                    barPosition = PositionEnum.RB.getCategory();
                                    break;
                                case "LB", "LWB":
                                    barPosition = PositionEnum.LB.getCategory();
                                    break;
                                case "CDM":
                                    barPosition = PositionEnum.CDM.getCategory();
                                    break;
                                case "CM":
                                    barPosition = PositionEnum.CM.getCategory();
                                    break;
                                case "CAM":
                                    barPosition = PositionEnum.CAM.getCategory();
                                    break;
                                case "RM", "RW":
                                    barPosition = PositionEnum.RW.getCategory();
                                    break;
                                case "LM", "LW":
                                    barPosition = PositionEnum.LW.getCategory();
                                    break;
                                case "ST", "CF", "RF", "LF":
                                    barPosition = PositionEnum.ST.getCategory();
                                    break;
                            }
                            Log.d(LOG_TAG, "Bar position determined: " + barPosition);
                            // Refresh the current activity instead of restarting
                            Toast.makeText(ShortlistPlayersActivity.this, "Player created successfully!", Toast.LENGTH_SHORT).show();
                            // Refresh and dismiss dialog after completion
                            refreshPlayerList(() -> {
                                // Dismiss dialog after refresh completes
                                if (createDialog != null && createDialog.isShowing()) {
                                    Log.d(LOG_TAG, "Dismissing create dialog after refresh.");
                                    createDialog.dismiss();
                                }
                            });
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "Error in onSuccess callback", e);
                            if (createDialog != null && createDialog.isShowing()) {
                                createDialog.dismiss();
                            }
                            Toast.makeText(ShortlistPlayersActivity.this, "Player created but error occurred: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(LOG_TAG, "Error creating player", e);
                        if (createDialog != null && createDialog.isShowing()) {
                            createDialog.dismiss();
                        }
                        createButton.setText("CREATE PLAYER");
                        createButton.setEnabled(true);
                        Toast.makeText(ShortlistPlayersActivity.this, "Failed to create player: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    public void refreshPlayerList() {
        refreshPlayerList(null);
    }

    public void refreshPlayerList(Runnable onComplete) {
        Log.d(LOG_TAG, "refreshPlayerList called");
        if (barPosition != null) {
            listPlayers(barPosition, 0, onComplete);
        } else {
            listPlayers(PositionEnum.GK.getCategory(), 0, onComplete);
        }
    }

    private void listPlayers(final String position, final int buttonInt) {
        listPlayers(position, buttonInt, null);
    }

    private void listPlayers(final String position, final int buttonInt, Runnable onComplete) {
        Log.d(LOG_TAG, "Listing players for position: " + position + ", buttonInt: " + buttonInt);

        playerList.clear();
        shPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            Log.d(LOG_TAG, "Players fetched from Firestore. Filtering by position: " + position);

                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                ShortlistedPlayer player = doc.toObject(ShortlistedPlayer.class);
                                String playerPosition = player.getPosition();
                                if (position.equals(PositionEnum.GK.getCategory())
                                        && playerPosition.equals(PositionEnum.GK.getInitials())) {
                                    playerList.add(player);
                                } else if (position.equals(PositionEnum.CB.getCategory())
                                        && playerPosition.equals(PositionEnum.CB.getInitials())) {
                                    playerList.add(player);
                                } else if (position.equals(PositionEnum.RB.getCategory())
                                        && (playerPosition.equals(PositionEnum.RB.getInitials())
                                            || playerPosition.equals(PositionEnum.RWB.getInitials()))) {
                                    playerList.add(player);
                                } else if (position.equals(PositionEnum.LB.getCategory())
                                        && (playerPosition.equals(PositionEnum.LB.getInitials())
                                            || playerPosition.equals(PositionEnum.LWB.getInitials()))) {
                                    playerList.add(player);
                                } else if (position.equals(PositionEnum.CDM.getCategory())
                                        && playerPosition.equals(PositionEnum.CDM.getInitials())) {
                                    playerList.add(player);
                                } else if (position.equals(PositionEnum.CM.getCategory())
                                        && playerPosition.equals(PositionEnum.CM.getInitials())) {
                                    playerList.add(player);
                                } else if (position.equals(PositionEnum.CAM.getCategory())
                                        && playerPosition.equals(PositionEnum.CAM.getInitials())) {
                                    playerList.add(player);
                                } else if (position.equals(PositionEnum.RM.getCategory())
                                        && (playerPosition.equals(PositionEnum.RM.getInitials())
                                            || playerPosition.equals(PositionEnum.RW.getInitials()))) {
                                    playerList.add(player);
                                } else if (position.equals(PositionEnum.LW.getCategory())
                                        && (playerPosition.equals(PositionEnum.LM.getInitials())
                                            || playerPosition.equals(PositionEnum.LW.getInitials()))) {
                                    playerList.add(player);
                                } else if (position.equals(PositionEnum.ST.getCategory())
                                        && (playerPosition.equals(PositionEnum.ST.getInitials())
                                            || playerPosition.equals(PositionEnum.CF.getInitials())
                                            || playerPosition.equals(PositionEnum.RF.getInitials())
                                            || playerPosition.equals(PositionEnum.LF.getInitials()))) {
                                    playerList.add(player);
                                }
                            }

                            Log.d(LOG_TAG, "Filtered player count: " + playerList.size());

                            Collections.sort(playerList, new Comparator<ShortlistedPlayer>() {
                                @Override
                                public int compare(ShortlistedPlayer o1, ShortlistedPlayer o2) {
                                    return o1.getTimeAdded().compareTo(o2.getTimeAdded());
                                }
                            });
                            Log.d(LOG_TAG, "Player list sorted by time added.");

                            // Update adapter metadata instead of creating new instance
                            if (shortlistedPlayerRecAdapter != null) {
                                shortlistedPlayerRecAdapter.updateMetadata(position, buttonInt);
                                shortlistedPlayerRecAdapter.notifyDataSetChanged();
                            }
                            positionPlayerCount.setText(playerList.size() + " player(s)");
                            Log.d(LOG_TAG, "RecyclerView updated with shortlisted players.");
                            
                            // Call completion callback if provided
                            if (onComplete != null) {
                                onComplete.run();
                            }
                        } else {
                            Log.w(LOG_TAG, "No players found for userId=" + UserApi.getInstance().getUserId() + ", managerId=" + managerId);
                            // Call completion callback even if no players found
                            if (onComplete != null) {
                                onComplete.run();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(LOG_TAG, "Error fetching players from Firestore.", e);
                    // Call completion callback on failure too
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
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
                Intent homeIntent = new Intent(ShortlistPlayersActivity.this, ManageTeamActivity.class);
                homeIntent.putExtra("managerId", managerId);
                homeIntent.putExtra("team", myTeam);
                startActivity(homeIntent);
                finish();
                break;
            case R.id.dr_manager_selection:
                Intent managerSelectionIntent = new Intent(ShortlistPlayersActivity.this, SelectManagerActivity.class);
                startActivity(managerSelectionIntent);
                break;
            case R.id.dr_profile:
                Intent profileIntent = new Intent(ShortlistPlayersActivity.this, ProfileActivity.class);
                profileIntent.putExtra("managerId", managerId);
                profileIntent.putExtra("team", myTeam);
                startActivity(profileIntent);
                finish();
                break;
            case R.id.dr_first_team:
                if (ftPlayersExist) {
                    Intent firstIntent = new Intent(ShortlistPlayersActivity.this, FirstTeamListActivity.class);
                    firstIntent.putExtra("managerId", managerId);
                    firstIntent.putExtra("team", myTeam);
                    startActivity(firstIntent);
                    finish();
                } else {
                    Intent firstIntent = new Intent(ShortlistPlayersActivity.this, FirstTeamActivity.class);
                    firstIntent.putExtra("managerId", managerId);
                    firstIntent.putExtra("team", myTeam);
                    startActivity(firstIntent);
                    finish();
                }
                break;
            case R.id.dr_youth_team:
                if (ytPlayersExist) {
                    Intent youthIntent = new Intent(ShortlistPlayersActivity.this, YouthTeamListActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", myTeam);
                    startActivity(youthIntent);
                    finish();
                } else {
                    Intent youthIntent = new Intent(ShortlistPlayersActivity.this, YouthTeamActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", myTeam);
                    startActivity(youthIntent);
                    finish();
                }
                break;
            case R.id.dr_former_players:
                Intent formerIntent = new Intent(ShortlistPlayersActivity.this, FormerPlayersListActivity.class);
                formerIntent.putExtra("managerId", managerId);
                formerIntent.putExtra("team", myTeam);
                startActivity(formerIntent);
                finish();
                break;
            case R.id.dr_shortlist:
                break;
            case R.id.dr_loaned_out_players:
                Intent loanIntent = new Intent(ShortlistPlayersActivity.this, LoanedOutPlayersActivity.class);
                loanIntent.putExtra("managerId", managerId);
                loanIntent.putExtra("team", myTeam);
                startActivity(loanIntent);
                finish();
                break;
            case R.id.dr_transfer_deals:
                Intent transferIntent = new Intent(ShortlistPlayersActivity.this, TransferDealsActivity.class);
                transferIntent.putExtra("managerId", managerId);
                transferIntent.putExtra("team", myTeam);
                startActivity(transferIntent);
                finish();
                break;
            case R.id.dr_support_info:
                Intent supportIntent = new Intent(ShortlistPlayersActivity.this, SupportActivity.class);
                supportIntent.putExtra("managerId", managerId);
                supportIntent.putExtra("team", myTeam);
                startActivity(supportIntent);
                finish();
                break;
            case R.id.dr_logout:
                if (user != null && firebaseAuth != null) {
                    firebaseAuth.signOut();
                    startActivity(new Intent(ShortlistPlayersActivity.this, MainActivity.class));
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
        Log.d(LOG_TAG, "onStart called: Fetching data for Youth Team, First Team, and Shortlisted Players.");

        ytPlayersColRef.whereEqualTo("userId", currentUserId)
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            ytPlayersExist = !Objects.requireNonNull(task.getResult()).isEmpty();
                            Log.d(LOG_TAG, "Youth Team players existence: " + ytPlayersExist);
                        } else {
                            Log.e(LOG_TAG, "Error fetching Youth Team players.", task.getException());
                        }
                    }
                });

        ftPlayersColRef.whereEqualTo("userId", currentUserId)
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            ftPlayersExist = !Objects.requireNonNull(task.getResult()).isEmpty();
                            Log.d(LOG_TAG, "First Team players existence: " + ftPlayersExist);
                        } else {
                            Log.e(LOG_TAG, "Error fetching First Team players.", task.getException());
                        }
                    }
                });

        shPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            Log.d(LOG_TAG, "Shortlisted players fetched from Firestore.");
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                ShortlistedPlayer player = doc.toObject(ShortlistedPlayer.class);
                                if (barPosition == null) {
                                    if (player.getPosition().equals(PositionEnum.GK.getInitials())) {
                                        playerList.add(player);
                                    }
                                } else if (barPosition.equals(PositionEnum.GK.getCategory())
                                        && player.getPosition().equals(PositionEnum.GK.getInitials())) {
                                    playerList.add(player);
                                } else if (barPosition.equals(PositionEnum.CB.getCategory())
                                        && player.getPosition().equals(PositionEnum.CB.getInitials())) {
                                    playerList.add(player);
                                } else if (barPosition.equals(PositionEnum.RB.getCategory())
                                        && (player.getPosition().equals(PositionEnum.RB.getInitials())
                                        || player.getPosition().equals(PositionEnum.RWB.getInitials()))) {
                                    playerList.add(player);
                                } else if (barPosition.equals(PositionEnum.LB.getCategory())
                                        && (player.getPosition().equals(PositionEnum.LB.getInitials())
                                        || player.getPosition().equals(PositionEnum.LWB.getInitials()))) {
                                    playerList.add(player);
                                } else if (barPosition.equals(PositionEnum.CDM.getCategory())
                                        && player.getPosition().equals(PositionEnum.CDM.getInitials())) {
                                    playerList.add(player);
                                } else if (barPosition.equals(PositionEnum.CM.getCategory())
                                        && player.getPosition().equals(PositionEnum.CM.getInitials())) {
                                    playerList.add(player);
                                } else if (barPosition.equals(PositionEnum.CAM.getCategory())
                                        && player.getPosition().equals(PositionEnum.CAM.getInitials())) {
                                    playerList.add(player);
                                } else if (barPosition.equals(PositionEnum.RM.getCategory())
                                        && (player.getPosition().equals(PositionEnum.RM.getInitials())
                                        || player.getPosition().equals(PositionEnum.RW.getInitials()))) {
                                    playerList.add(player);
                                } else if (barPosition.equals(PositionEnum.LW.getCategory())
                                        && (player.getPosition().equals(PositionEnum.LM.getInitials())
                                        || player.getPosition().equals(PositionEnum.LW.getInitials()))) {
                                    playerList.add(player);
                                } else if (barPosition.equals(PositionEnum.ST.getCategory())
                                        && (player.getPosition().equals(PositionEnum.ST.getInitials())
                                        || player.getPosition().equals(PositionEnum.CF.getInitials())
                                        || player.getPosition().equals(PositionEnum.RF.getInitials())
                                        || player.getPosition().equals(PositionEnum.LF.getInitials()))) {
                                    playerList.add(player);
                                }
                            }
                            Log.d(LOG_TAG, "Filtered shortlisted players count: " + playerList.size());

                            playerList.sort(new Comparator<ShortlistedPlayer>() {
                                @Override
                                public int compare(ShortlistedPlayer player1, ShortlistedPlayer player2) {
                                    return player1.getTimeAdded().compareTo(player2.getTimeAdded());
                                }
                            });
                            Log.d(LOG_TAG, "Shortlisted players sorted by time added.");

                            if (barPosition == null) {
                                positionText.setText(PositionEnum.GK.getCategory());
                                // Update adapter instead of creating new instance
                                if (shortlistedPlayerRecAdapter != null) {
                                    shortlistedPlayerRecAdapter.updateMetadata(PositionEnum.GK.getCategory(), 0);
                                    shortlistedPlayerRecAdapter.notifyDataSetChanged();
                                }
                            } else {
                                positionText.setText(barPosition);
                                // Update adapter instead of creating new instance
                                if (shortlistedPlayerRecAdapter != null) {
                                    shortlistedPlayerRecAdapter.updateMetadata(barPosition, 0);
                                    shortlistedPlayerRecAdapter.notifyDataSetChanged();
                                }
                            }
                            positionPlayerCount.setText(playerList.size() + " player(s)");
                            Log.d(LOG_TAG, "RecyclerView updated with shortlisted players.");
                        } else {
                            positionPlayerCount.setText(playerList.size() + " player(s)");
                            Log.w(LOG_TAG, "No shortlisted players found for managerId=" + managerId);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error fetching shortlisted players.", e));

        shPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                            List<ShortlistedPlayer> shPlayers = new ArrayList<>();
                            for (DocumentSnapshot doc: docs) {
                                ShortlistedPlayer player = doc.toObject(ShortlistedPlayer.class);
                                shPlayers.add(player);
                            }
                            findMaxPlayerId(shPlayers);
                            for (DocumentSnapshot ds: docs) {
                                ShortlistedPlayer shp = ds.toObject(ShortlistedPlayer.class);
                                assert shp != null;
                                if (shp.getId() == 0) {
                                    shp.setId(maxId+1);
                                    shPlayersColRef.document(ds.getId()).update("id", shp.getId());
                                    maxId++;
                                    Log.d(LOG_TAG, "Updated player ID for: " + shp.getFullName());
                                }
                            }
                        } else {
                            Log.w(LOG_TAG, "No shortlisted players found to update IDs.");
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

    private void findMaxPlayerId(List<ShortlistedPlayer> shPlayers) {
        maxId = shPlayers.get(0).getId();
        for (ShortlistedPlayer player: shPlayers) {
            if (player.getId() > maxId) {
                maxId = player.getId();
            }
        }
        Log.d(LOG_TAG, "Max player ID determined: " + maxId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume called: Clearing player list.");
        playerList.clear();
    }

    /**
     * Test mode - bypasses voice recognition and uses sample transcript
     * Useful when emulator audio is not working
     * Trigger by LONG-PRESSING the voice button
     */
    private void startTestMode() {
        Log.d(LOG_TAG, "Test mode activated - bypassing voice recognition.");
        
        // Show options for different test samples
        String[] testOptions = {
                "Mbappe (Complete data)",
                "Haaland (Complete data)",
                "Vinicius Jr (Partial data)",
                "Bellingham (Casual speech)",
                "Harry Kane (Natural speech)"
        };
        
        new AlertDialog.Builder(this)
                .setTitle("🧪 Test Mode - Select Sample")
                .setItems(testOptions, (dialog, which) -> {
                    runOnUiThread(() -> setVoiceUIState(VoiceUIState.PROCESSING));
                    
                    VoiceInputTestMode.testWithSample(this, which, new PlayerDataParser.PlayerDataCallback() {
                        @Override
                        public void onSuccess(PlayerDataParser.PlayerData data) {
                            Log.d(LOG_TAG, "Test mode: AI parsed successfully: " + data.toString());
                            runOnUiThread(() -> {
                                setVoiceUIState(VoiceUIState.SUCCESS);
                                fillFormWithVoiceData(data);
                                Toast.makeText(ShortlistPlayersActivity.this,
                                        "🧪 Test Mode: Data loaded! (No voice used)",
                                        Toast.LENGTH_LONG).show();
                            });
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(LOG_TAG, "Test mode: AI parsing error: " + error);
                            runOnUiThread(() -> {
                                setVoiceUIState(VoiceUIState.ERROR);
                                Toast.makeText(ShortlistPlayersActivity.this,
                                        "Test Error: " + error,
                                        Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Start voice input process
     */
    private void startVoiceInput() {
        Log.d(LOG_TAG, "Voice input button clicked.");
        voiceManager.startVoiceInput(new VoicePlayerDataManager.PlayerDataResultCallback() {
            @Override
            public void onPlayerDataParsed(PlayerDataParser.PlayerData data) {
                Log.d(LOG_TAG, "Voice data parsed successfully: " + data.toString());
                runOnUiThread(() -> {
                    setVoiceUIState(VoiceUIState.SUCCESS);
                    fillFormWithVoiceData(data);
                    Toast.makeText(ShortlistPlayersActivity.this,
                            "✓ Player data loaded! Please review and save.",
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(LOG_TAG, "Voice input error: " + error);
                runOnUiThread(() -> {
                    setVoiceUIState(VoiceUIState.ERROR);
                    Toast.makeText(ShortlistPlayersActivity.this,
                            "Error: " + error + "\nTap to try again.",
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onListeningStarted() {
                Log.d(LOG_TAG, "Voice listening started.");
                runOnUiThread(() -> setVoiceUIState(VoiceUIState.LISTENING));
            }

            @Override
            public void onListeningEnded() {
                Log.d(LOG_TAG, "Voice listening ended, processing...");
                runOnUiThread(() -> setVoiceUIState(VoiceUIState.PROCESSING));
            }
        });
    }

    /**
     * Fill form fields with voice-parsed data
     */
    private void fillFormWithVoiceData(PlayerDataParser.PlayerData data) {
        Log.d(LOG_TAG, "Filling form with voice data...");
        
        // Parse name into first and last name
        if (!data.name.isEmpty()) {
            String[] nameParts = data.name.trim().split("\\s+", 2);
            if (nameParts.length == 2) {
                firstName.setText(nameParts[0]);
                lastName.setText(nameParts[1]);
            } else {
                // If only one name, use it as last name
                lastName.setText(nameParts[0]);
            }
        }
        
        // Map position abbreviations
        if (!data.position.isEmpty()) {
            String mappedPosition = mapPositionFromVoice(data.position);
            positionPicker.setText(mappedPosition);
        }
        
        if (!data.nationality.isEmpty()) {
            nationality.setText(data.nationality);
        }
        
        if (!data.overall.isEmpty()) {
            overall.setText(data.overall);
        }
        
        if (!data.potential.isEmpty()) {
            // Use potential for both low and high if only one value provided
            potLow.setText(data.potential);
            potHigh.setText(data.potential);
        }
        
        if (!data.value.isEmpty()) {
            // Clean and format value (remove M, million, etc.)
            String cleanValue = cleanMonetaryValue(data.value);
            value.setText(cleanValue);
        }
        
        if (!data.wage.isEmpty()) {
            // Clean and format wage (remove K, thousand, etc.)
            String cleanWage = cleanMonetaryValue(data.wage);
            wage.setText(cleanWage);
        }
        
        if (!data.age.isEmpty()) {
            // Age can be added to comments
            String currentComments = comments.getText().toString();
            if (!currentComments.isEmpty()) {
                comments.setText(currentComments + "\nAge: " + data.age);
            } else {
                comments.setText("Age: " + data.age);
            }
        }
        
        Log.d(LOG_TAG, "Form filled successfully with voice data.");
    }

    /**
     * Map position from voice input (e.g., "striker" -> "ST")
     */
    private String mapPositionFromVoice(String voicePosition) {
        String lower = voicePosition.toLowerCase().trim();
        
        // Direct matches
        if (lower.matches("gk|goalkeeper")) return "GK";
        if (lower.matches("cb|center back|centre back")) return "CB";
        if (lower.matches("rb|right back")) return "RB";
        if (lower.matches("lb|left back")) return "LB";
        if (lower.matches("rwb|right wing back")) return "RWB";
        if (lower.matches("lwb|left wing back")) return "LWB";
        if (lower.matches("cdm|defensive mid.*")) return "CDM";
        if (lower.matches("cm|central mid.*|center mid.*")) return "CM";
        if (lower.matches("cam|attacking mid.*")) return "CAM";
        if (lower.matches("rm|right mid.*")) return "RM";
        if (lower.matches("lm|left mid.*")) return "LM";
        if (lower.matches("rw|right wing.*")) return "RW";
        if (lower.matches("lw|left wing.*")) return "LW";
        if (lower.matches("st|striker|center forward")) return "ST";
        if (lower.matches("cf|forward")) return "CF";
        if (lower.matches("rf|right forward")) return "RF";
        if (lower.matches("lf|left forward")) return "LF";
        
        // Partial matches
        if (lower.contains("winger") || lower.contains("wing")) {
            if (lower.contains("right")) return "RW";
            if (lower.contains("left")) return "LW";
            return "RW"; // Default to right wing
        }
        if (lower.contains("midfielder") || lower.contains("mid")) {
            if (lower.contains("defensive") || lower.contains("holding")) return "CDM";
            if (lower.contains("attacking") || lower.contains("offensive")) return "CAM";
            return "CM"; // Default to central midfielder
        }
        if (lower.contains("back") || lower.contains("defender")) {
            if (lower.contains("right")) return "RB";
            if (lower.contains("left")) return "LB";
            if (lower.contains("center") || lower.contains("centre")) return "CB";
            return "CB"; // Default to center back
        }
        
        // Return as-is if no match (uppercase first 2 letters)
        return voicePosition.length() >= 2 ? 
                voicePosition.substring(0, 2).toUpperCase() : 
                voicePosition.toUpperCase();
    }

    /**
     * Clean monetary values (remove M, K, million, thousand, etc.)
     */
    private String cleanMonetaryValue(String value) {
        String cleaned = value.toUpperCase()
                .replaceAll("[MK]$", "") // Remove M or K at end
                .replaceAll("MILLION|THOUSAND|EUROS?|POUNDS?|DOLLARS?", "")
                .replaceAll("[^0-9.]", "")
                .trim();
        
        // Convert M to millions, K to thousands
        if (value.toUpperCase().contains("M")) {
            try {
                double num = Double.parseDouble(cleaned);
                return String.valueOf((int)(num * 1_000_000));
            } catch (NumberFormatException e) {
                return cleaned;
            }
        } else if (value.toUpperCase().contains("K")) {
            try {
                double num = Double.parseDouble(cleaned);
                return String.valueOf((int)(num * 1_000));
            } catch (NumberFormatException e) {
                return cleaned;
            }
        }
        
        return cleaned;
    }

    /**
     * Voice UI states
     */
    private enum VoiceUIState {
        IDLE, LISTENING, PROCESSING, SUCCESS, ERROR
    }

    /**
     * Update voice input UI based on current state
     */
    private void setVoiceUIState(VoiceUIState state) {
        if (voiceInputContainer == null) return;
        
        // Stop any running animations
        if (pulseAnimator != null && pulseAnimator.isRunning()) {
            pulseAnimator.cancel();
        }
        
        switch (state) {
            case IDLE:
                voiceInputContainer.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFFE8F5E9));
                voiceIcon.setVisibility(View.VISIBLE);
                voiceIcon.setImageResource(R.drawable.ic_baseline_mic_24);
                voiceIcon.setColorFilter(0xFF4CAF50);
                voiceStatusText.setText("🎤 Voice Input - Say player details");
                voiceStatusText.setTextColor(0xFF2E7D32);
                voiceProgress.setVisibility(View.GONE);
                voiceInputContainer.setClickable(true);
                break;
                
            case LISTENING:
                voiceInputContainer.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFFFFF3E0));
                voiceIcon.setVisibility(View.VISIBLE);
                voiceIcon.setImageResource(R.drawable.ic_baseline_mic_24);
                voiceIcon.setColorFilter(0xFFFF9800);
                voiceStatusText.setText("🎙️ Listening... Speak now!");
                voiceStatusText.setTextColor(0xFFE65100);
                voiceProgress.setVisibility(View.GONE);
                voiceInputContainer.setClickable(false);
                
                // Pulse animation on icon
                pulseAnimator = ObjectAnimator.ofFloat(voiceIcon, "alpha", 1f, 0.3f, 1f);
                pulseAnimator.setDuration(1000);
                pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
                pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
                pulseAnimator.start();
                break;
                
            case PROCESSING:
                voiceInputContainer.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFFE3F2FD));
                voiceIcon.setVisibility(View.GONE);
                voiceStatusText.setText("⚡ Processing with AI...");
                voiceStatusText.setTextColor(0xFF1976D2);
                voiceProgress.setVisibility(View.VISIBLE);
                voiceInputContainer.setClickable(false);
                break;
                
            case SUCCESS:
                voiceInputContainer.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFFE8F5E9));
                voiceIcon.setVisibility(View.VISIBLE);
                voiceIcon.setImageResource(R.drawable.ic_baseline_mic_24);
                voiceIcon.setColorFilter(0xFF4CAF50);
                voiceStatusText.setText("✓ Data loaded! Review and save below");
                voiceStatusText.setTextColor(0xFF2E7D32);
                voiceProgress.setVisibility(View.GONE);
                voiceInputContainer.setClickable(true);
                break;
                
            case ERROR:
                voiceInputContainer.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFFFFEBEE));
                voiceIcon.setVisibility(View.VISIBLE);
                voiceIcon.setImageResource(R.drawable.ic_baseline_mic_24);
                voiceIcon.setColorFilter(0xFFD32F2F);
                voiceStatusText.setText("❌ Error - Tap to try again");
                voiceStatusText.setTextColor(0xFFC62828);
                voiceProgress.setVisibility(View.GONE);
                voiceInputContainer.setClickable(true);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (voiceManager != null) {
            voiceManager.onPermissionResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onDestroy() {
        // Cleanup voice manager
        if (voiceManager != null) {
            voiceManager.cleanup();
        }
        
        // Stop any running animations
        if (pulseAnimator != null && pulseAnimator.isRunning()) {
            pulseAnimator.cancel();
        }
        
        // if (nativeAdBottom != null) nativeAdBottom.destroy();
        Log.i(LOG_TAG, "ShortlistPlayersActivity destroyed.");
        super.onDestroy();
    }
}

