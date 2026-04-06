package com.dimxlp.managerdb;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import model.FirstTeamPlayer;
import model.Manager;
import model.ShortlistedPlayer;
import model.Transfer;
import model.YouthTeamPlayer;
import ui.DashboardShortlistAdapter;
import ui.DashboardTransferAdapter;
import ui.ManagerRecyclerAdapter;
import util.ManageTeamButton;
import util.UserApi;

public class DashboardActivity extends AppCompatActivity {

    private static final String LOG_TAG = "RAFI|Dashboard";
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navView;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference managersCollectionRef = db.collection("Managers");
    private CollectionReference ftPlayersCollectionRef = db.collection("FirstTeamPlayers");
    private CollectionReference ytPlayersCollectionRef = db.collection("YouthTeamPlayers");
    private CollectionReference shPlayersCollectionRef = db.collection("ShortlistedPlayers");
    private CollectionReference transfersCollectionRef = db.collection("Transfers");

    private boolean ftPlayersExist;
    private boolean ytPlayersExist;
    private boolean shPlayersExist;

    private List<ManageTeamButton> buttonList;
    private RecyclerView quickActionsRecycler;
    private RecyclerView recentTransfersRecycler;
    private RecyclerView shortlistRecycler;
    private ManagerRecyclerAdapter managerRecyclerAdapter;
    private DashboardTransferAdapter transferAdapter;
    private DashboardShortlistAdapter shortlistAdapter;

    private TextView teamName;
    private TextView managerName;
    private TextView firstTeamCount;
    private TextView youthTeamCount;
    private TextView noTransfersText;
    private TextView noShortlistText;
    private TextView viewAllTransfers;
    private TextView viewAllShortlist;
    
    private CardView cardFirstTeam;
    private CardView cardYouthTeam;

    private long managerId;
    private String team;

    private TextView managerNameHeader;
    private TextView teamHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        Log.i(LOG_TAG, "DashboardActivity launched.");

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            managerId = extras.getLong("managerId");
            team = extras.getString("team");
            Log.d(LOG_TAG, "Extras received: managerId=" + managerId + ", team=" + team);
        } else {
            Log.w(LOG_TAG, "No extras received in intent.");
        }

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        if (user != null) {
            Log.d(LOG_TAG, "User authenticated: " + user.getUid());
        } else {
            Log.w(LOG_TAG, "No user authenticated.");
        }

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_menu);

        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nvView_manage);
        setUpDrawerContent(navView);

        // Initialize views
        teamName = findViewById(R.id.team_name);
        managerName = findViewById(R.id.manager_name);
        firstTeamCount = findViewById(R.id.first_team_count);
        youthTeamCount = findViewById(R.id.youth_team_count);
        noTransfersText = findViewById(R.id.no_transfers_text);
        noShortlistText = findViewById(R.id.no_shortlist_text);
        viewAllTransfers = findViewById(R.id.view_all_transfers);
        viewAllShortlist = findViewById(R.id.view_all_shortlist);
        cardFirstTeam = findViewById(R.id.card_first_team);
        cardYouthTeam = findViewById(R.id.card_youth_team);

        // Initialize RecyclerViews
        recentTransfersRecycler = findViewById(R.id.recent_transfers_recycler);
        recentTransfersRecycler.setLayoutManager(new LinearLayoutManager(this));
        recentTransfersRecycler.setNestedScrollingEnabled(false);

        shortlistRecycler = findViewById(R.id.shortlist_recycler);
        shortlistRecycler.setLayoutManager(new LinearLayoutManager(this));
        shortlistRecycler.setNestedScrollingEnabled(false);

        quickActionsRecycler = findViewById(R.id.quick_actions_recycler);
        quickActionsRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        quickActionsRecycler.setNestedScrollingEnabled(false);

        // Set up header
        View headerLayout = null;
        if (navView.getHeaderCount() > 0) {
            headerLayout = navView.getHeaderView(0);
        }
        managerNameHeader = headerLayout.findViewById(R.id.manager_name_header);
        teamHeader = headerLayout.findViewById(R.id.team_name_header);

        // Set up click listeners
        setupClickListeners();

        // Initialize button list for quick actions
        buttonList = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            buttonList.add(new ManageTeamButton());
        }
    }

    private void setupClickListeners() {
        viewAllTransfers.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, TransferDealsActivity.class);
            intent.putExtra("managerId", managerId);
            intent.putExtra("team", team);
            startActivity(intent);
        });

        viewAllShortlist.setOnClickListener(v -> {
            if (shPlayersExist) {
                Intent intent = new Intent(DashboardActivity.this, ShortlistPlayersActivity.class);
                intent.putExtra("managerId", managerId);
                intent.putExtra("team", team);
                startActivity(intent);
            } else {
                Intent intent = new Intent(DashboardActivity.this, ShortlistActivity.class);
                intent.putExtra("managerId", managerId);
                intent.putExtra("team", team);
                startActivity(intent);
            }
        });

        cardFirstTeam.setOnClickListener(v -> {
            if (ftPlayersExist) {
                Intent intent = new Intent(DashboardActivity.this, FirstTeamListActivity.class);
                intent.putExtra("managerId", managerId);
                intent.putExtra("team", team);
                startActivity(intent);
            } else {
                Intent intent = new Intent(DashboardActivity.this, FirstTeamActivity.class);
                intent.putExtra("managerId", managerId);
                intent.putExtra("team", team);
                startActivity(intent);
            }
        });

        cardYouthTeam.setOnClickListener(v -> {
            if (ytPlayersExist) {
                Intent intent = new Intent(DashboardActivity.this, YouthTeamListActivity.class);
                intent.putExtra("managerId", managerId);
                intent.putExtra("team", team);
                startActivity(intent);
            } else {
                Intent intent = new Intent(DashboardActivity.this, YouthTeamActivity.class);
                intent.putExtra("managerId", managerId);
                intent.putExtra("team", team);
                startActivity(intent);
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
                drawerLayout.closeDrawers();
                break;
            case R.id.dr_manager_selection:
                Intent managerSelectionIntent = new Intent(DashboardActivity.this, SelectManagerActivity.class);
                startActivity(managerSelectionIntent);
                break;
            case R.id.dr_profile:
                Intent profileIntent = new Intent(DashboardActivity.this, ProfileActivity.class);
                profileIntent.putExtra("managerId", managerId);
                profileIntent.putExtra("team", team);
                startActivity(profileIntent);
                break;
            case R.id.dr_first_team:
                if (ftPlayersExist) {
                    Intent firstIntent = new Intent(DashboardActivity.this, FirstTeamListActivity.class);
                    firstIntent.putExtra("managerId", managerId);
                    firstIntent.putExtra("team", team);
                    startActivity(firstIntent);
                } else {
                    Intent firstIntent = new Intent(DashboardActivity.this, FirstTeamActivity.class);
                    firstIntent.putExtra("managerId", managerId);
                    firstIntent.putExtra("team", team);
                    startActivity(firstIntent);
                }
                break;
            case R.id.dr_youth_team:
                if (ytPlayersExist) {
                    Intent youthIntent = new Intent(DashboardActivity.this, YouthTeamListActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", team);
                    startActivity(youthIntent);
                } else {
                    Intent youthIntent = new Intent(DashboardActivity.this, YouthTeamActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", team);
                    startActivity(youthIntent);
                }
                break;
            case R.id.dr_former_players:
                Intent formerIntent = new Intent(DashboardActivity.this, FormerPlayersListActivity.class);
                formerIntent.putExtra("managerId", managerId);
                formerIntent.putExtra("team", team);
                startActivity(formerIntent);
                break;
            case R.id.dr_shortlist:
                if (shPlayersExist) {
                    Intent shortlistIntent = new Intent(DashboardActivity.this, ShortlistPlayersActivity.class);
                    shortlistIntent.putExtra("managerId", managerId);
                    shortlistIntent.putExtra("team", team);
                    startActivity(shortlistIntent);
                } else {
                    Intent shortlistIntent = new Intent(DashboardActivity.this, ShortlistActivity.class);
                    shortlistIntent.putExtra("managerId", managerId);
                    shortlistIntent.putExtra("team", team);
                    startActivity(shortlistIntent);
                }
                break;
            case R.id.dr_loaned_out_players:
                Intent loanIntent = new Intent(DashboardActivity.this, LoanedOutPlayersActivity.class);
                loanIntent.putExtra("managerId", managerId);
                loanIntent.putExtra("team", team);
                startActivity(loanIntent);
                break;
            case R.id.dr_transfer_deals:
                Intent transferIntent = new Intent(DashboardActivity.this, TransferDealsActivity.class);
                transferIntent.putExtra("managerId", managerId);
                transferIntent.putExtra("team", team);
                startActivity(transferIntent);
                break;
            case R.id.dr_support_info:
                Intent supportIntent = new Intent(DashboardActivity.this, SupportActivity.class);
                supportIntent.putExtra("managerId", managerId);
                supportIntent.putExtra("team", team);
                startActivity(supportIntent);
                break;
            case R.id.dr_logout:
                if (user != null && firebaseAuth != null) {
                    firebaseAuth.signOut();
                    Intent mainIntent = new Intent(DashboardActivity.this, MainActivity.class);
                    startActivity(mainIntent);
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
        Log.d(LOG_TAG, "onStart called: Fetching dashboard data.");

        // Fetch manager data
        fetchManagerData();
        
        // Fetch squad counts
        fetchSquadCounts();
        
        // Fetch recent transfers
        fetchRecentTransfers();
        
        // Fetch shortlisted players
        fetchShortlistedPlayers();
        
        // Set up quick actions
        setupQuickActions();
    }

    private void fetchManagerData() {
        managersCollectionRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("id", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot queryDocumentSnapshots = task.getResult();
                            if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                                List<DocumentSnapshot> documentSnapshotList = queryDocumentSnapshots.getDocuments();
                                List<Manager> managerList = new ArrayList<>();
                                for (DocumentSnapshot doc : documentSnapshotList) {
                                    Manager manager = doc.toObject(Manager.class);
                                    managerList.add(manager);
                                    if (manager != null) {
                                        Log.d(LOG_TAG, "Manager data fetched: " + manager.getFullName());
                                    }
                                }
                                if (!managerList.isEmpty()) {
                                    Manager manager = managerList.get(0);
                                    teamName.setText(manager.getTeam().toUpperCase());
                                    managerName.setText(manager.getFullName().toUpperCase());
                                    managerNameHeader.setText(manager.getFullName());
                                    teamHeader.setText(manager.getTeam());
                                } else {
                                    Log.w(LOG_TAG, "No manager data found.");
                                }
                            }
                        } else {
                            Log.e(LOG_TAG, "Error fetching manager data.", task.getException());
                        }
                    }
                });
    }

    private void fetchSquadCounts() {
        // Fetch First Team count
        ftPlayersCollectionRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            int count = task.getResult().size();
                            ftPlayersExist = count > 0;
                            firstTeamCount.setText(count + (count == 1 ? " Player" : " Players"));
                            Log.d(LOG_TAG, "First Team players count: " + count);
                        } else {
                            ftPlayersExist = false;
                            firstTeamCount.setText("No Players");
                            Log.e(LOG_TAG, "Error fetching First Team players.", task.getException());
                        }
                    }
                });

        // Fetch Youth Team count
        ytPlayersCollectionRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            int count = task.getResult().size();
                            ytPlayersExist = count > 0;
                            youthTeamCount.setText(count + (count == 1 ? " Future Star" : " Future Stars"));
                            Log.d(LOG_TAG, "Youth Team players count: " + count);
                        } else {
                            ytPlayersExist = false;
                            youthTeamCount.setText("No Players");
                            Log.e(LOG_TAG, "Error fetching Youth Team players.", task.getException());
                        }
                    }
                });
    }

    private void fetchRecentTransfers() {
        transfersCollectionRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .orderBy("timeAdded", Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            List<Transfer> transfers = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                Transfer transfer = doc.toObject(Transfer.class);
                                transfers.add(transfer);
                            }
                            
                            if (transfers.isEmpty()) {
                                recentTransfersRecycler.setVisibility(View.GONE);
                                noTransfersText.setVisibility(View.VISIBLE);
                                Log.d(LOG_TAG, "No recent transfers found.");
                            } else {
                                recentTransfersRecycler.setVisibility(View.VISIBLE);
                                noTransfersText.setVisibility(View.GONE);
                                transferAdapter = new DashboardTransferAdapter(DashboardActivity.this, transfers);
                                recentTransfersRecycler.setAdapter(transferAdapter);
                                Log.d(LOG_TAG, "Recent transfers loaded: " + transfers.size());
                            }
                        } else {
                            recentTransfersRecycler.setVisibility(View.GONE);
                            noTransfersText.setVisibility(View.VISIBLE);
                            Log.e(LOG_TAG, "Error fetching recent transfers.", task.getException());
                        }
                    }
                });
    }

    private void fetchShortlistedPlayers() {
        shPlayersCollectionRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .orderBy("overall", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            List<ShortlistedPlayer> players = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                ShortlistedPlayer player = doc.toObject(ShortlistedPlayer.class);
                                players.add(player);
                            }
                            
                            shPlayersExist = !players.isEmpty();
                            
                            if (players.isEmpty()) {
                                shortlistRecycler.setVisibility(View.GONE);
                                noShortlistText.setVisibility(View.VISIBLE);
                                Log.d(LOG_TAG, "No shortlisted players found.");
                            } else {
                                shortlistRecycler.setVisibility(View.VISIBLE);
                                noShortlistText.setVisibility(View.GONE);
                                shortlistAdapter = new DashboardShortlistAdapter(DashboardActivity.this, players);
                                shortlistRecycler.setAdapter(shortlistAdapter);
                                Log.d(LOG_TAG, "Shortlisted players loaded: " + players.size());
                            }
                        } else {
                            shPlayersExist = false;
                            shortlistRecycler.setVisibility(View.GONE);
                            noShortlistText.setVisibility(View.VISIBLE);
                            Log.e(LOG_TAG, "Error fetching shortlisted players.", task.getException());
                        }
                    }
                });
    }

    private void setupQuickActions() {
        managerRecyclerAdapter = new ManagerRecyclerAdapter(DashboardActivity.this, buttonList, ftPlayersExist, ytPlayersExist, shPlayersExist, managerId, team);
        quickActionsRecycler.setAdapter(managerRecyclerAdapter);
        managerRecyclerAdapter.notifyDataSetChanged();
    }
}

