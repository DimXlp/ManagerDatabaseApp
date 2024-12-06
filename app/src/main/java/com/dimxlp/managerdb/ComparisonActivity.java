package com.dimxlp.managerdb;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import model.FirstTeamPlayer;
import model.YouthTeamPlayer;
import ui.ComparePlayerSpinnerAdapter;
import util.UserApi;

public class ComparisonActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference firstTeamPlayersReference = db.collection("FirstTeamPlayers");
    private CollectionReference youthTeamPlayersReference = db.collection("YouthTeamPlayers");
    private CollectionReference formerPlayersReference = db.collection("FormerPlayers");
    private CollectionReference loanedOutPlayersReference = db.collection("LoanedOutPlayers");
    private CollectionReference shortlistedPlayersReference = db.collection("ShortlistedPlayers");

    private String currentUserId;
    private String currentUserName;

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navView;

    private Spinner player1Spinner;
    private Spinner player2Spinner;
    private EditText player1Position;
    private EditText player2Position;
    private ProgressBar player1AgeBar;
    private EditText player1AgeStat;
    private EditText player2AgeStat;
    private ProgressBar player2AgeBar;
    private ProgressBar player1OverallBar;
    private EditText player1OverallStat;
    private EditText player2OverallStat;
    private ProgressBar player2OverallBar;
    private ProgressBar player1PotentialLowBar;
    private EditText player1PotentialLowStat;
    private EditText player2PotentialLowStat;
    private ProgressBar player2PotentialLowBar;
    private ProgressBar player1PotentialHighBar;
    private EditText player1PotentialHighStat;
    private EditText player2PotentialHighStat;
    private ProgressBar player2PotentialHighBar;
    private Button compare;

    private String team;
    private long managerId;
    private TextView managerNameHeader;
    private TextView teamHeader;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comparison);

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
        }

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

        player1Spinner = findViewById(R.id.player1_spinner);
        player2Spinner = findViewById(R.id.player2_spinner);

        populatePlayerSpinners();
        
    }

    private void populatePlayerSpinners() {
        final List<String> playersList = new ArrayList<>();

        // Fetch first team players
        firstTeamPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get().addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        queryDocumentSnapshots.getDocuments().stream()
                                .map(doc -> Objects.requireNonNull(doc.toObject(FirstTeamPlayer.class)).getFullName())
                                .forEach(playersList::add);
                        Log.d("populatePlayerSpinners", "First Team Players Fetched: " + playersList.size());
                    }

                    // After fetching first team players, fetch youth team players
                    youthTeamPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                            .whereEqualTo("managerId", managerId)
                            .get().addOnSuccessListener(youthQuerySnapshots -> {
                                if (!youthQuerySnapshots.isEmpty()) {
                                    youthQuerySnapshots.getDocuments().stream()
                                            .map(doc -> Objects.requireNonNull(doc.toObject(YouthTeamPlayer.class)).getFullName())
                                            .forEach(playersList::add);
                                    Log.d("populatePlayerSpinners", "Youth Team Players Fetched: " + playersList.size());
                                }

                                // Now both queries are complete, set the adapter
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(ComparisonActivity.this, android.R.layout.simple_spinner_item, playersList);
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                player1Spinner.setAdapter(adapter);
                                player2Spinner.setAdapter(adapter);
                            });
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
//        switch (item.getItemId()) {
//            case R.id.dr_home:
//                Intent homeIntent = new Intent(ComparisonActivity.this, ManageTeamActivity.class);
//                homeIntent.putExtra("managerId", managerId);
//                homeIntent.putExtra("team", team);
//                startActivity(homeIntent);
//                finish();
//                break;
//            case R.id.dr_profile:
//                Intent profileIntent = new Intent(ComparisonActivity.this, ProfileActivity.class);
//                profileIntent.putExtra("managerId", managerId);
//                profileIntent.putExtra("team", team);
//                startActivity(profileIntent);
//                finish();
//                break;
//            case R.id.dr_first_team:
//                break;
//            case R.id.dr_youth_team:
//                if (ytPlayersExist) {
//                    Intent youthIntent = new Intent(ComparisonActivity.this, YouthTeamListActivity.class);
//                    youthIntent.putExtra("managerId", managerId);
//                    youthIntent.putExtra("team", team);
//                    startActivity(youthIntent);
//                    finish();
//                } else {
//                    Intent youthIntent = new Intent(ComparisonActivity.this, YouthTeamActivity.class);
//                    youthIntent.putExtra("managerId", managerId);
//                    youthIntent.putExtra("team", team);
//                    startActivity(youthIntent);
//                    finish();
//                }
//                break;
//            case R.id.dr_former_players:
//                Intent formerIntent = new Intent(ComparisonActivity.this, FormerPlayersListActivity.class);
//                formerIntent.putExtra("managerId", managerId);
//                formerIntent.putExtra("team", team);
//                startActivity(formerIntent);
//                finish();
//                break;
//            case R.id.dr_shortlist:
//                if (shPlayersExist) {
//                    Intent shortlistIntent = new Intent(ComparisonActivity.this, ShortlistPlayersActivity.class);
//                    shortlistIntent.putExtra("managerId", managerId);
//                    shortlistIntent.putExtra("team", team);
//                    startActivity(shortlistIntent);
//                    finish();
//                } else {
//                    Intent shortlistIntent = new Intent(ComparisonActivity.this, ShortlistActivity.class);
//                    shortlistIntent.putExtra("managerId", managerId);
//                    shortlistIntent.putExtra("team", team);
//                    startActivity(shortlistIntent);
//                    finish();
//                }
//                break;
//            case R.id.dr_loaned_out_players:
//                Intent loanIntent = new Intent(ComparisonActivity.this, LoanedOutPlayersActivity.class);
//                loanIntent.putExtra("managerId", managerId);
//                loanIntent.putExtra("team", team);
//                startActivity(loanIntent);
//                finish();
//                break;
//            case R.id.dr_transfer_deals:
//                Intent transferIntent = new Intent(ComparisonActivity.this, TransferDealsActivity.class);
//                transferIntent.putExtra("managerId", managerId);
//                transferIntent.putExtra("team", team);
//                startActivity(transferIntent);
//                finish();
//                break;
//            case R.id.dr_logout:
//                if (user != null && firebaseAuth != null) {
//                    firebaseAuth.signOut();
//                    startActivity(new Intent(ComparisonActivity.this, MainActivity.class));
//                    finishAffinity();
//                }
//                break;
//        }

        item.setChecked(true);
        setTitle(item.getTitle());
        drawerLayout.closeDrawers();
    }
}
