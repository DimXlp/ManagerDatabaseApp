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
import com.google.android.gms.ads.AdView;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import model.FormerPlayer;
import model.Manager;
import ui.FormerPlayerRecAdapter;
import util.UserApi;

public class FormerPlayersListActivity extends AppCompatActivity {

    private static final String LOG_TAG = "RAFI|FirstTeamList";
    private FirebaseAuth firebaseAuth;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference ftpColRef = db.collection("FirstTeamPlayers");
    private CollectionReference ytpColRef = db.collection("YouthTeamPlayers");
    private CollectionReference frpColRef = db.collection("FormerPlayers");

    private String currentUserId;
    private String currentUserName;

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navView;

    private List<FormerPlayer> ftPlayerList;
    private List<FormerPlayer> ytPlayerList;
    private RecyclerView recyclerView;
    private FormerPlayerRecAdapter formerPlayerRecAdapter;

    private Button prevButton;
    private Button nextButton;
    private TextView teamText;
    private boolean ftPlayersExist;
    private boolean ytPlayersExist;
    private List<FormerPlayer> formerPlayerList;
    private long maxId;
    private boolean shPlayersExist;
    private long managerId;
    private String team;

    private TextView managerNameHeader;
    private TextView teamHeader;
    private String barTeam;
    private NativeAd nativeAdBottom;
    private NativeAdView nativeAdViewBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_former_players_list);
        Log.i(LOG_TAG, "FirstTeamListActivity launched.");

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
            Log.d(LOG_TAG, "Current user authenticated: " + user.getUid());
        } else {
            Log.w(LOG_TAG, "No user is authenticated.");
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            managerId = extras.getLong("managerId");
            team = extras.getString("team");
            barTeam = extras.getString("barTeam");
            Log.d(LOG_TAG, "Extras received: managerId=" + managerId + ", team=" + team + ", barTeam=" + barTeam);
        } else {
            Log.w(LOG_TAG, "No extras received in intent.");
        }

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_menu);

        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nvView_fpl);
        setUpDrawerContent(navView);

        prevButton = findViewById(R.id.prev_button_fpl);
        nextButton = findViewById(R.id.next_button_fpl);
        teamText = findViewById(R.id.team_text_fpl);

        View headerLayout = null;
        if (navView.getHeaderCount() > 0) {
            headerLayout = navView.getHeaderView(0);
        }
        managerNameHeader = headerLayout.findViewById(R.id.manager_name_header);
        teamHeader = headerLayout.findViewById(R.id.team_name_header);

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> Log.d(LOG_TAG, "Mobile Ads SDK initialized."));

        // Load Banner Ads
//        AdView formerPlayersListBanner = findViewById(R.id.former_players_list_banner);
//        AdRequest adBannerRequest = new AdRequest.Builder().build();
//        formerPlayersListBanner.loadAd(adBannerRequest);
//        Log.d(LOG_TAG, "Banner ad loaded.");

        nativeAdViewBottom = findViewById(R.id.native_ad_view_bottom);
        loadNativeAd("ca-app-pub-3940256099942544/2247696110", nativeAdViewBottom);

        View.OnClickListener prevYearListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateYearButtons(v);
                checkTeam(1);
            }
        };

        View.OnClickListener nextYearListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateYearButtons(v);
                checkTeam(2);
            }
        };

        prevButton.setOnClickListener(prevYearListener);
        nextButton.setOnClickListener(nextYearListener);

        formerPlayerList = new ArrayList<>();
        ftPlayerList = new ArrayList<>();
        ytPlayerList = new ArrayList<>();
        recyclerView = findViewById(R.id.rec_view_fpl);
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
                    nativeAdBottom = ad;
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
        Log.d(LOG_TAG, "Populating native ad view.");
        // Dynamically assign IDs
        int headlineId = R.id.ad_headline_bottom;
        int bodyId = R.id.ad_body_bottom;
        int callToActionId = R.id.ad_call_to_action_bottom;

        // Set views for the NativeAdView
        nativeAdView.setHeadlineView(nativeAdView.findViewById(headlineId));
        nativeAdView.setBodyView(nativeAdView.findViewById(bodyId));
        nativeAdView.setCallToActionView(nativeAdView.findViewById(callToActionId));

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

        // Bind the NativeAd object to the NativeAdView
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

    private void checkTeam(int buttonInt) {
        if (teamText.getText().toString().equals("First Team")) {
            teamText.setText("Youth Team");
            listFormerYouthTeamPlayers(buttonInt);
        } else {
            teamText.setText("First Team");
            listFormerFirstTeamPlayers(buttonInt);
        }
    }

    private void listFormerYouthTeamPlayers(final int buttonInt) {
        Log.d(LOG_TAG, "Listing former Youth Team players.");
        ytPlayerList.clear();

        frpColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            for (QueryDocumentSnapshot doc: queryDocumentSnapshots) {
                                FormerPlayer player = doc.toObject(FormerPlayer.class);
                                if (player.getYearSigned().equals("0")) {
                                    ytPlayerList.add(player);
                                }
                            }
                            Collections.sort(ytPlayerList, new Comparator<FormerPlayer>() {
                                @Override
                                public int compare(FormerPlayer o1, FormerPlayer o2) {
                                    return o1.getTimeAdded().compareTo(o2.getTimeAdded());
                                }
                            });
                            formerPlayerRecAdapter = new FormerPlayerRecAdapter(FormerPlayersListActivity.this, ytPlayerList, managerId, team, "Youth Team", buttonInt);
                            recyclerView.setAdapter(formerPlayerRecAdapter);
                            formerPlayerRecAdapter.notifyDataSetChanged();
                            Log.d(LOG_TAG, "Former Youth Team players listed successfully.");
                        } else {
                            Log.w(LOG_TAG, "No former Youth Team players found.");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error fetching Youth Team players: " + e.getMessage(), e));
    }

    private void listFormerFirstTeamPlayers(final int buttonInt) {
        Log.d(LOG_TAG, "Listing former First Team players.");
        ftPlayerList.clear();

        frpColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            for (QueryDocumentSnapshot doc: queryDocumentSnapshots) {
                                FormerPlayer player = doc.toObject(FormerPlayer.class);
                                if (!player.getYearSigned().equals("0")) {
                                    ftPlayerList.add(player);
                                }
                            }
                            Collections.sort(ftPlayerList, new Comparator<FormerPlayer>() {
                                @Override
                                public int compare(FormerPlayer o1, FormerPlayer o2) {
                                    return o1.getTimeAdded().compareTo(o2.getTimeAdded());
                                }
                            });
                            formerPlayerRecAdapter = new FormerPlayerRecAdapter(FormerPlayersListActivity.this, ftPlayerList, managerId, team, "First Team", buttonInt);
                            recyclerView.setAdapter(formerPlayerRecAdapter);
                            formerPlayerRecAdapter.notifyDataSetChanged();
                            Log.d(LOG_TAG, "Former First Team players listed successfully.");
                        } else {
                            Log.w(LOG_TAG, "No former First Team players found.");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error fetching First Team players: " + e.getMessage(), e));
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
                Intent homeIntent = new Intent(FormerPlayersListActivity.this, ManageTeamActivity.class);
                homeIntent.putExtra("managerId", managerId);
                homeIntent.putExtra("team", team);
                startActivity(homeIntent);
                finish();
                break;
            case R.id.dr_manager_selection:
                Intent managerSelectionIntent = new Intent(FormerPlayersListActivity.this, SelectManagerActivity.class);
                startActivity(managerSelectionIntent);
                break;
            case R.id.dr_profile:
                Intent profileIntent = new Intent(FormerPlayersListActivity.this, ProfileActivity.class);
                profileIntent.putExtra("managerId", managerId);
                profileIntent.putExtra("team", team);
                startActivity(profileIntent);
                finish();
                break;
            case R.id.dr_first_team:
                if (ftPlayersExist) {
                    Intent firstIntent = new Intent(FormerPlayersListActivity.this, FirstTeamListActivity.class);
                    firstIntent.putExtra("managerId", managerId);
                    firstIntent.putExtra("team", team);
                    startActivity(firstIntent);
                    finish();
                } else {
                    Intent firstIntent = new Intent(FormerPlayersListActivity.this, FirstTeamActivity.class);
                    firstIntent.putExtra("managerId", managerId);
                    firstIntent.putExtra("team", team);
                    startActivity(firstIntent);
                    finish();
                }
                break;
            case R.id.dr_youth_team:
                if (ytPlayersExist) {
                    Intent youthIntent = new Intent(FormerPlayersListActivity.this, YouthTeamListActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", team);
                    startActivity(youthIntent);
                    finish();
                } else {
                    Intent youthIntent = new Intent(FormerPlayersListActivity.this, YouthTeamActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", team);
                    startActivity(youthIntent);
                    finish();
                }
                break;
            case R.id.dr_former_players:
                break;
            case R.id.dr_shortlist:
                if (shPlayersExist) {
                    Intent shortlistIntent = new Intent(FormerPlayersListActivity.this, ShortlistPlayersActivity.class);
                    shortlistIntent.putExtra("managerId", managerId);
                    shortlistIntent.putExtra("team", team);
                    startActivity(shortlistIntent);
                    finish();
                } else {
                    Intent shortlistIntent = new Intent(FormerPlayersListActivity.this, ShortlistActivity.class);
                    shortlistIntent.putExtra("managerId", managerId);
                    shortlistIntent.putExtra("team", team);
                    startActivity(shortlistIntent);
                    finish();
                }
                break;
            case R.id.dr_loaned_out_players:
                Intent loanIntent = new Intent(FormerPlayersListActivity.this, LoanedOutPlayersActivity.class);
                loanIntent.putExtra("managerId", managerId);
                loanIntent.putExtra("team", team);
                startActivity(loanIntent);
                finish();
                break;
            case R.id.dr_transfer_deals:
                Intent transferIntent = new Intent(FormerPlayersListActivity.this, TransferDealsActivity.class);
                transferIntent.putExtra("managerId", managerId);
                transferIntent.putExtra("team", team);
                startActivity(transferIntent);
                finish();
                break;
            case R.id.dr_support_info:
                Intent supportIntent = new Intent(FormerPlayersListActivity.this, SupportActivity.class);
                supportIntent.putExtra("managerId", managerId);
                supportIntent.putExtra("team", team);
                startActivity(supportIntent);
                finish();
                break;
            case R.id.dr_logout:
                if (user != null && firebaseAuth != null) {
                    firebaseAuth.signOut();
                    startActivity(new Intent(FormerPlayersListActivity.this, MainActivity.class));
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
        Log.d(LOG_TAG, "FirstTeamListActivity started.");

        db.collection("ShortlistedPlayers").whereEqualTo("userId", currentUserId)
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
                            ftPlayersExist = task.getResult().size() > 0;
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
                            ytPlayersExist = task.getResult().size() > 0;
                            Log.d(LOG_TAG, "Youth Team players existence: " + ytPlayersExist);
                        } else {
                            Log.e(LOG_TAG, "Error fetching YouthTeamPlayers.", task.getException());
                        }
                    }
                });

        frpColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            Log.d(LOG_TAG, "Former players data fetched successfully.");
                            for (QueryDocumentSnapshot doc: queryDocumentSnapshots) {
                                FormerPlayer player = doc.toObject(FormerPlayer.class);
                                if (barTeam == null || barTeam.equals("First Team")) {
                                    if (!player.getYearSigned().equals("0")) {
                                        ftPlayerList.add(player);
                                        teamText.setText("First Team");
                                    }
                                } else if (barTeam.equals("Youth Team")) {
                                    if (player.getYearSigned().equals("0")) {
                                        ytPlayerList.add(player);
                                        teamText.setText("Youth Team");
                                    }
                                }
                                formerPlayerList.add(player);
                            }
                            findMaxPlayerId();
                            for (QueryDocumentSnapshot doc: queryDocumentSnapshots) {
                                FormerPlayer player = doc.toObject(FormerPlayer.class);
                                if (player.getId() == 0) {
                                    player.setId(maxId+1);
                                    frpColRef.document(doc.getId()).update("id", player.getId());
                                    maxId++;
                                }
                            }
                            if (barTeam == null || barTeam.equals("First Team")) {
                                Collections.sort(ftPlayerList, new Comparator<FormerPlayer>() {
                                    @Override
                                    public int compare(FormerPlayer o1, FormerPlayer o2) {
                                        return o1.getTimeAdded().compareTo(o2.getTimeAdded());
                                    }
                                });
                                formerPlayerRecAdapter = new FormerPlayerRecAdapter(FormerPlayersListActivity.this, ftPlayerList, managerId, team, "First Team", 0);
                                recyclerView.setAdapter(formerPlayerRecAdapter);
                                formerPlayerRecAdapter.notifyDataSetChanged();
                            } else {
                                Collections.sort(ytPlayerList, new Comparator<FormerPlayer>() {
                                    @Override
                                    public int compare(FormerPlayer o1, FormerPlayer o2) {
                                        return o1.getTimeAdded().compareTo(o2.getTimeAdded());
                                    }
                                });
                                formerPlayerRecAdapter = new FormerPlayerRecAdapter(FormerPlayersListActivity.this, ytPlayerList, managerId, team, "Youth Team", 0);
                                recyclerView.setAdapter(formerPlayerRecAdapter);
                                formerPlayerRecAdapter.notifyDataSetChanged();
                            }
                        } else {
                            Log.w(LOG_TAG, "No former players found.");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error fetching FormerPlayers.", e));

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
                            Log.d(LOG_TAG, "Manager data loaded: " + theManager.getFullName());
                        } else {
                            Log.w(LOG_TAG, "No manager data found.");
                        }
                    }
                });
    }

    private void findMaxPlayerId() {
        maxId = formerPlayerList.get(0).getId();
        for (FormerPlayer player: formerPlayerList) {
            if (player.getId() > maxId) {
                maxId = player.getId();
            }
        }
        Log.d(LOG_TAG, "Max player ID found: " + maxId);
    }

    @Override
    protected void onResume() {
        super.onResume();

        ftPlayerList.clear();
        ytPlayerList.clear();
    }

    @Override
    protected void onDestroy() {
        if (nativeAdBottom != null) nativeAdBottom.destroy();
        Log.i(LOG_TAG, "FormerPlayersListActivity destroyed.");
        super.onDestroy();
    }
}