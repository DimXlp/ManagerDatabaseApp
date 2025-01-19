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
import android.widget.Button;
import android.widget.TextView;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import model.FormerPlayer;
import model.LoanedOutPlayer;
import model.Manager;
import ui.FormerPlayerRecAdapter;
import ui.LoanedOutPlayerRecAdapter;
import util.UserApi;

public class LoanedOutPlayersActivity extends AppCompatActivity {

    private static final String LOG_TAG = "RAFI|LoanedOutPlayers";
    private FirebaseAuth firebaseAuth;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference ftpColRef = db.collection("FirstTeamPlayers");
    private CollectionReference ytpColRef = db.collection("YouthTeamPlayers");
    private CollectionReference frpColRef = db.collection("FormerPlayers");
    private CollectionReference lopColRef = db.collection("LoanedOutPlayers");
    private CollectionReference shpColRef = db.collection("ShortlistedPlayers");

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navView;
    private boolean ftPlayersExist;
    private boolean ytPlayersExist;
    private boolean shPlayersExist;

    private RecyclerView recyclerView;
    private LoanedOutPlayerRecAdapter loanedOutPlayerRecAdapter;
    private List<LoanedOutPlayer> playerList;
    private long maxId;
    private long managerId;
    private String team;

    private TextView managerNameHeader;
    private TextView teamHeader;
    private NativeAd nativeAd;
    private NativeAdView nativeAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loaned_out_players);
        Log.i(LOG_TAG, "LoanedOutPlayersActivity launched.");

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
            team = extras.getString("team");
            Log.d(LOG_TAG, "Received extras: managerId=" + managerId + ", team=" + team);
        } else {
            Log.w(LOG_TAG, "No extras received in intent.");
        }

        playerList = new ArrayList<>();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_menu);

        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nvView_lop);
        setUpDrawerContent(navView);

        View headerLayout = null;
        if (navView.getHeaderCount() > 0) {
            headerLayout = navView.getHeaderView(0);
        }
        managerNameHeader = headerLayout.findViewById(R.id.manager_name_header);
        teamHeader = headerLayout.findViewById(R.id.team_name_header);

        recyclerView = findViewById(R.id.rec_view_lop);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> Log.d(LOG_TAG, "Mobile Ads SDK initialized."));

        // Load Native Ad
        nativeAdView = findViewById(R.id.native_ad_view);
        loadNativeAd();
        Log.d(LOG_TAG, "Native ad view set up.");
    }

    private void loadNativeAd() {
        AdLoader adLoader = new AdLoader.Builder(this, "ca-app-pub-3940256099942544/2247696110") // Replace with your Native Ad Unit ID
                .forNativeAd(ad -> {
                    if (isDestroyed()) {
                        ad.destroy();
                        return;
                    }
                    nativeAd = ad;
                    populateNativeAdView(nativeAd, nativeAdView);
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

        // Set the views for the NativeAdView
        nativeAdView.setHeadlineView(nativeAdView.findViewById(R.id.ad_headline));
        nativeAdView.setBodyView(nativeAdView.findViewById(R.id.ad_body));
        nativeAdView.setCallToActionView(nativeAdView.findViewById(R.id.ad_call_to_action));

        // Populate the Headline
        ((TextView) nativeAdView.getHeadlineView()).setText(nativeAd.getHeadline());

        // Populate the Body
        if (nativeAd.getBody() != null) {
            ((TextView) nativeAdView.getBodyView()).setText(nativeAd.getBody());
            nativeAdView.getBodyView().setVisibility(View.VISIBLE);
        } else {
            nativeAdView.getBodyView().setVisibility(View.GONE);
        }

        // Populate the Call-to-Action
        if (nativeAd.getCallToAction() != null) {
            ((Button) nativeAdView.getCallToActionView()).setText(nativeAd.getCallToAction());
            nativeAdView.getCallToActionView().setVisibility(View.VISIBLE);
        } else {
            nativeAdView.getCallToActionView().setVisibility(View.GONE);
        }

        // Bind native ad to the view
        nativeAdView.setNativeAd(nativeAd);
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
                Intent homeIntent = new Intent(LoanedOutPlayersActivity.this, ManageTeamActivity.class);
                homeIntent.putExtra("managerId", managerId);
                homeIntent.putExtra("team", team);
                startActivity(homeIntent);
                finish();
                break;
            case R.id.dr_manager_selection:
                Intent managerSelectionIntent = new Intent(LoanedOutPlayersActivity.this, SelectManagerActivity.class);
                startActivity(managerSelectionIntent);
                break;
            case R.id.dr_profile:
                Intent profileIntent = new Intent(LoanedOutPlayersActivity.this, ProfileActivity.class);
                profileIntent.putExtra("managerId", managerId);
                profileIntent.putExtra("team", team);
                startActivity(profileIntent);
                finish();
                break;
            case R.id.dr_first_team:
                if (ftPlayersExist) {
                    Intent firstIntent = new Intent(LoanedOutPlayersActivity.this, FirstTeamListActivity.class);
                    firstIntent.putExtra("managerId", managerId);
                    firstIntent.putExtra("team", team);
                    startActivity(firstIntent);
                    finish();
                } else {
                    Intent firstIntent = new Intent(LoanedOutPlayersActivity.this, FirstTeamActivity.class);
                    firstIntent.putExtra("managerId", managerId);
                    firstIntent.putExtra("team", team);
                    startActivity(firstIntent);
                    finish();
                }
                break;
            case R.id.dr_youth_team:
                if (ytPlayersExist) {
                    Intent youthIntent = new Intent(LoanedOutPlayersActivity.this, YouthTeamListActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", team);
                    startActivity(youthIntent);
                    finish();
                } else {
                    Intent youthIntent = new Intent(LoanedOutPlayersActivity.this, YouthTeamActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", team);
                    startActivity(youthIntent);
                    finish();
                }
                break;
            case R.id.dr_former_players:
                Intent formerIntent = new Intent(LoanedOutPlayersActivity.this, FormerPlayersListActivity.class);
                formerIntent.putExtra("managerId", managerId);
                formerIntent.putExtra("team", team);
                startActivity(formerIntent);
                finish();
                break;
            case R.id.dr_shortlist:
                if (shPlayersExist) {
                    Intent shortlistIntent = new Intent(LoanedOutPlayersActivity.this, ShortlistPlayersActivity.class);
                    shortlistIntent.putExtra("managerId", managerId);
                    shortlistIntent.putExtra("team", team);
                    startActivity(shortlistIntent);
                    finish();
                } else {
                    Intent shortlistIntent = new Intent(LoanedOutPlayersActivity.this, ShortlistActivity.class);
                    shortlistIntent.putExtra("managerId", managerId);
                    shortlistIntent.putExtra("team", team);
                    startActivity(shortlistIntent);
                    finish();
                }
                break;
            case R.id.dr_loaned_out_players:
                break;
            case R.id.dr_transfer_deals:
                Intent transferIntent = new Intent(LoanedOutPlayersActivity.this, TransferDealsActivity.class);
                transferIntent.putExtra("managerId", managerId);
                transferIntent.putExtra("team", team);
                startActivity(transferIntent);
                finish();
                break;
            case R.id.dr_support_info:
                Intent supportIntent = new Intent(LoanedOutPlayersActivity.this, SupportActivity.class);
                supportIntent.putExtra("managerId", managerId);
                supportIntent.putExtra("team", team);
                startActivity(supportIntent);
                finish();
                break;
            case R.id.dr_logout:
                if (user != null && firebaseAuth != null) {
                    firebaseAuth.signOut();
                    startActivity(new Intent(LoanedOutPlayersActivity.this, MainActivity.class));
                    Log.i(LOG_TAG, "User logged out successfully.");
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
        if (item.getItemId() == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "onStart called: Fetching player and manager data.");

        shpColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            shPlayersExist = Objects.requireNonNull(task.getResult()).size() > 0;
                            Log.d(LOG_TAG, "Shortlisted players existence: " + shPlayersExist);
                        } else {
                            Log.e(LOG_TAG, "Error fetching ShortlistedPlayers.", task.getException());
                        }
                    }
                });

        ftpColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            ftPlayersExist = Objects.requireNonNull(task.getResult()).size() > 0;
                            Log.d(LOG_TAG, "First Team players existence: " + ftPlayersExist);
                        } else {
                            Log.e(LOG_TAG, "Error fetching FirstTeamPlayers.", task.getException());
                        }
                    }
                });

        ytpColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            ytPlayersExist = Objects.requireNonNull(task.getResult()).size() > 0;
                            Log.d(LOG_TAG, "Youth Team players existence: " + ytPlayersExist);
                        } else {
                            Log.e(LOG_TAG, "Error fetching YouthTeamPlayers.", task.getException());
                        }
                    }
                });

        lopColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            for (QueryDocumentSnapshot doc: queryDocumentSnapshots) {
                                LoanedOutPlayer player = doc.toObject(LoanedOutPlayer.class);
                                playerList.add(player);
                            }
                            findMaxPlayerId();
                            playerList.clear();
                            for (QueryDocumentSnapshot doc: queryDocumentSnapshots) {
                                LoanedOutPlayer player = doc.toObject(LoanedOutPlayer.class);
                                if (player.getId() == 0) {
                                    player.setId(maxId+1);
                                    lopColRef.document(doc.getId()).update("id", player.getId());
                                    maxId++;
                                }
                                playerList.add(player);
                            }
                            loanedOutPlayerRecAdapter = new LoanedOutPlayerRecAdapter(LoanedOutPlayersActivity.this, playerList, managerId, team);
                            recyclerView.setAdapter(loanedOutPlayerRecAdapter);
                            loanedOutPlayerRecAdapter.notifyDataSetChanged();
                            Log.d(LOG_TAG, "Loaned out players listed successfully.");
                        } else {
                            Log.w(LOG_TAG, "No loaned out players found.");
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
                            Log.d(LOG_TAG, "Manager data loaded: " + theManager.getFullName());
                        } else {
                            Log.w(LOG_TAG, "No manager data found.");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error fetching Manager data.", e));
    }

    private void findMaxPlayerId() {
        maxId = playerList.get(0).getId();
        for (LoanedOutPlayer player: playerList) {
            if (player.getId() > maxId) {
                maxId = player.getId();
            }
        }
        Log.d(LOG_TAG, "Max player ID: " + maxId);
    }

    @Override
    protected void onDestroy() {
        if (nativeAd != null) {
            nativeAd.destroy();
        }
        super.onDestroy();
        Log.d(LOG_TAG, "LoanedOutPlayersActivity destroyed.");
    }
}