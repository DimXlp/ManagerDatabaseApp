package com.dimxlp.managerdb;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import model.FirstTeamPlayer;
import model.Manager;
import ui.FirstTeamPlayerRecAdapter;
import util.UserApi;

public class FirstTeamListActivity extends AppCompatActivity {

    private static final String LOG_TAG = "RAFI|FirstTeamList";
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("FirstTeamPlayers");

    private String currentUserId;
    private String currentUserName;

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navView;

    private List<FirstTeamPlayer> playerList;
    private RecyclerView recyclerView;
    private FirstTeamPlayerRecAdapter firstTeamPlayerRecAdapter;

    private Button prevYearButton;
    private Button nextYearButton;
    private TextView yearText;
    private TextView yearPlayerCount;
    private Button addPlayerButton;

    private AlertDialog.Builder builder;
    private AlertDialog dialog;

    private EditText firstName;
    private EditText lastName;
    private TextView positionPicker;
    private EditText number;
    private EditText nationality;
    private EditText overall;
    private EditText potentialLow;
    private EditText potentialHigh;
    private TextView yearSigned;
    private TextView yearScouted;
    private SwitchMaterial loanSwitch;
    private Button createPlayerButton;
    private String currentYear;
    private String firstYear;
    //private int minYear;

    //private int playerCount;
    private long maxId;
    private boolean ytPlayersExist;
    private boolean shPlayersExist;
    private String team;
    private long managerId;

    private TextView managerNameHeader;
    private TextView teamHeader;
    private String minYearText;
    private String barYear;

    private Animation slideLeft;
    private Animation slideRight;
    private NativeAdView nativeAdViewBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_team_list);

        if (UserApi.getInstance() != null) {
            currentUserId = UserApi.getInstance().getUserId();
            currentUserName = UserApi.getInstance().getUsername();
        }

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            managerId = extras.getLong("managerId");
            team = extras.getString("team");
            barYear = extras.getString("barYear");
        }

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_menu);

        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nvView);
        setUpDrawerContent(navView);

        prevYearButton = findViewById(R.id.prev_year_button_ftp);
        nextYearButton = findViewById(R.id.next_year_button_ftp);

        LinearLayout yearPickerLayout = findViewById(R.id.year_picker_container);
        yearText = findViewById(R.id.year_text_ftp);
        yearPlayerCount = findViewById(R.id.year_player_count);

        List<String> availableYears = new ArrayList<>();

        collectionReference.whereEqualTo("userId", currentUserId)
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            FirstTeamPlayer player = doc.toObject(FirstTeamPlayer.class);
                            if (player != null && player.getYearSigned() != null) {
                                String y = player.getYearSigned();
                                if (!availableYears.contains(y)) {
                                    availableYears.add(y);
                                }
                            }
                        }
                        Collections.sort(availableYears);
                    }
                });

        yearPickerLayout.setOnClickListener(v -> {
            if (!availableYears.isEmpty()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Select Year");
                builder.setItems(availableYears.toArray(new String[0]), (dialog, which) -> {
                    currentYear = availableYears.get(which);
                    listPlayers(0);
                });
                builder.show();
            }
        });

        View headerLayout = null;
        if (navView.getHeaderCount() > 0) {
            headerLayout = navView.getHeaderView(0);
        }
        managerNameHeader = headerLayout.findViewById(R.id.manager_name_header);
        teamHeader = headerLayout.findViewById(R.id.team_name_header);

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> Log.d(LOG_TAG, "Mobile Ads SDK initialized."));

        nativeAdViewBottom = findViewById(R.id.native_ad_view_bottom);
        loadNativeAd("ca-app-pub-3940256099942544/2247696110", nativeAdViewBottom);

        slideLeft = AnimationUtils.loadAnimation(FirstTeamListActivity.this, R.anim.slide_left);
        slideRight = AnimationUtils.loadAnimation(FirstTeamListActivity.this, R.anim.slide_right);

        View.OnClickListener prevYearListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateYearButtons(v);
                int cYear = Integer.parseInt(currentYear.substring(0, 4));
                int minYear = Integer.parseInt(minYearText.substring(0, 4));
                if (cYear > minYear) {
                    cYear--;
                    currentYear = cYear + "/" + ((cYear % 100) + 1);
                    listPlayers(1);
                } else {
                    Toast.makeText(FirstTeamListActivity.this, "You are already in the first year!", Toast.LENGTH_LONG)
                            .show();
                }
            }
        };

        View.OnClickListener nextYearListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateYearButtons(v);
                int cYear = Integer.parseInt(currentYear.substring(0, 4));
                cYear++;
                currentYear = cYear + "/" + ((cYear % 100) + 1);
                listPlayers(2);
            }
        };

        prevYearButton.setOnClickListener(prevYearListener);
        nextYearButton.setOnClickListener(nextYearListener);

        addPlayerButton = findViewById(R.id.add_player_button);

        addPlayerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createPopupDialog();
            }
        });

        playerList = new ArrayList<>();
        recyclerView = findViewById(R.id.rec_view_ftp);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadNativeAd(String adUnitId, NativeAdView nativeAdView) {
        AdLoader adLoader = new AdLoader.Builder(this, adUnitId)
                .forNativeAd(ad -> {
                    if (isDestroyed()) {
                        ad.destroy();
                        return;
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
        int headlineId =  R.id.ad_headline_bottom;
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

    private static void animateYearButtons(View v) {
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

    private void listPlayers(final int buttonInt) {
        playerList.clear();

        // Ensure currentYear is initialized
        if (currentYear == null) {
            if (barYear != null) {
                currentYear = barYear;
            } else if (minYearText != null) {
                currentYear = minYearText;
            } else {
                Toast.makeText(this, "Unable to determine the current year. Please try again.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        collectionReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                FirstTeamPlayer player = doc.toObject(FirstTeamPlayer.class);
                                if (player.getYearSigned().equals(currentYear)) {
                                    playerList.add(player);
                                }
                            }
                            Collections.sort(playerList, new Comparator<FirstTeamPlayer>() {
                                @Override
                                public int compare(FirstTeamPlayer o1, FirstTeamPlayer o2) {
                                    return o1.getTimeAdded().compareTo(o2.getTimeAdded());
                                }
                            });
                            yearText.setText(currentYear);
                            firstTeamPlayerRecAdapter = new FirstTeamPlayerRecAdapter(FirstTeamListActivity.this, playerList, managerId, team, currentYear, buttonInt, maxId);
                            recyclerView.setAdapter(firstTeamPlayerRecAdapter);
                            firstTeamPlayerRecAdapter.notifyDataSetChanged();
                            yearText.setText(currentYear);
                            yearPlayerCount.setText(playerList.size() + " players");
                        }
                    }
                });
    }

    private void createPopupDialog() {
        builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.create_first_team_player_popup, null);

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

        String[] positions = this.getResources().getStringArray(R.array.position_array);
        String[] years = this.getResources().getStringArray(R.array.years_array);

        positionPicker.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Select Position")
                    .setItems(positions, (pickerDialog, which) -> positionPicker.setText(positions[which]))
                    .show();
        });

        yearSigned.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Select Year Signed")
                    .setItems(years, (pickerDialog, which) -> yearSigned.setText(years[which]))
                    .show();
        });

        yearScouted.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Select Year Scouted")
                    .setItems(years, (pickerDialog, which) -> yearScouted.setText(years[which]))
                    .show();
        });

        createPlayerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!lastName.getText().toString().isEmpty() &&
                        !nationality.getText().toString().isEmpty() &&
                        !positionPicker.getText().toString().isEmpty() &&
                        !overall.getText().toString().isEmpty() &&
                        !yearSigned.getText().toString().isEmpty()) {
                    createPlayer();
                } else {
                    Toast.makeText(FirstTeamListActivity.this, "Last Name/Nickname, Nationality, Position, Overall and Year Signed are required", Toast.LENGTH_LONG)
                            .show();
                }
            }
        });

        builder.setView(view);
        dialog = builder.create();
        dialog.show();
    }

    private void createPlayer() {
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
        String overallPlayer = overall.getText().toString().trim();
        String potentialLowPlayer = potentialLow.getText().toString().trim();
        String potentialHiPlayer = potentialHigh.getText().toString().trim();
        final String ySignedPlayer = yearSigned.getText().toString().trim();
        String yScoutedPlayer = yearScouted.getText().toString().trim();

        final FirstTeamPlayer player = new FirstTeamPlayer();

        player.setId(maxId+1);
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
        player.setNationality(nationalityPlayer);
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
        player.setManagerId(managerId);
        player.setUserId(currentUserId);
        player.setTimeAdded(new Timestamp(new Date()));
        player.setLoanPlayer(loanSwitch.isChecked());

        collectionReference.add(player)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        dialog.dismiss();
                        Intent intent = new Intent(FirstTeamListActivity.this, FirstTeamListActivity.class);
                        intent.putExtra("managerId", managerId);
                        intent.putExtra("team", team);
                        intent.putExtra("barYear", ySignedPlayer);
                        startActivity(intent);
                        finish();
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
                Intent homeIntent = new Intent(FirstTeamListActivity.this, ManageTeamActivity.class);
                homeIntent.putExtra("managerId", managerId);
                homeIntent.putExtra("team", team);
                startActivity(homeIntent);
                finish();
                break;
            case R.id.dr_manager_selection:
                Intent managerSelectionIntent = new Intent(FirstTeamListActivity.this, SelectManagerActivity.class);
                startActivity(managerSelectionIntent);
                break;
            case R.id.dr_profile:
                Intent profileIntent = new Intent(FirstTeamListActivity.this, ProfileActivity.class);
                profileIntent.putExtra("managerId", managerId);
                profileIntent.putExtra("team", team);
                startActivity(profileIntent);
                finish();
                break;
            case R.id.dr_first_team:
                break;
            case R.id.dr_youth_team:
                if (ytPlayersExist) {
                    Intent youthIntent = new Intent(FirstTeamListActivity.this, YouthTeamListActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", team);
                    startActivity(youthIntent);
                    finish();
                } else {
                    Intent youthIntent = new Intent(FirstTeamListActivity.this, YouthTeamActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", team);
                    startActivity(youthIntent);
                    finish();
                }
                break;
            case R.id.dr_former_players:
                Intent formerIntent = new Intent(FirstTeamListActivity.this, FormerPlayersListActivity.class);
                formerIntent.putExtra("managerId", managerId);
                formerIntent.putExtra("team", team);
                startActivity(formerIntent);
                finish();
                break;
            case R.id.dr_shortlist:
                if (shPlayersExist) {
                    Intent shortlistIntent = new Intent(FirstTeamListActivity.this, ShortlistPlayersActivity.class);
                    shortlistIntent.putExtra("managerId", managerId);
                    shortlistIntent.putExtra("team", team);
                    startActivity(shortlistIntent);
                    finish();
                } else {
                    Intent shortlistIntent = new Intent(FirstTeamListActivity.this, ShortlistActivity.class);
                    shortlistIntent.putExtra("managerId", managerId);
                    shortlistIntent.putExtra("team", team);
                    startActivity(shortlistIntent);
                    finish();
                }
                break;
            case R.id.dr_loaned_out_players:
                Intent loanIntent = new Intent(FirstTeamListActivity.this, LoanedOutPlayersActivity.class);
                loanIntent.putExtra("managerId", managerId);
                loanIntent.putExtra("team", team);
                startActivity(loanIntent);
                finish();
                break;
            case R.id.dr_transfer_deals:
                Intent transferIntent = new Intent(FirstTeamListActivity.this, TransferDealsActivity.class);
                transferIntent.putExtra("managerId", managerId);
                transferIntent.putExtra("team", team);
                startActivity(transferIntent);
                finish();
                break;
            case R.id.dr_support_info:
                Intent supportIntent = new Intent(FirstTeamListActivity.this, SupportActivity.class);
                supportIntent.putExtra("managerId", managerId);
                supportIntent.putExtra("team", team);
                startActivity(supportIntent);
                finish();
                break;
            case R.id.dr_logout:
                if (user != null && firebaseAuth != null) {
                    firebaseAuth.signOut();
                    startActivity(new Intent(FirstTeamListActivity.this, MainActivity.class));
                    finishAffinity();
                }
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

        // Ensure currentYear has a fallback value
        if (barYear != null) {
            currentYear = barYear;
        } else if (minYearText != null) {
            currentYear = minYearText;
        } else {
            currentYear = "2020/21"; // Default fallback
        }

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

        collectionReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                            List<FirstTeamPlayer> ftplayers = new ArrayList<>();
                            for (DocumentSnapshot doc: docs) {
                                FirstTeamPlayer player = doc.toObject(FirstTeamPlayer.class);
                                ftplayers.add(player);
                            }
                            findMinYearSigned(ftplayers);
                            findMaxPlayerId(ftplayers);

                            if (barYear != null) {
                                currentYear = barYear;
                            } else {
                                currentYear = minYearText;
                            }

                            //lastYear = minYear;
                            for (DocumentSnapshot ds: docs) {
                                FirstTeamPlayer ftp = ds.toObject(FirstTeamPlayer.class);
                                assert ftp != null;
                                if (ftp.getId() == 0) {
                                    ftp.setId(maxId+1);
                                    collectionReference.document(ds.getId()).update("id", ftp.getId());
                                    maxId++;
                                }
                            }
                        }
                    }
                });

        collectionReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                FirstTeamPlayer player = doc.toObject(FirstTeamPlayer.class);
                                if (barYear == null || barYear.equals(minYearText)) {
                                    if (player.getYearSigned().equals(minYearText)) {
                                        playerList.add(player);
                                    }
                                } else {
                                    if (player.getYearSigned().equals(barYear)) {
                                        playerList.add(player);
                                    }
                                }
                            }
                            Collections.sort(playerList, new Comparator<FirstTeamPlayer>() {
                                @Override
                                public int compare(FirstTeamPlayer o1, FirstTeamPlayer o2) {
                                    return o1.getTimeAdded().compareTo(o2.getTimeAdded());
                                }
                            });
                            if (barYear == null) {
                                yearText.setText(minYearText);
                                firstTeamPlayerRecAdapter = new FirstTeamPlayerRecAdapter(FirstTeamListActivity.this, playerList, managerId, team, minYearText, 0, maxId);
                                recyclerView.setAdapter(firstTeamPlayerRecAdapter);
                                firstTeamPlayerRecAdapter.notifyDataSetChanged();
                                yearText.setText(currentYear);
                                yearPlayerCount.setText(playerList.size() + " players");
                            } else {
                                yearText.setText(barYear);
                                firstTeamPlayerRecAdapter = new FirstTeamPlayerRecAdapter(FirstTeamListActivity.this, playerList, managerId, team, barYear, 0, maxId);
                                recyclerView.setAdapter(firstTeamPlayerRecAdapter);
                                firstTeamPlayerRecAdapter.notifyDataSetChanged();
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

    private void findMaxPlayerId(List<FirstTeamPlayer> ftplayers) {
        maxId = ftplayers.get(0).getId();
        for (FirstTeamPlayer player: ftplayers) {
            if (player.getId() > maxId) {
                maxId = player.getId();
            }
        }
    }

    private void findMinYearSigned(List<FirstTeamPlayer> ftplayers) {
        if (ftplayers == null || ftplayers.isEmpty()) {
            minYearText = "2020/21";
            return;
        }

        String ySigned = ftplayers.get(0).getYearSigned().substring(0, 4);
        int minYear = Integer.parseInt(ySigned);
        for (FirstTeamPlayer player: ftplayers) {
            int y = Integer.parseInt(player.getYearSigned().substring(0, 4));
            if (y < minYear) {
                minYear = y;
            }
        }
        minYearText = minYear + "/" + ((minYear % 100) + 1);
    }

    @Override
    protected void onResume() {
        super.onResume();

        playerList.clear();
    }
}