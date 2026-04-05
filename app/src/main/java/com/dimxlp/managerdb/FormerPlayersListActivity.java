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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import enumeration.PositionEnum;
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
    private List<FormerPlayer> fullPlayerList; // Store all players for filtering (current team)
    private List<FormerPlayer> allPlayersCache; // Store all players for search across current team
    private boolean isLoadingAllPlayers = false; // Flag to prevent multiple simultaneous loads
    private RecyclerView recyclerView;
    private FormerPlayerRecAdapter formerPlayerRecAdapter;

    private Button prevButton;
    private Button nextButton;
    private TextView teamText;
    private TextView teamPlayerCount;
    private EditText searchBar;
    private LinearLayout teamNavigationContainer;
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
    // private NativeAd nativeAdBottom;
    // private NativeAdView nativeAdViewBottom;

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
        
        teamNavigationContainer = findViewById(R.id.team_navigation_container);
        searchBarContainer = findViewById(R.id.search_bar_container);
        searchIconButton = findViewById(R.id.search_icon_button);
        closeSearchButton = findViewById(R.id.close_search_button);
        filterIconButton = findViewById(R.id.filter_icon_button);

        LinearLayout teamPickerLayout = findViewById(R.id.team_picker_container_fpl);
        teamText = findViewById(R.id.team_text_fpl);
        teamPlayerCount = findViewById(R.id.team_player_count_fpl);

        List<String> teamOptions = new ArrayList<>();
        teamOptions.add("First Team");
        teamOptions.add("Youth Team");

        teamPickerLayout.setOnClickListener(v -> {
            if (!teamOptions.isEmpty()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Select Team");
                builder.setItems(teamOptions.toArray(new String[0]), (dialog, which) -> {
                    team = teamOptions.get(which);
                    teamText.setText(team);

                    if (team.equals("First Team")) {
                        listFormerFirstTeamPlayers(0);
                    } else {
                        listFormerYouthTeamPlayers(0);
                    }
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

        // Initialize search bar
        searchBar = findViewById(R.id.search_bar_fpl);
        
        // Search icon click - show search bar with slide animation
        searchIconButton.setOnClickListener(v -> showSearchBar());
        
        // Filter icon click - show filter bottom sheet
        filterIconButton.setOnClickListener(v -> showFilterDialog());
        
        // Close search button click - hide search bar with slide animation
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
                    // Restore filtered or team-based view depending on filter mode
                    if (isFilterMode) {
                        applyFiltersAndSort();
                    } else {
                        filterPlayersByTeam();
                    }
                } else {
                    // Load all players if not already loaded (and not currently loading)
                    if (allPlayersCache.isEmpty() && !isLoadingAllPlayers) {
                        loadAllPlayersForCurrentTeam(() -> filterPlayersWithSearchAndFilters(query));
                    } else if (!isLoadingAllPlayers) {
                        filterPlayersWithSearchAndFilters(query);
                    } else {
                        Log.d(LOG_TAG, "Already loading players, skipping duplicate search request");
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });

        // Initialize Mobile Ads SDK
        // MobileAds.initialize(this, initializationStatus -> Log.d(LOG_TAG, "Mobile Ads SDK initialized."));
        //
        // nativeAdViewBottom = findViewById(R.id.native_ad_view_bottom);
        // loadNativeAd("ca-app-pub-3940256099942544/2247696110", nativeAdViewBottom);

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
        fullPlayerList = new ArrayList<>();
        allPlayersCache = new ArrayList<>();
        recyclerView = findViewById(R.id.rec_view_fpl);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Initialize adapter once here with empty list
        formerPlayerRecAdapter = new FormerPlayerRecAdapter(this, formerPlayerList, managerId, team, barTeam != null ? barTeam : "First Team", 0);
        recyclerView.setAdapter(formerPlayerRecAdapter);
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

    public void refreshPlayerList() {
        Log.d(LOG_TAG, "refreshPlayerList called");
        if (barTeam != null && barTeam.equals("Youth Team")) {
            listFormerYouthTeamPlayers(0);
        } else {
            listFormerFirstTeamPlayers(0);
        }
    }

    /**
     * Show search bar with animation
     */
    private void showSearchBar() {
        if (isSearchMode) return;
        
        isSearchMode = true;
        
        // Hide team navigation with slide animation
        teamNavigationContainer.animate()
                .translationX(-teamNavigationContainer.getWidth())
                .alpha(0f)
                .setDuration(300);
        
        // Show search bar container
        searchBarContainer.setVisibility(View.VISIBLE);
        searchBarContainer.animate()
                .alpha(1f)
                .setDuration(300);
        
        // Show close button
        closeSearchButton.setVisibility(View.VISIBLE);
        closeSearchButton.animate()
                .alpha(1f)
                .setDuration(300);
        
        // Focus search bar and show keyboard
        searchBar.requestFocus();
        android.view.inputmethod.InputMethodManager imm = 
            (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(searchBar, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }
        
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
                .withEndAction(() -> searchBarContainer.setVisibility(View.GONE));
        
        // Animate close button fading out
        closeSearchButton.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> closeSearchButton.setVisibility(View.GONE));
        
        // Animate team navigation sliding back (only if not in filter mode)
        if (!isFilterMode) {
            teamNavigationContainer.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(300);
        }
        
        // Restore team-based view (only if not in filter mode)
        // If fullPlayerList is empty, refresh the data for the CURRENT team instead of showing an empty list
        if (!isFilterMode) {
            if (fullPlayerList.isEmpty()) {
                Log.d(LOG_TAG, "fullPlayerList is empty, refreshing data for current team");
                // Get current team from the UI
                String currentTeam = teamText.getText().toString();
                if (currentTeam.equals("Youth Team")) {
                    listFormerYouthTeamPlayers(0);
                } else {
                    listFormerFirstTeamPlayers(0);
                }
            } else {
                filterPlayersByTeam();
            }
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
        Button applyFiltersButton = view.findViewById(R.id.apply_filters_button);
        
        // Set current sort option
        switch (currentSortOption) {
            case "name":
                sortRadioGroup.check(R.id.sort_name);
                sortBySelection.setText("Name");
                break;
            case "position":
                sortRadioGroup.check(R.id.sort_position);
                sortBySelection.setText("Position");
                break;
            case "overall_asc":
                sortRadioGroup.check(R.id.sort_overall_asc);
                sortBySelection.setText("Overall ↑");
                break;
            case "overall_desc":
                sortRadioGroup.check(R.id.sort_overall_desc);
                sortBySelection.setText("Overall ↓");
                break;
            default:
                sortRadioGroup.check(R.id.sort_none);
                sortBySelection.setText("None");
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
        
        // Position filter checkboxes
        String[] positions = {"GK", "LB", "LWB", "LCB", "CB", "RCB", "RWB", "RB", "LDM", "CDM", "RDM", "LM", "LCM", "CM", "RCM", "RM", "LAM", "CAM", "RAM", "LW", "LF", "CF", "RF", "RW", "LS", "ST", "RS"};
        for (String position : positions) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(position);
            checkBox.setTextColor(getResources().getColor(android.R.color.black));
            checkBox.setChecked(selectedPositions.contains(position));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedPositions.add(position);
                } else {
                    selectedPositions.remove(position);
                }
                updatePositionSelectionText(positionFilterSelection);
            });
            positionFilterContent.addView(checkBox);
        }
        
        // Position category filter checkboxes
        String[] categories = {"Goalkeepers", "Left Backs", "Right Backs", "Center Backs", "Defensive Midfielders", "Central Midfielders", "Attacking Midfielders", "Wingers", "Strikers"};
        for (String category : categories) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(category);
            checkBox.setTextColor(getResources().getColor(android.R.color.black));
            checkBox.setChecked(selectedPositionCategories.contains(category));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedPositionCategories.add(category);
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
        
        // Toggle section visibility
        sortByHeader.setOnClickListener(v -> toggleSection(sortByContent, sortByChevron));
        positionFilterHeader.setOnClickListener(v -> toggleSection(positionFilterContent, positionFilterChevron));
        positionCategoryHeader.setOnClickListener(v -> toggleSection(positionCategoryContent, positionCategoryChevron));
        
        // Clear filters button
        Button clearFiltersButton = view.findViewById(R.id.clear_filters_button);
        clearFiltersButton.setOnClickListener(v -> {
            // Clear all filter state
            currentSortOption = "none";
            selectedPositions.clear();
            selectedPositionCategories.clear();
            isFilterMode = false;
            
            filterDialog.dismiss();
            
            // Show team navigation with animation
            teamNavigationContainer.setVisibility(View.VISIBLE);
            teamNavigationContainer.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(300);
            
            // Restore team-based view
            filterPlayersByTeam();
            
            Log.d(LOG_TAG, "All filters cleared and team-based view restored");
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
                // Hide team navigation when filters are active (slide to left like search)
                if (!wasFilterMode) {
                    teamNavigationContainer.animate()
                            .translationX(-teamNavigationContainer.getWidth())
                            .alpha(0f)
                            .setDuration(300);
                }
                
                // Load all players if not already loaded (and not currently loading), then apply filters
                if (allPlayersCache.isEmpty() && !isLoadingAllPlayers) {
                    loadAllPlayersForCurrentTeam(() -> applyFiltersAndSort());
                } else if (!isLoadingAllPlayers) {
                    applyFiltersAndSort();
                } else {
                    Log.d(LOG_TAG, "Already loading players, will apply filters when load completes");
                }
            } else {
                // Show team navigation and restore team-based view (slide back from left)
                if (wasFilterMode) {
                    teamNavigationContainer.setVisibility(View.VISIBLE);
                    teamNavigationContainer.animate()
                            .translationX(0f)
                            .alpha(1f)
                            .setDuration(300);
                }
                filterPlayersByTeam();
            }
        });

        filterDialog.show();
    }

    private void toggleSection(LinearLayout content, ImageView chevron) {
        if (content.getVisibility() == View.VISIBLE) {
            content.setVisibility(View.GONE);
            chevron.animate().rotation(0f).setDuration(200).start();
        } else {
            content.setVisibility(View.VISIBLE);
            chevron.animate().rotation(180f).setDuration(200).start();
        }
    }

    private void updatePositionSelectionText(TextView textView) {
        if (selectedPositions.isEmpty()) {
            textView.setText("All");
        } else if (selectedPositions.size() == 1) {
            textView.setText(selectedPositions.get(0));
        } else {
            textView.setText(selectedPositions.size() + " selected");
        }
    }

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
     * Apply filters and sorting to the player list
     */
    private void applyFiltersAndSort() {
        List<FormerPlayer> filteredList = new ArrayList<>(allPlayersCache);
        
        // Filter by position
        if (!selectedPositions.isEmpty()) {
            List<FormerPlayer> temp = new ArrayList<>();
            for (FormerPlayer player : filteredList) {
                if (selectedPositions.contains(player.getPosition())) {
                    temp.add(player);
                }
            }
            filteredList = temp;
        }
        
        // Filter by position category
        if (!selectedPositionCategories.isEmpty()) {
            List<FormerPlayer> temp = new ArrayList<>();
            for (FormerPlayer player : filteredList) {
                String playerCategory = getPositionCategory(player.getPosition());
                if (selectedPositionCategories.contains(playerCategory)) {
                    temp.add(player);
                }
            }
            filteredList = temp;
        }
        
        // Apply sorting
        String[] positionOrder = {"GK", "LB", "LWB", "LCB", "CB", "RCB", "RWB", "RB", "LDM", "CDM", "RDM", "LM", "LCM", "CM", "RCM", "RM", "LAM", "CAM", "RAM", "LW", "LF", "CF", "RF", "RW", "LS", "ST", "RS"};
        
        switch (currentSortOption) {
            case "name":
                Collections.sort(filteredList, (p1, p2) -> {
                    String name1 = p1.getLastName() != null ? p1.getLastName() : "";
                    String name2 = p2.getLastName() != null ? p2.getLastName() : "";
                    return name1.compareToIgnoreCase(name2);
                });
                break;
            case "position":
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
        }
        
        // Update the displayed list
        formerPlayerList.clear();
        formerPlayerList.addAll(filteredList);
        
        if (formerPlayerRecAdapter != null) {
            formerPlayerRecAdapter.notifyDataSetChanged();
        }
        
        teamPlayerCount.setText(formerPlayerList.size() + " players");
        
        Log.d(LOG_TAG, "Applied filters and sort. " + formerPlayerList.size() + " players found.");
    }

    /**
     * Get position category from position initials
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

    /**
     * Load all former players (both First Team and Youth Team) from Firestore for searching
     * Note: This loads ALL former players regardless of current team view
     */
    private void loadAllPlayersForCurrentTeam(Runnable onComplete) {
        // Prevent multiple simultaneous loads
        if (isLoadingAllPlayers) {
            Log.d(LOG_TAG, "Already loading players, skipping duplicate load request");
            return;
        }
        
        isLoadingAllPlayers = true;
        allPlayersCache.clear();
        
        Log.d(LOG_TAG, "Loading all former players (both First Team and Youth Team) from Firestore for search...");
        
        frpColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<FormerPlayer> tempList = new ArrayList<>();
                        
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            FormerPlayer player = doc.toObject(FormerPlayer.class);
                            if (player != null) {
                                // Load ALL former players for search (both First Team and Youth Team)
                                tempList.add(player);
                                Log.d(LOG_TAG, "Added player: " + player.getFullName() + " (yearSigned: " + player.getYearSigned() + ")");
                            }
                        }
                        
                        // Sort by time added
                        Collections.sort(tempList, (o1, o2) -> o1.getTimeAdded().compareTo(o2.getTimeAdded()));
                        
                        allPlayersCache.addAll(tempList);
                        
                        Log.d(LOG_TAG, "Loaded " + allPlayersCache.size() + " former players (all teams) for search");
                    } else {
                        Log.d(LOG_TAG, "No former players found in Firestore");
                    }
                    
                    isLoadingAllPlayers = false;
                    
                    if (onComplete != null) {
                        onComplete.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(LOG_TAG, "Error loading players from Firestore.", e);
                    isLoadingAllPlayers = false;
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
    }

    /**
     * Filter players with both search and filters applied
     */
    private void filterPlayersWithSearchAndFilters(String query) {
        Log.d(LOG_TAG, "filterPlayersWithSearchAndFilters called with query: " + query);
        Log.d(LOG_TAG, "allPlayersCache size: " + allPlayersCache.size());
        
        formerPlayerList.clear();

        if (query == null || query.trim().isEmpty()) {
            if (isFilterMode) {
                applyFiltersAndSort();
            } else {
                filterPlayersByTeam();
            }
            return;
        }

        String searchQuery = query.toLowerCase();
        
        // Start with all players or filtered players depending on filter mode
        List<FormerPlayer> baseList = allPlayersCache;
        
        // If filters are active, apply them first
        if (isFilterMode) {
            baseList = new ArrayList<>(allPlayersCache);
            
            // Filter by position
            if (!selectedPositions.isEmpty()) {
                List<FormerPlayer> temp = new ArrayList<>();
                for (FormerPlayer player : baseList) {
                    if (selectedPositions.contains(player.getPosition())) {
                        temp.add(player);
                    }
                }
                baseList = temp;
            }
            
            // Filter by position category
            if (!selectedPositionCategories.isEmpty()) {
                List<FormerPlayer> temp = new ArrayList<>();
                for (FormerPlayer player : baseList) {
                    String playerCategory = getPositionCategory(player.getPosition());
                    if (selectedPositionCategories.contains(playerCategory)) {
                        temp.add(player);
                    }
                }
                baseList = temp;
            }
        }
        
        // Now apply search filter
        for (FormerPlayer player : baseList) {
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
            
            if (matches) {
                formerPlayerList.add(player);
                Log.d(LOG_TAG, "Match found: " + player.getFullName());
            }
        }
        
        // Apply sorting if active
        if (isFilterMode) {
            String[] positionOrder = {"GK", "LB", "LWB", "LCB", "CB", "RCB", "RWB", "RB", "LDM", "CDM", "RDM", "LM", "LCM", "CM", "RCM", "RM", "LAM", "CAM", "RAM", "LW", "LF", "CF", "RF", "RW", "LS", "ST", "RS"};
            
            switch (currentSortOption) {
                case "name":
                    Collections.sort(formerPlayerList, (p1, p2) -> {
                        String name1 = p1.getLastName() != null ? p1.getLastName() : "";
                        String name2 = p2.getLastName() != null ? p2.getLastName() : "";
                        return name1.compareToIgnoreCase(name2);
                    });
                    break;
                case "position":
                    Collections.sort(formerPlayerList, (p1, p2) -> {
                        String pos1 = p1.getPosition() != null ? p1.getPosition() : "";
                        String pos2 = p2.getPosition() != null ? p2.getPosition() : "";
                        int index1 = getPositionIndex(pos1, positionOrder);
                        int index2 = getPositionIndex(pos2, positionOrder);
                        return Integer.compare(index1, index2);
                    });
                    break;
                case "overall_asc":
                    Collections.sort(formerPlayerList, (p1, p2) -> Integer.compare(p1.getOverall(), p2.getOverall()));
                    break;
                case "overall_desc":
                    Collections.sort(formerPlayerList, (p1, p2) -> Integer.compare(p2.getOverall(), p1.getOverall()));
                    break;
            }
        }
        
        if (formerPlayerRecAdapter != null) {
            formerPlayerRecAdapter.notifyDataSetChanged();
        }
        
        teamPlayerCount.setText(formerPlayerList.size() + " players");
        
        Log.d(LOG_TAG, "Filtered " + formerPlayerList.size() + " players with search" + (isFilterMode ? " and filters" : ""));
    }

    /**
     * Filter players by current team (restore team-based view)
     */
    private void filterPlayersByTeam() {
        formerPlayerList.clear();
        formerPlayerList.addAll(fullPlayerList);
        
        if (formerPlayerRecAdapter != null) {
            formerPlayerRecAdapter.notifyDataSetChanged();
        }
        
        teamPlayerCount.setText(formerPlayerList.size() + " players");
    }

    private void listFormerYouthTeamPlayers(final int buttonInt) {
        Log.d(LOG_TAG, "Listing former Youth Team players.");
        ytPlayerList.clear();
        fullPlayerList.clear();
        // Note: allPlayersCache is NOT cleared here because it contains all teams for search
        
        // Close search mode when navigating teams
        if (isSearchMode) {
            hideSearchBar();
        }
        
        // Reset filter mode when navigating teams
        if (isFilterMode) {
            isFilterMode = false;
            currentSortOption = "none";
            selectedPositions.clear();
            selectedPositionCategories.clear();
        }

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
                                    fullPlayerList.add(player);
                                }
                            }
                            
                            // Sort both lists by time added to maintain consistent ordering
                            Comparator<FormerPlayer> timeAddedComparator = new Comparator<FormerPlayer>() {
                                @Override
                                public int compare(FormerPlayer o1, FormerPlayer o2) {
                                    return o1.getTimeAdded().compareTo(o2.getTimeAdded());
                                }
                            };
                            Collections.sort(ytPlayerList, timeAddedComparator);
                            Collections.sort(fullPlayerList, timeAddedComparator);
                            
                            // Update displayed list
                            formerPlayerList.clear();
                            formerPlayerList.addAll(ytPlayerList);

                            // Update adapter data instead of creating new instance
                            if (formerPlayerRecAdapter != null) {
                                formerPlayerRecAdapter.updateData(formerPlayerList, "Youth Team", buttonInt);
                                formerPlayerRecAdapter.notifyDataSetChanged();
                            }
                            teamPlayerCount.setText(ytPlayerList.size() + " players");
                            Log.d(LOG_TAG, "Former Youth Team players listed successfully.");
                        } else {
                            teamPlayerCount.setText(ytPlayerList.size() + " players");
                            Log.w(LOG_TAG, "No former Youth Team players found.");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error fetching Youth Team players: " + e.getMessage(), e));
    }

    private void listFormerFirstTeamPlayers(final int buttonInt) {
        Log.d(LOG_TAG, "Listing former First Team players.");
        ftPlayerList.clear();
        fullPlayerList.clear();
        // Note: allPlayersCache is NOT cleared here because it contains all teams for search
        
        // Close search mode when navigating teams
        if (isSearchMode) {
            hideSearchBar();
        }
        
        // Reset filter mode when navigating teams
        if (isFilterMode) {
            isFilterMode = false;
            currentSortOption = "none";
            selectedPositions.clear();
            selectedPositionCategories.clear();
        }

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
                                    fullPlayerList.add(player);
                                }
                            }
                            
                            // Sort both lists by time added to maintain consistent ordering
                            Comparator<FormerPlayer> timeAddedComparator = new Comparator<FormerPlayer>() {
                                @Override
                                public int compare(FormerPlayer o1, FormerPlayer o2) {
                                    return o1.getTimeAdded().compareTo(o2.getTimeAdded());
                                }
                            };
                            Collections.sort(ftPlayerList, timeAddedComparator);
                            Collections.sort(fullPlayerList, timeAddedComparator);
                            
                            // Update displayed list
                            formerPlayerList.clear();
                            formerPlayerList.addAll(ftPlayerList);
                            
                            // Update adapter data instead of creating new instance
                            if (formerPlayerRecAdapter != null) {
                                formerPlayerRecAdapter.updateData(formerPlayerList, "First Team", buttonInt);
                                formerPlayerRecAdapter.notifyDataSetChanged();
                            }
                            teamPlayerCount.setText(ftPlayerList.size() + " players");
                            Log.d(LOG_TAG, "Former First Team players listed successfully.");
                        } else {
                            teamPlayerCount.setText(ftPlayerList.size() + " players");
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
                                        fullPlayerList.add(player);
                                        teamText.setText("First Team");
                                    }
                                } else if (barTeam.equals("Youth Team")) {
                                    if (player.getYearSigned().equals("0")) {
                                        ytPlayerList.add(player);
                                        fullPlayerList.add(player);
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
                                Comparator<FormerPlayer> timeAddedComparator = new Comparator<FormerPlayer>() {
                                    @Override
                                    public int compare(FormerPlayer o1, FormerPlayer o2) {
                                        return o1.getTimeAdded().compareTo(o2.getTimeAdded());
                                    }
                                };
                                Collections.sort(ftPlayerList, timeAddedComparator);
                                Collections.sort(fullPlayerList, timeAddedComparator);
                                
                                // Update displayed list
                                formerPlayerList.clear();
                                formerPlayerList.addAll(ftPlayerList);
                                // Update adapter instead of creating new instance
                                if (formerPlayerRecAdapter != null) {
                                    formerPlayerRecAdapter.updateData(formerPlayerList, "First Team", 0);
                                    formerPlayerRecAdapter.notifyDataSetChanged();
                                }
                                teamPlayerCount.setText(ftPlayerList.size() + " players");
                            } else {
                                Comparator<FormerPlayer> timeAddedComparator = new Comparator<FormerPlayer>() {
                                    @Override
                                    public int compare(FormerPlayer o1, FormerPlayer o2) {
                                        return o1.getTimeAdded().compareTo(o2.getTimeAdded());
                                    }
                                };
                                Collections.sort(ytPlayerList, timeAddedComparator);
                                Collections.sort(fullPlayerList, timeAddedComparator);
                                
                                // Update displayed list
                                formerPlayerList.clear();
                                formerPlayerList.addAll(ytPlayerList);
                                // Update adapter instead of creating new instance
                                if (formerPlayerRecAdapter != null) {
                                    formerPlayerRecAdapter.updateData(ytPlayerList, "Youth Team", 0);
                                    formerPlayerRecAdapter.notifyDataSetChanged();
                                }
                                teamPlayerCount.setText(ytPlayerList.size() + " players");
                            }
                        } else {
                            teamPlayerCount.setText(ftPlayerList.size() + " players");
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
        Log.d(LOG_TAG, "onResume called: Clearing player lists.");
        ftPlayerList.clear();
        ytPlayerList.clear();
        allPlayersCache.clear(); // Clear cached all-players list to ensure deleted players don't appear in search
        isLoadingAllPlayers = false; // Reset loading flag
    }

    @Override
    protected void onDestroy() {
        // if (nativeAdBottom != null) nativeAdBottom.destroy();
        Log.i(LOG_TAG, "FormerPlayersListActivity destroyed.");
        super.onDestroy();
    }
}