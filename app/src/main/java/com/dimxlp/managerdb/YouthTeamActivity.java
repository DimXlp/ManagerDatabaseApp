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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import model.Manager;
import model.YouthTeamPlayer;
import util.UserApi;

public class YouthTeamActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navView;

    private FloatingActionButton addPlayerFab;
    private TextView addPlayerText;

    private AlertDialog.Builder builder;
    private AlertDialog dialog;

    private EditText firstName;
    private EditText lastName;
    private Spinner positionSpinner;
    private EditText number;
    private EditText nationality;
    private EditText overall;
    private EditText potentialLow;
    private EditText potentialHigh;
    private Spinner yearScouted;
    private Button createPlayerButton;

    private String currentUserId;
    private String currentUserName;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("YouthTeamPlayers");
    private boolean ftPlayersExist;
    private boolean shPlayersExist;
    private long managerId;
    private String team;

    private TextView managerNameHeader;
    private TextView teamHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youth_team);

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
            Log.d("RAFI", "managerId = " + managerId + "\nteam = " + team);
        }

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_menu);

        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nvView_yt);
        setUpDrawerContent(navView);

        View headerLayout = null;
        if (navView.getHeaderCount() > 0) {
            headerLayout = navView.getHeaderView(0);
        }
        managerNameHeader = headerLayout.findViewById(R.id.manager_name_header);
        teamHeader = headerLayout.findViewById(R.id.team_name_header);

        addPlayerFab = findViewById(R.id.add_player_button_yt);
        addPlayerText = findViewById(R.id.add_player_text_yt);

        addPlayerFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createPopupDialog();
            }
        });
    }

    private void createPopupDialog() {
        builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.create_youth_team_player_popup, null);

        firstName = view.findViewById(R.id.first_name_ytp_create);
        lastName = view.findViewById(R.id.last_name_ytp_create);
        positionSpinner = view.findViewById(R.id.position_spinner_ytp_create);
        number = view.findViewById(R.id.number_ytp_create);
        nationality = view.findViewById(R.id.nationality_ytp_create);
        overall = view.findViewById(R.id.overall_ytp_create);
        potentialLow = view.findViewById(R.id.potential_low_ytp_create);
        potentialHigh = view.findViewById(R.id.potential_high__ytp_create);
        yearScouted = view.findViewById(R.id.year_scouted_spinner_ytp_create);
        createPlayerButton = view.findViewById(R.id.create_yt_player_button);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.position_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        positionSpinner.setAdapter(adapter);

        ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(this, R.array.years_array, android.R.layout.simple_spinner_item);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearScouted.setAdapter(yearAdapter);

        positionSpinner.setOnItemSelectedListener(new SpinnerActivity());

        createPlayerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!lastName.getText().toString().isEmpty() &&
                        !nationality.getText().toString().isEmpty() &&
                        !positionSpinner.getSelectedItem().toString().isEmpty() &&
                        !overall.getText().toString().isEmpty() &&
                        !yearScouted.getSelectedItem().toString().equals("0")) {
                    createPlayer();
                } else {
                    Toast.makeText(YouthTeamActivity.this, "Last Name/Nickname, Nationality, Position, Overall and Year Scouted are required", Toast.LENGTH_LONG)
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
        String positionPlayer = positionSpinner.getSelectedItem().toString().trim();
        String numberPlayer = number.getText().toString().trim();
        String nationalityPlayer = nationality.getText().toString().trim();
        String overallPlayer = overall.getText().toString().trim();
        String potentialLowPlayer = potentialLow.getText().toString().trim();
        String potentialHiPlayer = potentialHigh.getText().toString().trim();
        final String yScoutedPlayer = yearScouted.getSelectedItem().toString().trim();

        YouthTeamPlayer player = new YouthTeamPlayer();

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
        player.setNationality(nationalityPlayer);
        player.setOverall(Integer.parseInt(overallPlayer));
        if (!potentialLowPlayer.isEmpty()) {
            player.setPotentialLow(Integer.parseInt(potentialLowPlayer));
        }
        if (!potentialHiPlayer.isEmpty()) {
            player.setPotentialHigh(Integer.parseInt(potentialHiPlayer));
        }
        player.setYearScouted(yScoutedPlayer);
        player.setManagerId(managerId);
        player.setUserId(currentUserId);
        player.setTimeAdded(new Timestamp(new Date()));

        collectionReference.add(player)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Intent intent = new Intent(YouthTeamActivity.this, YouthTeamListActivity.class);
                        intent.putExtra("managerId", managerId);
                        intent.putExtra("team", team);
                        intent.putExtra("barYear", yScoutedPlayer);
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
                Intent homeIntent = new Intent(YouthTeamActivity.this, ManageTeamActivity.class);
                homeIntent.putExtra("managerId", managerId);
                homeIntent.putExtra("team", team);
                startActivity(homeIntent);
                finish();
                break;
            case R.id.dr_manager_selection:
                Intent managerSelectionIntent = new Intent(YouthTeamActivity.this, SelectManagerActivity.class);
                startActivity(managerSelectionIntent);
                break;
            case R.id.dr_profile:
                Intent profileIntent = new Intent(YouthTeamActivity.this, ProfileActivity.class);
                profileIntent.putExtra("managerId", managerId);
                profileIntent.putExtra("team", team);
                startActivity(profileIntent);
                finish();
                break;
            case R.id.dr_first_team:
                if (ftPlayersExist) {
                    Intent firstIntent = new Intent(YouthTeamActivity.this, FirstTeamListActivity.class);
                    firstIntent.putExtra("managerId", managerId);
                    firstIntent.putExtra("team", team);
                    startActivity(firstIntent);
                    finish();
                } else {
                    Intent firstIntent = new Intent(YouthTeamActivity.this, FirstTeamActivity.class);
                    firstIntent.putExtra("managerId", managerId);
                    firstIntent.putExtra("team", team);
                    startActivity(firstIntent);
                    finish();
                }
                break;
            case R.id.dr_youth_team:
                break;
            case R.id.dr_former_players:
                Intent formerIntent = new Intent(YouthTeamActivity.this, FormerPlayersListActivity.class);
                formerIntent.putExtra("managerId", managerId);
                formerIntent.putExtra("team", team);
                startActivity(formerIntent);
                finish();
                break;
            case R.id.dr_shortlist:
                if (shPlayersExist) {
                    Intent shortlistIntent = new Intent(YouthTeamActivity.this, ShortlistPlayersActivity.class);
                    shortlistIntent.putExtra("managerId", managerId);
                    shortlistIntent.putExtra("team", team);
                    startActivity(shortlistIntent);
                    finish();
                } else {
                    Intent shortlistIntent = new Intent(YouthTeamActivity.this, ShortlistActivity.class);
                    shortlistIntent.putExtra("managerId", managerId);
                    shortlistIntent.putExtra("team", team);
                    startActivity(shortlistIntent);
                    finish();
                }
                break;
            case R.id.dr_loaned_out_players:
                Intent loanIntent = new Intent(YouthTeamActivity.this, LoanedOutPlayersActivity.class);
                loanIntent.putExtra("managerId", managerId);
                loanIntent.putExtra("team", team);
                startActivity(loanIntent);
                finish();
                break;
            case R.id.dr_transfer_deals:
                Intent transferIntent = new Intent(YouthTeamActivity.this, TransferDealsActivity.class);
                transferIntent.putExtra("managerId", managerId);
                transferIntent.putExtra("team", team);
                startActivity(transferIntent);
                finish();
                break;
            case R.id.dr_logout:
                if (user != null && firebaseAuth != null) {
                    firebaseAuth.signOut();
                    startActivity(new Intent(YouthTeamActivity.this, MainActivity.class));
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

        db.collection("FirstTeamPlayers").whereEqualTo("userId", currentUserId)
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (Objects.requireNonNull(task.getResult()).size() > 0) {
                                ftPlayersExist = true;
                            } else {
                                ftPlayersExist = false;
                            }
                        }
                    }
                });

        db.collection("Managers").whereEqualTo("userId", UserApi.getInstance().getUserId())
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

    public static class SpinnerActivity extends FirstTeamActivity implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }
}