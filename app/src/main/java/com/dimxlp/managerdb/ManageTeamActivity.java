package com.dimxlp.managerdb;

import androidx.annotation.NonNull;
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
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import model.Manager;
import ui.ManagerRecyclerAdapter;
import util.ManageTeamButton;
import util.UserApi;

public class ManageTeamActivity extends AppCompatActivity {

    private static final String LOG_TAG = "RAFI|ManageTeam";
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navView;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private StorageReference storageReference;
    private CollectionReference managersCollectionRef = db.collection("Managers");
    private CollectionReference ftPlayersCollectionRef = db.collection("FirstTeamPlayers");
    private CollectionReference ytPlayersCollectionRef = db.collection("YouthTeamPlayers");
    private CollectionReference shPlayersCollectionRef = db.collection("ShortlistedPlayers");

    private boolean ftPlayersExist;
    private boolean ytPlayersExist;
    private boolean shPlayersExist;

    private List<ManageTeamButton> buttonList;
    private RecyclerView recyclerView;
    private ManagerRecyclerAdapter managerRecyclerAdapter;

    private TextView teamName;
    private TextView managerName;

    private long managerId;
    private String team;

    private TextView managerNameHeader;
    private TextView teamHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_team);
        Log.i(LOG_TAG, "ManageTeamActivity launched.");

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

        buttonList = new ArrayList<>();
        for (int i=0; i<8; i++) {
            buttonList.add(new ManageTeamButton());
        }

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        teamName = findViewById(R.id.team_name);
        managerName = findViewById(R.id.manager_name);

        View headerLayout = null;
        if (navView.getHeaderCount() > 0) {
            headerLayout = navView.getHeaderView(0);
        }
        managerNameHeader = headerLayout.findViewById(R.id.manager_name_header);
        teamHeader = headerLayout.findViewById(R.id.team_name_header);

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> Log.d(LOG_TAG, "Mobile Ads SDK initialized."));

        // Load Banner Ad
        AdView manageBanner = findViewById(R.id.manage_banner);
        AdRequest adBannerRequest = new AdRequest.Builder().build();
        manageBanner.loadAd(adBannerRequest);

        // Load Interstitial Ad
        InterstitialAd.load(this, "ca-app-pub-8349697523222717/9777554763", new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        interstitialAd.show(ManageTeamActivity.this);
                        Log.d(LOG_TAG, "Interstitial ad loaded and displayed.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(LOG_TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
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
                Intent homeIntent = new Intent(ManageTeamActivity.this, ManageTeamActivity.class);
                homeIntent.putExtra("managerId", managerId);
                homeIntent.putExtra("team", team);
                startActivity(homeIntent);
                finish();
                break;
            case R.id.dr_manager_selection:
                Intent managerSelectionIntent = new Intent(ManageTeamActivity.this, SelectManagerActivity.class);
                startActivity(managerSelectionIntent);
                break;
            case R.id.dr_profile:
                Intent profileIntent = new Intent(ManageTeamActivity.this, ProfileActivity.class);
                profileIntent.putExtra("managerId", managerId);
                profileIntent.putExtra("team", team);
                startActivity(profileIntent);
                finish();
                break;
            case R.id.dr_first_team:
                if (ftPlayersExist) {
                    Intent firstIntent = new Intent(ManageTeamActivity.this, FirstTeamListActivity.class);
                    firstIntent.putExtra("managerId", managerId);
                    firstIntent.putExtra("team", team);
                    startActivity(firstIntent);
                    finish();
                } else {
                    Intent firstIntent = new Intent(ManageTeamActivity.this, FirstTeamActivity.class);
                    firstIntent.putExtra("managerId", managerId);
                    firstIntent.putExtra("team", team);
                    startActivity(firstIntent);
                    finish();
                }
                break;
            case R.id.dr_youth_team:
                if (ytPlayersExist) {
                    Intent youthIntent = new Intent(ManageTeamActivity.this, YouthTeamListActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", team);
                    startActivity(youthIntent);
                    finish();
                } else {
                    Intent youthIntent = new Intent(ManageTeamActivity.this, YouthTeamActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", team);
                    startActivity(youthIntent);
                    finish();
                }
                break;
            case R.id.dr_former_players:
                Intent formerIntent = new Intent(ManageTeamActivity.this, FormerPlayersListActivity.class);
                formerIntent.putExtra("managerId", managerId);
                formerIntent.putExtra("team", team);
                startActivity(formerIntent);
                finish();
                break;
            case R.id.dr_shortlist:
                if (shPlayersExist) {
                    Intent shortlistIntent = new Intent(ManageTeamActivity.this, ShortlistPlayersActivity.class);
                    shortlistIntent.putExtra("managerId", managerId);
                    shortlistIntent.putExtra("team", team);
                    startActivity(shortlistIntent);
                    finish();
                } else {
                    Intent shortlistIntent = new Intent(ManageTeamActivity.this, ShortlistActivity.class);
                    shortlistIntent.putExtra("managerId", managerId);
                    shortlistIntent.putExtra("team", team);
                    startActivity(shortlistIntent);
                    finish();
                }
                break;
            case R.id.dr_loaned_out_players:
                Intent loanIntent = new Intent(ManageTeamActivity.this, LoanedOutPlayersActivity.class);
                loanIntent.putExtra("managerId", managerId);
                loanIntent.putExtra("team", team);
                startActivity(loanIntent);
                finish();
                break;
            case R.id.dr_transfer_deals:
                Intent transferIntent = new Intent(ManageTeamActivity.this, TransferDealsActivity.class);
                transferIntent.putExtra("managerId", managerId);
                transferIntent.putExtra("team", team);
                startActivity(transferIntent);
                finish();
                break;
            case R.id.dr_support_info:
                Intent supportIntent = new Intent(ManageTeamActivity.this, SupportActivity.class);
                supportIntent.putExtra("managerId", managerId);
                supportIntent.putExtra("team", team);
                startActivity(supportIntent);
                finish();
                break;
            case R.id.dr_logout:
                if (user != null && firebaseAuth != null) {
                    firebaseAuth.signOut();
                    Intent mainIntent = new Intent(ManageTeamActivity.this, MainActivity.class);
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
        Log.d(LOG_TAG, "onStart called: Fetching manager and player data.");

        managersCollectionRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("id", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot queryDocumentSnapshots = task.getResult();
                            assert queryDocumentSnapshots != null;
                            List<DocumentSnapshot> documentSnapshotList = queryDocumentSnapshots.getDocuments();
                            List<Manager> managerList = new ArrayList<>();
                            for (DocumentSnapshot doc: documentSnapshotList) {
                                Manager manager = doc.toObject(Manager.class);
                                managerList.add(manager);
                                assert manager != null;
                                Log.d(LOG_TAG, "Manager data fetched: " + manager.getFullName());
                            }
                            if (!managerList.isEmpty()) {
                                teamName.setText(managerList.get(0).getTeam());
                                managerName.setText(managerList.get(0).getFullName());
                            } else {
                                Log.w(LOG_TAG, "No manager data found.");
                            }
                        } else {
                            Log.e(LOG_TAG, "Error fetching manager data.", task.getException());
                        }
                    }
                });

        ftPlayersCollectionRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if(task.getResult().size() > 0) {
                                ftPlayersExist = true;
                                Log.d(LOG_TAG, "First Team players exist: " + ftPlayersExist);
                            } else {
                                ftPlayersExist = false;
                                Log.e(LOG_TAG, "Error fetching First Team players.", task.getException());
                            }
                        }
                        ytPlayersCollectionRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                .whereEqualTo("managerId", managerId)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            if(task.getResult().size() > 0) {
                                                ytPlayersExist = true;
                                                Log.d(LOG_TAG, "Youth Team players exist: " + ytPlayersExist);
                                            } else {
                                                ytPlayersExist = false;
                                                Log.e(LOG_TAG, "Error fetching Youth Team players.", task.getException());
                                            }
                                        }
                                        shPlayersCollectionRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                                .whereEqualTo("managerId", managerId)
                                                .get()
                                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                        if (task.isSuccessful()) {
                                                            if(task.getResult().size() > 0) {
                                                                shPlayersExist = true;
                                                                Log.d(LOG_TAG, "Shortlisted players exist: " + shPlayersExist);
                                                            } else {
                                                                shPlayersExist = false;
                                                                Log.e(LOG_TAG, "Error fetching Shortlisted players.", task.getException());
                                                            }
                                                        }

                                                        managerRecyclerAdapter = new ManagerRecyclerAdapter(ManageTeamActivity.this, buttonList, ftPlayersExist, ytPlayersExist, shPlayersExist, managerId, team);
                                                        recyclerView.setAdapter(managerRecyclerAdapter);
                                                        managerRecyclerAdapter.notifyDataSetChanged();
                                                    }
                                                });
                                    }
                                });
                    }
                });

        managersCollectionRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
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
                                Log.d(LOG_TAG, "Additional manager data fetched: " + manager.getFullName());
                            }
                            if (!managerList.isEmpty()) {
                                Manager theManager = managerList.get(0);
                                managerNameHeader.setText(theManager.getFullName());
                                teamHeader.setText(theManager.getTeam());
                            } else {
                                Log.w(LOG_TAG, "No additional manager data found.");
                            }
                        } else {
                            Log.w(LOG_TAG, "No manager data found for header.");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error fetching additional manager data.", e));

    }
}