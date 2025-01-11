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
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import enumeration.PositionEnum;
import model.Manager;
import model.ShortlistedPlayer;
import ui.ShortlistedPlayerRecAdapter;
import util.UserApi;
import util.ValueFormatter;

public class ShortlistPlayersActivity extends AppCompatActivity {

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
    private FloatingActionButton addPlayerFab;
    private boolean ftPlayersExist;
    private boolean ytPlayersExist;
    private AlertDialog.Builder builder;
    private AlertDialog dialog;

    private EditText firstName;
    private EditText lastName;
    private Spinner positionSpinner;
    private EditText team;
    private EditText nationality;
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

    private TextView managerNameHeader;
    private TextView teamHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shortlist_players);

        if (UserApi.getInstance() != null) {
            currentUserId = UserApi.getInstance().getUserId();
            currentUserName = UserApi.getInstance().getUsername();
        }

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            managerId = extras.getLong("managerId");
            myTeam = extras.getString("team");
            barPosition = extras.getString("barPosition");
        }

        playerList = new ArrayList<>();
        recyclerView = findViewById(R.id.rec_view_shp);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

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

        prevPositionButton = findViewById(R.id.prev_position_button_shp);
        nextPositionButton = findViewById(R.id.next_position_button_shp);
        positionText = findViewById(R.id.position_text_shp);
        addPlayerFab = findViewById(R.id.add_new_sh_player_button);
        positionText.setText(PositionEnum.GK.getCategory());

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> {});

        // Load Banner Ads
        AdView shortlistBanner = findViewById(R.id.shortlist_banner);
        AdRequest adBannerRequest = new AdRequest.Builder().build();
        shortlistBanner.loadAd(adBannerRequest);

        addPlayerFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

    private void changePositionForNextButton() {
        String pos = positionText.getText().toString();
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
        }
        pos = positionText.getText().toString();
        listPlayers(pos, 2);
    }

    private void changePositionForPreviousButton() {
        String pos = positionText.getText().toString();
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
        }
        pos = positionText.getText().toString();
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
        builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.create_shortlisted_player_popup, null);

        firstName = view.findViewById(R.id.first_name_shp_create);
        lastName = view.findViewById(R.id.last_name_shp_create);
        positionSpinner = view.findViewById(R.id.position_spinner_shp_create);
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
                        }
                    }
                });

        ArrayAdapter<CharSequence> positionAdapter = ArrayAdapter.createFromResource(this, R.array.position_array, android.R.layout.simple_spinner_item);
        positionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        positionSpinner.setAdapter(positionAdapter);

        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!lastName.getText().toString().isEmpty() &&
                        !nationality.getText().toString().isEmpty() &&
                        !positionSpinner.getSelectedItem().toString().isEmpty() &&
                        !team.getText().toString().isEmpty() &&
                        !overall.getText().toString().isEmpty()) {
                    createPlayer();
                } else {
                    Toast.makeText(ShortlistPlayersActivity.this, "Last Name/Nickname, Nationality, Position, Team and Overall are required", Toast.LENGTH_LONG)
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
        final String positionPlayer = positionSpinner.getSelectedItem().toString().trim();
        String nationalityPlayer = nationality.getText().toString().trim();
        String overallPlayer = overall.getText().toString().trim();
        String potLowPlayer = potLow.getText().toString().trim();
        String potHighPlayer = potHigh.getText().toString().trim();
        String teamPlayer = team.getText().toString().trim();
        String valuePlayer = value.getText().toString().trim().replaceAll(",", "");
        String wagePlayer = wage.getText().toString().trim().replaceAll(",", "");
        String commentsPlayer = comments.getText().toString().trim();

        final ShortlistedPlayer player = new ShortlistedPlayer();

        player.setId(maxId+1);
        player.setFirstName(firstNamePlayer);
        player.setLastName(lastNamePlayer);
        player.setFullName(fullNamePlayer);
        player.setPosition(positionPlayer);
        player.setNationality(nationalityPlayer);
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

        shPlayersColRef.add(player)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Intent intent = new Intent(ShortlistPlayersActivity.this, ShortlistPlayersActivity.class);
                        intent.putExtra("managerId", managerId);
                        intent.putExtra("team", myTeam);
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
                        intent.putExtra("barPosition", barPosition);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    private void listPlayers(final String position, final int buttonInt) {
        playerList.clear();
        shPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
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
                            Collections.sort(playerList, new Comparator<ShortlistedPlayer>() {
                                @Override
                                public int compare(ShortlistedPlayer o1, ShortlistedPlayer o2) {
                                    return o1.getTimeAdded().compareTo(o2.getTimeAdded());
                                }
                            });
                            shortlistedPlayerRecAdapter = new ShortlistedPlayerRecAdapter(ShortlistPlayersActivity.this, playerList, managerId, myTeam, position, buttonInt);
                            recyclerView.setAdapter(shortlistedPlayerRecAdapter);
                            shortlistedPlayerRecAdapter.notifyDataSetChanged();
                        }
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

        ytPlayersColRef.whereEqualTo("userId", currentUserId)
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            ytPlayersExist = !Objects.requireNonNull(task.getResult()).isEmpty();
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
                            playerList.sort(new Comparator<ShortlistedPlayer>() {
                                @Override
                                public int compare(ShortlistedPlayer player1, ShortlistedPlayer player2) {
                                    return player1.getTimeAdded().compareTo(player2.getTimeAdded());
                                }
                            });
                            if (barPosition == null) {
                                positionText.setText(PositionEnum.GK.getCategory());
                                shortlistedPlayerRecAdapter = new ShortlistedPlayerRecAdapter(ShortlistPlayersActivity.this, playerList, managerId, myTeam, PositionEnum.GK.getCategory(), 0);
                            } else {
                                positionText.setText(barPosition);
                                shortlistedPlayerRecAdapter = new ShortlistedPlayerRecAdapter(ShortlistPlayersActivity.this, playerList, managerId, myTeam, barPosition, 0);
                            }
                            recyclerView.setAdapter(shortlistedPlayerRecAdapter);
                            shortlistedPlayerRecAdapter.notifyDataSetChanged();

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
                                }
                            }
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
                            }
                            Manager theManager = managerList.get(0);
                            managerNameHeader.setText(theManager.getFullName());
                            teamHeader.setText(theManager.getTeam());
                        }
                    }
                });
    }

    private void findMaxPlayerId(List<ShortlistedPlayer> shPlayers) {
        maxId = shPlayers.get(0).getId();
        for (ShortlistedPlayer player: shPlayers) {
            if (player.getId() > maxId) {
                maxId = player.getId();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        playerList.clear();
    }
}