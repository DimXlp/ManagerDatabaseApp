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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

// import com.google.android.gms.ads.AdLoader;
// import com.google.android.gms.ads.AdRequest;
// import com.google.android.gms.ads.LoadAdError;
// import com.google.android.gms.ads.MobileAds;
// import com.google.android.gms.ads.nativead.NativeAd;
// import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import enumeration.PositionEnum;
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

    private TextView loanedOutPlayerCount;
    private RecyclerView recyclerView;
    private LoanedOutPlayerRecAdapter loanedOutPlayerRecAdapter;
    private List<LoanedOutPlayer> playerList;
    private List<LoanedOutPlayer> fullPlayerList; // Store all players for filtering
    private long maxId;
    private long managerId;
    private String team;

    private EditText searchBar;
    private LinearLayout titleContainer;
    private LinearLayout searchBarContainer;
    private ImageButton searchIconButton;
    private ImageButton closeSearchButton;
    private ImageButton filterIconButton;
    private boolean isSearchMode = false;
    private boolean isFilterMode = false;
    
    // Filter state
    private String currentSortOption = "none";
    private List<String> selectedPositions = new ArrayList<>();
    private List<String> selectedPositionCategories = new ArrayList<>();

    private TextView managerNameHeader;
    private TextView teamHeader;
    // private NativeAd nativeAd;
    // private NativeAdView nativeAdView;

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
        fullPlayerList = new ArrayList<>();

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

        loanedOutPlayerCount = findViewById(R.id.player_count_lop);

        recyclerView = findViewById(R.id.rec_view_lop);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize search and filter UI components
        searchIconButton = findViewById(R.id.search_icon_button_lop);
        filterIconButton = findViewById(R.id.filter_icon_button_lop);
        closeSearchButton = findViewById(R.id.close_search_button_lop);
        titleContainer = findViewById(R.id.title_container_lop);
        searchBarContainer = findViewById(R.id.search_bar_container_lop);
        searchBar = findViewById(R.id.search_bar_lop);
        
        // Search icon click - show search bar with animation
        searchIconButton.setOnClickListener(v -> showSearchBar());
        
        // Filter icon click - show filter bottom sheet
        filterIconButton.setOnClickListener(v -> showFilterDialog());
        
        // Close search button click - hide search bar with animation
        closeSearchButton.setOnClickListener(v -> hideSearchBar());
        
        // Search bar text watcher
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                
                if (query.isEmpty()) {
                    // Restore filtered or full view depending on filter mode
                    if (isFilterMode) {
                        applyFiltersAndSort();
                    } else {
                        restoreFullList();
                    }
                } else {
                    filterPlayersWithSearchAndFilters(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });

        // Initialize Mobile Ads SDK
        // MobileAds.initialize(this, initializationStatus -> Log.d(LOG_TAG, "Mobile Ads SDK initialized."));

        // Load Native Ad
        // nativeAdView = findViewById(R.id.native_ad_view_bottom);
        // loadNativeAd("ca-app-pub-3940256099942544/2247696110", nativeAdView);
        Log.d(LOG_TAG, "Native ad view set up.");
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

    public void refreshPlayerList() {
        Log.d(LOG_TAG, "refreshPlayerList called");
        playerList.clear();
        fullPlayerList.clear();
        lopColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            for (QueryDocumentSnapshot doc: queryDocumentSnapshots) {
                                LoanedOutPlayer player = doc.toObject(LoanedOutPlayer.class);
                                if (player.getId() == 0) {
                                    player.setId(maxId+1);
                                    lopColRef.document(doc.getId()).update("id", player.getId());
                                    maxId++;
                                }
                                fullPlayerList.add(player);
                            }
                            playerList.addAll(fullPlayerList);
                            loanedOutPlayerRecAdapter.notifyDataSetChanged();
                            loanedOutPlayerCount.setText(playerList.size() + " players");
                            Log.d(LOG_TAG, "Loaned out players refreshed successfully.");
                        } else {
                            loanedOutPlayerCount.setText(playerList.size() + " players");
                            Log.w(LOG_TAG, "No loaned out players found.");
                        }
                    }
                });
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
                            fullPlayerList.clear();
                            for (QueryDocumentSnapshot doc: queryDocumentSnapshots) {
                                LoanedOutPlayer player = doc.toObject(LoanedOutPlayer.class);
                                if (player.getId() == 0) {
                                    player.setId(maxId+1);
                                    lopColRef.document(doc.getId()).update("id", player.getId());
                                    maxId++;
                                }
                                fullPlayerList.add(player);
                            }
                            playerList.addAll(fullPlayerList);
                            loanedOutPlayerRecAdapter = new LoanedOutPlayerRecAdapter(LoanedOutPlayersActivity.this, playerList, managerId, team);
                            recyclerView.setAdapter(loanedOutPlayerRecAdapter);
                            loanedOutPlayerRecAdapter.notifyDataSetChanged();
                            loanedOutPlayerCount.setText(playerList.size() + " players");
                            Log.d(LOG_TAG, "Loaned out players listed successfully.");
                        } else {
                            loanedOutPlayerCount.setText(playerList.size() + " players");
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

    /**
     * Show search bar with animation
     */
    private void showSearchBar() {
        if (isSearchMode) return;
        
        isSearchMode = true;
        
        // Make search container visible
        searchBarContainer.setVisibility(View.VISIBLE);
        searchBarContainer.setAlpha(0f);
        
        // Make close button visible
        closeSearchButton.setVisibility(View.VISIBLE);
        closeSearchButton.setAlpha(0f);
        
        // Animate title sliding to the right
        titleContainer.animate()
                .translationX(titleContainer.getWidth())
                .alpha(0f)
                .setDuration(300);
        
        // Animate search bar fading in
        searchBarContainer.animate()
                .alpha(1f)
                .setDuration(300)
                .withEndAction(() -> {
                    // Focus on search bar and show keyboard
                    searchBar.requestFocus();
                    searchBar.postDelayed(() -> {
                        android.view.inputmethod.InputMethodManager imm = 
                            (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.showSoftInput(searchBar, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                        }
                    }, 100);
                });
        
        // Animate close button fading in
        closeSearchButton.animate()
                .alpha(1f)
                .setDuration(300);
        
        Log.d(LOG_TAG, "Search bar shown");
    }

    /**
     * Hide search bar with animation
     */
    private void hideSearchBar() {
        if (!isSearchMode) return;
        
        isSearchMode = false;
        
        // Clear search text
        searchBar.setText("");
        
        // Hide keyboard
        android.view.inputmethod.InputMethodManager imm = 
            (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
        }
        
        // Animate search bar fading out
        searchBarContainer.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> {
                    searchBarContainer.setVisibility(View.GONE);
                });
        
        // Animate close button fading out
        closeSearchButton.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> {
                    closeSearchButton.setVisibility(View.GONE);
                });
        
        // Animate title sliding back if not in filter mode
        if (!isFilterMode && titleContainer.getTranslationX() != 0f) {
            titleContainer.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(300);
        }
        
        // Restore view
        if (!isFilterMode) {
            restoreFullList();
        }
        
        Log.d(LOG_TAG, "Search bar hidden");
    }

    /**
     * Show filter bottom sheet dialog
     */
    private void showFilterDialog() {
        BottomSheetDialog filterDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.filter_bottom_sheet, null);
        filterDialog.setContentView(view);

        // Get views from the bottom sheet
        LinearLayout sortByHeader = view.findViewById(R.id.sort_by_header);
        LinearLayout sortByContent = view.findViewById(R.id.sort_by_content);
        ImageView sortByChevron = view.findViewById(R.id.sort_by_chevron);
        TextView sortBySelection = view.findViewById(R.id.sort_by_selection);
        
        LinearLayout positionFilterHeader = view.findViewById(R.id.position_filter_header);
        LinearLayout positionFilterContent = view.findViewById(R.id.position_filter_content);
        ImageView positionFilterChevron = view.findViewById(R.id.position_filter_chevron);
        TextView positionFilterSelection = view.findViewById(R.id.position_filter_selection);
        
        LinearLayout positionCategoryHeader = view.findViewById(R.id.position_category_header);
        LinearLayout positionCategoryContent = view.findViewById(R.id.position_category_content);
        ImageView positionCategoryChevron = view.findViewById(R.id.position_category_chevron);
        TextView positionCategorySelection = view.findViewById(R.id.position_category_selection);
        
        RadioGroup sortRadioGroup = view.findViewById(R.id.sort_radio_group);
        RadioButton sortNone = view.findViewById(R.id.sort_none);
        RadioButton sortName = view.findViewById(R.id.sort_name);
        RadioButton sortPosition = view.findViewById(R.id.sort_position);
        RadioButton sortOverallAsc = view.findViewById(R.id.sort_overall_asc);
        RadioButton sortOverallDesc = view.findViewById(R.id.sort_overall_desc);
        
        Button clearFiltersButton = view.findViewById(R.id.clear_filters_button);
        Button applyFiltersButton = view.findViewById(R.id.apply_filters_button);
        
        // Handle dialog dismissal - keep title hidden if filters active
        filterDialog.setOnDismissListener(dialog -> {
            if (isFilterMode) {
                titleContainer.setVisibility(View.INVISIBLE);
                titleContainer.setAlpha(0f);
                titleContainer.setTranslationX(titleContainer.getWidth());
            }
        });

        // Set current sort selection
        switch (currentSortOption) {
            case "name":
                sortName.setChecked(true);
                sortBySelection.setText("Name");
                break;
            case "position":
                sortPosition.setChecked(true);
                sortBySelection.setText("Position");
                break;
            case "overall_asc":
                sortOverallAsc.setChecked(true);
                sortBySelection.setText("Overall ↑");
                break;
            case "overall_desc":
                sortOverallDesc.setChecked(true);
                sortBySelection.setText("Overall ↓");
                break;
            default:
                sortNone.setChecked(true);
                sortBySelection.setText("None");
                break;
        }

        // Update sort selection text when radio buttons change
        sortRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.sort_name) {
                sortBySelection.setText("Name");
            } else if (checkedId == R.id.sort_position) {
                sortBySelection.setText("Position");
            } else if (checkedId == R.id.sort_overall_asc) {
                sortBySelection.setText("Overall ↑");
            } else if (checkedId == R.id.sort_overall_desc) {
                sortBySelection.setText("Overall ↓");
            } else {
                sortBySelection.setText("None");
            }
        });

        // Populate position checkboxes dynamically
        String[] positions = getResources().getStringArray(R.array.position_array);
        for (String position : positions) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(position);
            checkBox.setTextColor(getResources().getColor(android.R.color.black));
            checkBox.setChecked(selectedPositions.contains(position));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!selectedPositions.contains(position)) {
                        selectedPositions.add(position);
                    }
                } else {
                    selectedPositions.remove(position);
                }
                updatePositionSelectionText(positionFilterSelection);
            });
            positionFilterContent.addView(checkBox);
        }

        // Populate position category checkboxes dynamically
        String[] categories = new String[]{
                "Goalkeepers",
                "Center Backs",
                "Right Backs",
                "Left Backs",
                "Center Defensive Mids",
                "Center Midfielders",
                "Center Attacking Mids",
                "Right Wingers",
                "Left Wingers",
                "Strikers"
        };
        
        for (String category : categories) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(category);
            checkBox.setTextColor(getResources().getColor(android.R.color.black));
            checkBox.setChecked(selectedPositionCategories.contains(category));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!selectedPositionCategories.contains(category)) {
                        selectedPositionCategories.add(category);
                    }
                } else {
                    selectedPositionCategories.remove(category);
                }
                updatePositionCategorySelectionText(positionCategorySelection);
            });
            positionCategoryContent.addView(checkBox);
        }

        // Update selection texts
        updatePositionSelectionText(positionFilterSelection);
        updatePositionCategorySelectionText(positionCategorySelection);

        // Sort By Header Click - Toggle expand/collapse
        sortByHeader.setOnClickListener(v -> toggleSection(sortByContent, sortByChevron));

        // Position Filter Header Click - Toggle expand/collapse
        positionFilterHeader.setOnClickListener(v -> toggleSection(positionFilterContent, positionFilterChevron));

        // Position Category Header Click - Toggle expand/collapse
        positionCategoryHeader.setOnClickListener(v -> toggleSection(positionCategoryContent, positionCategoryChevron));

        // Clear filters button
        clearFiltersButton.setOnClickListener(v -> {
            currentSortOption = "none";
            selectedPositions.clear();
            selectedPositionCategories.clear();
            isFilterMode = false;
            
            filterDialog.dismiss();
            
            // Show title with animation
            titleContainer.setVisibility(View.VISIBLE);
            titleContainer.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(300);
            
            restoreFullList();
        });

        // Apply filters button
        applyFiltersButton.setOnClickListener(v -> {
            // Get selected sort option
            int selectedId = sortRadioGroup.getCheckedRadioButtonId();
            if (selectedId == R.id.sort_name) {
                currentSortOption = "name";
            } else if (selectedId == R.id.sort_position) {
                currentSortOption = "position";
            } else if (selectedId == R.id.sort_overall_asc) {
                currentSortOption = "overall_asc";
            } else if (selectedId == R.id.sort_overall_desc) {
                currentSortOption = "overall_desc";
            } else {
                currentSortOption = "none";
            }

            // Check if any filters/sorts are active
            boolean hasActiveFilters = !currentSortOption.equals("none") || 
                                       !selectedPositions.isEmpty() || 
                                       !selectedPositionCategories.isEmpty();
            
            boolean wasFilterMode = isFilterMode;
            isFilterMode = hasActiveFilters;
            
            filterDialog.dismiss();
            
            if (isFilterMode) {
                // Hide title when filters are active
                if (!wasFilterMode) {
                    titleContainer.animate()
                            .translationX(titleContainer.getWidth())
                            .alpha(0f)
                            .setDuration(300);
                }
                
                applyFiltersAndSort();
            } else {
                // Show title and restore full view
                if (wasFilterMode) {
                    titleContainer.setVisibility(View.VISIBLE);
                    titleContainer.animate()
                            .translationX(0f)
                            .alpha(1f)
                            .setDuration(300);
                }
                restoreFullList();
            }
        });

        filterDialog.show();
    }

    /**
     * Toggle section visibility with animated chevron rotation
     */
    private void toggleSection(LinearLayout content, ImageView chevron) {
        if (content.getVisibility() == View.VISIBLE) {
            // Collapse
            content.setVisibility(View.GONE);
            chevron.animate().rotation(0f).setDuration(200).start();
        } else {
            // Expand
            content.setVisibility(View.VISIBLE);
            chevron.animate().rotation(180f).setDuration(200).start();
        }
    }

    /**
     * Update position selection text based on selected positions
     */
    private void updatePositionSelectionText(TextView textView) {
        if (selectedPositions.isEmpty()) {
            textView.setText("All");
        } else if (selectedPositions.size() == 1) {
            textView.setText(selectedPositions.get(0));
        } else {
            textView.setText(selectedPositions.size() + " selected");
        }
    }

    /**
     * Update position category selection text based on selected categories
     */
    private void updatePositionCategorySelectionText(TextView textView) {
        if (selectedPositionCategories.isEmpty()) {
            textView.setText("All");
        } else if (selectedPositionCategories.size() == 1) {
            textView.setText(selectedPositionCategories.get(0));
        } else {
            textView.setText(selectedPositionCategories.size() + " selected");
        }
    }

    /**
     * Apply filters and sorting to all players
     */
    private void applyFiltersAndSort() {
        playerList.clear();
        
        // Start with full player list
        List<LoanedOutPlayer> filteredList = new ArrayList<>(fullPlayerList);
        
        // Apply position filter (multiple selections)
        if (!selectedPositions.isEmpty()) {
            List<LoanedOutPlayer> temp = new ArrayList<>();
            for (LoanedOutPlayer player : filteredList) {
                if (player.getPosition() != null && selectedPositions.contains(player.getPosition())) {
                    temp.add(player);
                }
            }
            filteredList = temp;
        }
        
        // Apply position category filter (multiple selections)
        if (!selectedPositionCategories.isEmpty()) {
            List<LoanedOutPlayer> temp = new ArrayList<>();
            for (LoanedOutPlayer player : filteredList) {
                if (player.getPosition() != null) {
                    String playerCategory = getPositionCategory(player.getPosition());
                    if (selectedPositionCategories.contains(playerCategory)) {
                        temp.add(player);
                    }
                }
            }
            filteredList = temp;
        }
        
        // Apply sorting
        switch (currentSortOption) {
            case "name":
                Collections.sort(filteredList, (p1, p2) -> {
                    String name1 = p1.getLastName() != null ? p1.getLastName() : "";
                    String name2 = p2.getLastName() != null ? p2.getLastName() : "";
                    return name1.compareToIgnoreCase(name2);
                });
                break;
            case "position":
                // Sort by position using the same order as position_array
                String[] positionOrder = getResources().getStringArray(R.array.position_array);
                Collections.sort(filteredList, (p1, p2) -> {
                    String pos1 = p1.getPosition() != null ? p1.getPosition() : "";
                    String pos2 = p2.getPosition() != null ? p2.getPosition() : "";
                    
                    int index1 = getPositionIndex(pos1, positionOrder);
                    int index2 = getPositionIndex(pos2, positionOrder);
                    
                    return Integer.compare(index1, index2);
                });
                break;
            case "overall_asc":
                Collections.sort(filteredList, (p1, p2) -> Integer.compare(p1.getOverall(), p2.getOverall()));
                break;
            case "overall_desc":
                Collections.sort(filteredList, (p1, p2) -> Integer.compare(p2.getOverall(), p1.getOverall()));
                break;
            default:
                // Keep default order (by time added)
                break;
        }
        
        playerList.addAll(filteredList);
        
        // Update adapter
        if (loanedOutPlayerRecAdapter != null) {
            loanedOutPlayerRecAdapter.notifyDataSetChanged();
        }
        
        // Update player count
        loanedOutPlayerCount.setText(playerList.size() + " players");
        
        Log.d(LOG_TAG, "Applied filters and sort. " + playerList.size() + " players found.");
    }

    /**
     * Filter players with both search and filters applied
     */
    private void filterPlayersWithSearchAndFilters(String query) {
        Log.d(LOG_TAG, "filterPlayersWithSearchAndFilters called with query: " + query);
        
        playerList.clear();
        
        if (query == null || query.trim().isEmpty()) {
            // If search is empty, restore filtered or full view
            if (isFilterMode) {
                applyFiltersAndSort();
            } else {
                restoreFullList();
            }
            return;
        }
        
        // Start with full list
        List<LoanedOutPlayer> baseList = new ArrayList<>(fullPlayerList);
        
        // Apply filters if in filter mode
        if (isFilterMode) {
            // Apply position filter (multiple selections)
            if (!selectedPositions.isEmpty()) {
                List<LoanedOutPlayer> temp = new ArrayList<>();
                for (LoanedOutPlayer player : baseList) {
                    if (player.getPosition() != null && selectedPositions.contains(player.getPosition())) {
                        temp.add(player);
                    }
                }
                baseList = temp;
            }
            
            // Apply position category filter (multiple selections)
            if (!selectedPositionCategories.isEmpty()) {
                List<LoanedOutPlayer> temp = new ArrayList<>();
                for (LoanedOutPlayer player : baseList) {
                    if (player.getPosition() != null) {
                        String playerCategory = getPositionCategory(player.getPosition());
                        if (selectedPositionCategories.contains(playerCategory)) {
                            temp.add(player);
                        }
                    }
                }
                baseList = temp;
            }
        }
        
        // Now apply search query to the base list
        String searchQuery = query.toLowerCase().trim();
        for (LoanedOutPlayer player : baseList) {
            boolean matches = false;
            
            // Search in first name
            if (player.getFirstName() != null && 
                player.getFirstName().toLowerCase().contains(searchQuery)) {
                matches = true;
            }
            
            // Search in last name
            if (!matches && player.getLastName() != null && 
                player.getLastName().toLowerCase().contains(searchQuery)) {
                matches = true;
            }
            
            // Search in full name
            if (!matches && player.getFullName() != null && 
                player.getFullName().toLowerCase().contains(searchQuery)) {
                matches = true;
            }
            
            // Search in team name
            if (!matches && player.getTeam() != null && 
                player.getTeam().toLowerCase().contains(searchQuery)) {
                matches = true;
            }
            
            if (matches) {
                playerList.add(player);
            }
        }
        
        // Apply sorting if in filter mode
        if (isFilterMode && !currentSortOption.equals("none")) {
            switch (currentSortOption) {
                case "name":
                    Collections.sort(playerList, (p1, p2) -> {
                        String name1 = p1.getLastName() != null ? p1.getLastName() : "";
                        String name2 = p2.getLastName() != null ? p2.getLastName() : "";
                        return name1.compareToIgnoreCase(name2);
                    });
                    break;
                case "position":
                    String[] positionOrder = getResources().getStringArray(R.array.position_array);
                    Collections.sort(playerList, (p1, p2) -> {
                        String pos1 = p1.getPosition() != null ? p1.getPosition() : "";
                        String pos2 = p2.getPosition() != null ? p2.getPosition() : "";
                        
                        int index1 = getPositionIndex(pos1, positionOrder);
                        int index2 = getPositionIndex(pos2, positionOrder);
                        
                        return Integer.compare(index1, index2);
                    });
                    break;
                case "overall_asc":
                    Collections.sort(playerList, (p1, p2) -> Integer.compare(p1.getOverall(), p2.getOverall()));
                    break;
                case "overall_desc":
                    Collections.sort(playerList, (p1, p2) -> Integer.compare(p2.getOverall(), p1.getOverall()));
                    break;
            }
        }
        
        // Update adapter
        if (loanedOutPlayerRecAdapter != null) {
            loanedOutPlayerRecAdapter.notifyDataSetChanged();
        }
        
        // Update player count
        loanedOutPlayerCount.setText(playerList.size() + " players");
        
        Log.d(LOG_TAG, "Search found " + playerList.size() + " players for query: " + query);
    }

    /**
     * Restore full list of players
     */
    private void restoreFullList() {
        playerList.clear();
        playerList.addAll(fullPlayerList);
        
        if (loanedOutPlayerRecAdapter != null) {
            loanedOutPlayerRecAdapter.notifyDataSetChanged();
        }
        
        loanedOutPlayerCount.setText(playerList.size() + " players");
        
        Log.d(LOG_TAG, "Restored full list. " + playerList.size() + " players.");
    }

    /**
     * Get position category from position initials (e.g., "LB" -> "Left Backs")
     */
    private String getPositionCategory(String positionInitials) {
        if (positionInitials == null || positionInitials.isEmpty()) {
            return "Unknown";
        }
        
        for (PositionEnum pos : PositionEnum.values()) {
            if (pos.getInitials().equals(positionInitials)) {
                return pos.getCategory();
            }
        }
        
        return "Unknown";
    }

    /**
     * Get position index from position array for sorting
     * Returns a large number if position not found (will be sorted to end)
     */
    private int getPositionIndex(String position, String[] positionOrder) {
        if (position == null || position.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        
        for (int i = 0; i < positionOrder.length; i++) {
            if (positionOrder[i].equals(position)) {
                return i;
            }
        }
        
        return Integer.MAX_VALUE;
    }

    @Override
    protected void onDestroy() {
        // if (nativeAd != null) {
        //     nativeAd.destroy();
        // }
        super.onDestroy();
        Log.d(LOG_TAG, "LoanedOutPlayersActivity destroyed.");
    }
}