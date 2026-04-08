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
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

// import com.google.android.gms.ads.AdLoader;
// import com.google.android.gms.ads.AdRequest;
// import com.google.android.gms.ads.LoadAdError;
// import com.google.android.gms.ads.MobileAds;
// import com.google.android.gms.ads.nativead.NativeAd;
// import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
import java.util.Map;
import java.util.Objects;

import enumeration.PositionEnum;
import model.FirstTeamPlayer;
import model.Manager;
import ui.FirstTeamPlayerRecAdapter;
import util.NationalityFlagUtil;
import util.UserApi;

public class FirstTeamListActivity extends AppCompatActivity {

    private static final String LOG_TAG = "RAFI|FirstTeamList";
    private static final long CREATE_PLAYER_TIMEOUT_MS = 20000L;
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
    private List<FirstTeamPlayer> fullPlayerList; // Store all players for filtering (current year)
    private List<FirstTeamPlayer> allYearsPlayerList; // Store all players across all years
    private boolean isLoadingAllPlayers = false; // Flag to prevent multiple simultaneous loads
    private RecyclerView recyclerView;
    private FirstTeamPlayerRecAdapter firstTeamPlayerRecAdapter;

    private Button prevYearButton;
    private Button nextYearButton;
    private TextView yearText;
    private TextView yearPlayerCount;
    private Button addPlayerButton;
    private EditText searchBar;
    private LinearLayout yearNavigationContainer;
    private LinearLayout searchBarContainer;
    private ImageButton searchIconButton;
    private ImageButton closeSearchButton;
    private ImageButton filterIconButton;
    private boolean isSearchMode = false;
    private boolean isFilterMode = false;
    
    // Filter state
    private String currentSortOption = "none";
    private List<String> selectedPositions = new ArrayList<>(); // Multiple positions can be selected
    private List<String> selectedPositionCategories = new ArrayList<>(); // Multiple categories can be selected

    private AlertDialog.Builder builder;
    private BottomSheetDialog dialog;

    private EditText firstName;
    private EditText lastName;
    private TextView positionPicker;
    private EditText number;
    private AutoCompleteTextView nationality;
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
    // private NativeAdView nativeAdViewBottom;

    private BottomSheetDialog createDialog;
    private final Handler createPlayerTimeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable createPlayerTimeoutRunnable;
    private boolean isCreatePlayerRequestInFlight = false;

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

        yearNavigationContainer = findViewById(R.id.year_navigation_container);
        searchBarContainer = findViewById(R.id.search_bar_container);
        searchIconButton = findViewById(R.id.search_icon_button);
        closeSearchButton = findViewById(R.id.close_search_button);
        filterIconButton = findViewById(R.id.filter_icon_button);
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
        // MobileAds.initialize(this, initializationStatus -> Log.d(LOG_TAG, "Mobile Ads SDK initialized."));
        //
        // nativeAdViewBottom = findViewById(R.id.native_ad_view_bottom);
        // loadNativeAd("ca-app-pub-3940256099942544/2247696110", nativeAdViewBottom);

        slideLeft = AnimationUtils.loadAnimation(FirstTeamListActivity.this, R.anim.slide_left);
        slideRight = AnimationUtils.loadAnimation(FirstTeamListActivity.this, R.anim.slide_right);

        View.OnClickListener prevYearListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentYear == null || minYearText == null) {
                    Toast.makeText(FirstTeamListActivity.this, "Please wait for data to load.", Toast.LENGTH_SHORT).show();
                    return;
                }
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
                if (currentYear == null) {
                    Toast.makeText(FirstTeamListActivity.this, "Please wait for data to load.", Toast.LENGTH_SHORT).show();
                    return;
                }
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
        fullPlayerList = new ArrayList<>();
        allYearsPlayerList = new ArrayList<>();
        recyclerView = findViewById(R.id.rec_view_ftp);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Initialize adapter once here
        firstTeamPlayerRecAdapter = new FirstTeamPlayerRecAdapter(this, playerList, managerId, team, currentYear != null ? currentYear : "", 0, 0);
        recyclerView.setAdapter(firstTeamPlayerRecAdapter);

        // Initialize search bar
        searchBar = findViewById(R.id.search_bar_ftp);
        
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
                    // Restore filtered or year-based view depending on filter mode
                    if (isFilterMode) {
                        applyFiltersAndSort();
                    } else {
                        filterPlayersByYear();
                    }
                } else {
                    // Load all players if not already loaded
                    if (allYearsPlayerList.isEmpty()) {
                        loadAllPlayers(() -> filterPlayersWithSearchAndFilters(query));
                    } else {
                        filterPlayersWithSearchAndFilters(query);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });
    }

    /**
     * Show search bar with animation
     * - Search icon STAYS in place
     * - Search bar fades in
     * - Year selector slides to the right
     * - Filter icon STAYS in place
     * - Close button appears next to filter icon
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
        
        // Animate year navigation sliding to the right
        yearNavigationContainer.animate()
                .translationX(yearNavigationContainer.getWidth())
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
        
        Log.d(LOG_TAG, "Search bar shown - search and filter icons stay in place");
    }

    /**
     * Hide search bar with animation
     * - Search bar fades out
     * - Close button fades out
     * - Year selector slides back from the right
     * - Search and filter icons remain in place
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
        
        // Only animate year navigation if it was actually hidden (translationX != 0)
        if (!isFilterMode && yearNavigationContainer.getTranslationX() != 0f) {
            yearNavigationContainer.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(300);
        }
        
        // Restore year-based view (only if not in filter mode)
        // If fullPlayerList is empty (e.g., user clicked search before data loaded),
        // refresh the data instead of showing an empty list
        if (!isFilterMode) {
            if (fullPlayerList.isEmpty() && currentYear != null) {
                Log.d(LOG_TAG, "fullPlayerList is empty, refreshing data for current year");
                listPlayers(0);
            } else {
                filterPlayersByYear();
            }
        }
        
        Log.d(LOG_TAG, "Search bar hidden - icons stay in place");
    }

    /**
     * Show filter bottom sheet dialog with expandable sections
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
        
        // Handle dialog dismissal - keep year navigation hidden if filters active
        filterDialog.setOnDismissListener(dialog -> {
            if (isFilterMode) {
                yearNavigationContainer.setVisibility(View.INVISIBLE);
                yearNavigationContainer.setAlpha(0f);
                yearNavigationContainer.setTranslationX(yearNavigationContainer.getWidth());
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

        // Sort By Header Click - Toggle expand/collapse with chevron animation
        sortByHeader.setOnClickListener(v -> {
            toggleSection(sortByContent, sortByChevron);
        });

        // Position Filter Header Click - Toggle expand/collapse with chevron animation
        positionFilterHeader.setOnClickListener(v -> {
            toggleSection(positionFilterContent, positionFilterChevron);
        });

        // Position Category Header Click - Toggle expand/collapse with chevron animation
        positionCategoryHeader.setOnClickListener(v -> {
            toggleSection(positionCategoryContent, positionCategoryChevron);
        });

        // Clear filters button
        clearFiltersButton.setOnClickListener(v -> {
            currentSortOption = "none";
            selectedPositions.clear();
            selectedPositionCategories.clear();
            isFilterMode = false;
            
            filterDialog.dismiss();
            
            // Show year navigation with animation
            yearNavigationContainer.setVisibility(View.VISIBLE);
            yearNavigationContainer.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(300);
            
            filterPlayersByYear();
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
                // Hide year navigation when filters are active (only animate if not already hidden)
                if (!wasFilterMode) {
                    yearNavigationContainer.animate()
                            .translationX(yearNavigationContainer.getWidth())
                            .alpha(0f)
                            .setDuration(300);
                }
                
                // Load all players if not already loaded, then apply filters
                if (allYearsPlayerList.isEmpty()) {
                    loadAllPlayers(() -> applyFiltersAndSort());
                } else {
                    applyFiltersAndSort();
                }
            } else {
                // Show year navigation and restore year-based view (only animate if it was hidden)
                if (wasFilterMode) {
                    yearNavigationContainer.setVisibility(View.VISIBLE);
                    yearNavigationContainer.animate()
                            .translationX(0f)
                            .alpha(1f)
                            .setDuration(300);
                }
                filterPlayersByYear();
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
        
        // Start with all years player list
        List<FirstTeamPlayer> filteredList = new ArrayList<>(allYearsPlayerList);
        
        // Apply position filter (multiple selections)
        if (!selectedPositions.isEmpty()) {
            List<FirstTeamPlayer> temp = new ArrayList<>();
            for (FirstTeamPlayer player : filteredList) {
                if (player.getPosition() != null && selectedPositions.contains(player.getPosition())) {
                    temp.add(player);
                }
            }
            filteredList = temp;
        }
        
        // Apply position category filter (multiple selections)
        if (!selectedPositionCategories.isEmpty()) {
            List<FirstTeamPlayer> temp = new ArrayList<>();
            for (FirstTeamPlayer player : filteredList) {
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
        if (firstTeamPlayerRecAdapter != null) {
            firstTeamPlayerRecAdapter.notifyDataSetChanged();
        }
        
        // Update player count
        if (yearPlayerCount != null) {
            yearPlayerCount.setText(playerList.size() + " players");
        }
        
        Log.d(LOG_TAG, "Applied filters and sort. " + playerList.size() + " players found.");
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
            return Integer.MAX_VALUE; // Unknown positions go to the end
        }
        
        for (int i = 0; i < positionOrder.length; i++) {
            if (positionOrder[i].equals(position)) {
                return i;
            }
        }
        
        return Integer.MAX_VALUE; // Position not in array goes to the end
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

    public void refreshPlayerList() {
        refreshPlayerList(null);
    }

    public void refreshPlayerList(Runnable onComplete) {
        Log.d(LOG_TAG, "refreshPlayerList called");
        listPlayers(0, onComplete);
    }

    private void listPlayers(final int buttonInt) {
        listPlayers(buttonInt, null);
    }

    private void listPlayers(final int buttonInt, Runnable onComplete) {
        playerList.clear();
        fullPlayerList.clear();

        // Close search mode when navigating years
        if (isSearchMode) {
            hideSearchBar();
        }
        
        // Reset filter mode when navigating years
        if (isFilterMode) {
            isFilterMode = false;
            currentSortOption = "none";
            selectedPositions.clear();
            selectedPositionCategories.clear();
        }
        
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
                                    fullPlayerList.add(player);
                                }
                            }
                            Collections.sort(fullPlayerList, new Comparator<FirstTeamPlayer>() {
                                @Override
                                public int compare(FirstTeamPlayer o1, FirstTeamPlayer o2) {
                                    return o1.getTimeAdded().compareTo(o2.getTimeAdded());
                                }
                            });
                            
                            // Always show all players for the current year when listPlayers is called
                            // (search bar is cleared when navigating years)
                            playerList.addAll(fullPlayerList);
                            
                            yearText.setText(currentYear);
                            
                            // Update adapter metadata instead of creating new instance
                            if (firstTeamPlayerRecAdapter != null) {
                                firstTeamPlayerRecAdapter.updateMetadata(currentYear, buttonInt, maxId);
                                firstTeamPlayerRecAdapter.notifyDataSetChanged();
                            }
                            yearPlayerCount.setText(playerList.size() + " players");
                            
                            // Scroll to top when year changes
                            if (recyclerView != null) {
                                recyclerView.scrollToPosition(0);
                            }

                            // Call completion callback if provided
                            if (onComplete != null) {
                                onComplete.run();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(LOG_TAG, "Error fetching players from Firestore.", e);
                    // Call completion callback on failure too
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
    }

    /**
     * Load all players across all years from Firestore
     */
    private void loadAllPlayers(Runnable onComplete) {
        // Always clear to prevent duplicates
        allYearsPlayerList.clear();
        
        Log.d(LOG_TAG, "Loading all players from Firestore...");
        
        collectionReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Use a temporary list to avoid any potential concurrent modification
                        List<FirstTeamPlayer> tempList = new ArrayList<>();
                        
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            FirstTeamPlayer player = doc.toObject(FirstTeamPlayer.class);
                            if (player != null) {
                                tempList.add(player);
                            }
                        }
                        
                        // Sort by time added
                        Collections.sort(tempList, new Comparator<FirstTeamPlayer>() {
                            @Override
                            public int compare(FirstTeamPlayer o1, FirstTeamPlayer o2) {
                                return o1.getTimeAdded().compareTo(o2.getTimeAdded());
                            }
                        });
                        
                        // Now add all to the main list at once
                        allYearsPlayerList.addAll(tempList);
                        
                        Log.d(LOG_TAG, "Loaded " + allYearsPlayerList.size() + " unique players across all years");
                    } else {
                        Log.d(LOG_TAG, "No players found in Firestore");
                    }
                    
                    if (onComplete != null) {
                        onComplete.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(LOG_TAG, "Error loading all players from Firestore.", e);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
    }

    /**
     * Filter players across all years based on search query
     * Searches in: first name, last name, full name
     */
    private void filterPlayersAllYears(String query) {
        playerList.clear();
        
        if (query == null || query.trim().isEmpty()) {
            // If search is empty, restore year-based view
            filterPlayersByYear();
            return;
        }
        
        // Filter players by query (case-insensitive)
        String searchQuery = query.toLowerCase().trim();
        for (FirstTeamPlayer player : allYearsPlayerList) {
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
                playerList.add(player);
            }
        }
        
        // Update adapter
        if (firstTeamPlayerRecAdapter != null) {
            firstTeamPlayerRecAdapter.notifyDataSetChanged();
        }
        
        // Update player count
        if (yearPlayerCount != null) {
            yearPlayerCount.setText(playerList.size() + " players");
        }
        
        Log.d(LOG_TAG, "Filtered " + playerList.size() + " players from all years for query: " + query);
    }

    /**
     * Filter players with both search and filters applied
     * Searches across all years for this manager's first team players
     * If filters are active, applies filters first then searches
     */
    private void filterPlayersWithSearchAndFilters(String query) {
        Log.d(LOG_TAG, "filterPlayersWithSearchAndFilters called with query: " + query);
        Log.d(LOG_TAG, "allYearsPlayerList size: " + allYearsPlayerList.size());
        
        playerList.clear();
        
        if (query == null || query.trim().isEmpty()) {
            // If search is empty, restore filtered or year-based view
            if (isFilterMode) {
                applyFiltersAndSort();
            } else {
                filterPlayersByYear();
            }
            return;
        }
        
        // Start with all years for this manager's first team
        List<FirstTeamPlayer> baseList = new ArrayList<>(allYearsPlayerList);

        Log.d(LOG_TAG, "Base list size before filters: " + baseList.size());
        
        // Apply filters if in filter mode
        if (isFilterMode) {
            // Apply position filter (multiple selections)
            if (!selectedPositions.isEmpty()) {
                List<FirstTeamPlayer> temp = new ArrayList<>();
                for (FirstTeamPlayer player : baseList) {
                    if (player.getPosition() != null && selectedPositions.contains(player.getPosition())) {
                        temp.add(player);
                    }
                }
                baseList = temp;
                Log.d(LOG_TAG, "After position filter: " + baseList.size());
            }
            
            // Apply position category filter (multiple selections)
            if (!selectedPositionCategories.isEmpty()) {
                List<FirstTeamPlayer> temp = new ArrayList<>();
                for (FirstTeamPlayer player : baseList) {
                    if (player.getPosition() != null) {
                        String playerCategory = getPositionCategory(player.getPosition());
                        if (selectedPositionCategories.contains(playerCategory)) {
                            temp.add(player);
                        }
                    }
                }
                baseList = temp;
                Log.d(LOG_TAG, "After category filter: " + baseList.size());
            }
        }
        
        // Now apply search query to the base list (across all years within context)
        String searchQuery = query.toLowerCase().trim();
        for (FirstTeamPlayer player : baseList) {
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
                Log.d(LOG_TAG, "Match found: " + player.getFullName() + " (Year: " + player.getYearSigned() + ", ID: " + player.getId() + ")");
                playerList.add(player);
            }
        }
        
        Log.d(LOG_TAG, "Total matches found: " + playerList.size());
        
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
        if (firstTeamPlayerRecAdapter != null) {
            firstTeamPlayerRecAdapter.notifyDataSetChanged();
        }
        
        // Update player count
        if (yearPlayerCount != null) {
            yearPlayerCount.setText(playerList.size() + " players");
        }
        
        Log.d(LOG_TAG, "Filtered " + playerList.size() + " players with search across all years" + (isFilterMode ? " and filters" : ""));
    }

    /**
     * Filter players by current year (restore year-based view)
     */
    private void filterPlayersByYear() {
        playerList.clear();
        playerList.addAll(fullPlayerList);
        
        // Update adapter
        if (firstTeamPlayerRecAdapter != null) {
            firstTeamPlayerRecAdapter.notifyDataSetChanged();
        }
        
        // Update player count
        if (yearPlayerCount != null) {
            yearPlayerCount.setText(playerList.size() + " players");
        }
    }

    private void createPopupDialog() {
        createDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.create_first_team_player_popup, null);
        createDialog.setContentView(view);

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

        String[] countrySuggestions = getResources().getStringArray(R.array.nationalities);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, countrySuggestions);

        nationality.setAdapter(adapter);

        nationality.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) nationality.showDropDown();
        });

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
                if (isCreatePlayerRequestInFlight) {
                    Log.w(LOG_TAG, "Create ignored: request already in flight.");
                    return;
                }
                if (!lastName.getText().toString().isEmpty() &&
                        !nationality.getText().toString().isEmpty() &&
                        !positionPicker.getText().toString().isEmpty() &&
                        !overall.getText().toString().isEmpty() &&
                        !yearSigned.getText().toString().isEmpty()) {
                    // Disable to prevent duplicate taps
                    createPlayerButton.setEnabled(false);
                    createPlayerButton.setText("Saving...");
                    createPlayer();
                } else {
                    Toast.makeText(FirstTeamListActivity.this, "Last Name/Nickname, Nationality, Position, Overall and Year Signed are required", Toast.LENGTH_LONG)
                            .show();
                }
            }
        });

        createDialog.show();
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
        Map<String, String> variantMap = NationalityFlagUtil.getVariantToStandardMap();
        String nationalityInput = variantMap.getOrDefault(nationalityPlayer, nationalityPlayer);

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
        player.setNationality(nationalityInput);
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

        isCreatePlayerRequestInFlight = true;
        startCreatePlayerTimeout();

        collectionReference.add(player)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        completeCreatePlayerRequest();
                        Log.i(LOG_TAG, "Player successfully added to Firestore. Document ID: " + documentReference.getId());
                        try {
                            // Update currentYear to the created player's year before refreshing
                            currentYear = ySignedPlayer;
                            // Show success message
                            Toast.makeText(FirstTeamListActivity.this, "Player created successfully!", Toast.LENGTH_SHORT).show();
                            // Trigger refresh and dismiss dialog after completion
                            refreshPlayerList(() -> {
                                // Dismiss dialog after refresh completes
                                if (createDialog != null && createDialog.isShowing()) {
                                    Log.d(LOG_TAG, "Dismissing create dialog after refresh.");
                                    createDialog.dismiss();
                                }
                            });
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "Error in onSuccess callback", e);
                            if (createDialog != null && createDialog.isShowing()) {
                                createDialog.dismiss();
                            }
                            Toast.makeText(FirstTeamListActivity.this, "Player created but error occurred: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        completeCreatePlayerRequest();
                        Log.e(LOG_TAG, "Error creating player", e);
                        if (createDialog != null && createDialog.isShowing()) {
                            createDialog.dismiss();
                        }
                        createPlayerButton.setText("CREATE PLAYER");
                        createPlayerButton.setEnabled(true);
                        Toast.makeText(FirstTeamListActivity.this, "Failed to create player: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void startCreatePlayerTimeout() {
        cancelCreatePlayerTimeout();
        createPlayerTimeoutRunnable = () -> {
            if (!isCreatePlayerRequestInFlight) {
                return;
            }
            isCreatePlayerRequestInFlight = false;
            Log.e(LOG_TAG, "Create player request timed out after " + CREATE_PLAYER_TIMEOUT_MS + "ms.");

            if (isFinishing() || isDestroyed()) {
                return;
            }

            if (createPlayerButton != null) {
                createPlayerButton.setEnabled(true);
                createPlayerButton.setText("CREATE PLAYER");
            }
            Toast.makeText(FirstTeamListActivity.this,
                    "Save is taking too long. Please check your connection and try again.",
                    Toast.LENGTH_LONG).show();
        };
        createPlayerTimeoutHandler.postDelayed(createPlayerTimeoutRunnable, CREATE_PLAYER_TIMEOUT_MS);
    }

    private void cancelCreatePlayerTimeout() {
        if (createPlayerTimeoutRunnable != null) {
            createPlayerTimeoutHandler.removeCallbacks(createPlayerTimeoutRunnable);
            createPlayerTimeoutRunnable = null;
        }
    }

    private void completeCreatePlayerRequest() {
        isCreatePlayerRequestInFlight = false;
        cancelCreatePlayerTimeout();
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
                Intent homeIntent = new Intent(FirstTeamListActivity.this, DashboardActivity.class);
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

        db.collection("YouthTeamPlayers").whereEqualTo("userId", currentUserId)
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ytPlayersExist = !Objects.requireNonNull(task.getResult()).isEmpty();
                    }
                });

        db.collection("ShortlistedPlayers").whereEqualTo("userId", currentUserId)
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        shPlayersExist = !Objects.requireNonNull(task.getResult()).isEmpty();
                    }
                });

        db.collection("Managers").whereEqualTo("userId", currentUserId)
                .whereEqualTo("id", managerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Manager theManager = queryDocumentSnapshots.getDocuments()
                                .get(0).toObject(Manager.class);
                        managerNameHeader.setText(theManager.getFullName());
                        teamHeader.setText(theManager.getTeam());
                    }
                });

        fetchPlayersAndFixIds(); // single source of data load and UI refresh
    }

    private void fetchPlayersAndFixIds() {
        collectionReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                        List<FirstTeamPlayer> ftplayers = new ArrayList<>();
                        for (DocumentSnapshot doc : docs) {
                            FirstTeamPlayer player = doc.toObject(FirstTeamPlayer.class);
                            ftplayers.add(player);
                        }
                        findMinYearSigned(ftplayers);
                        findMaxPlayerId(ftplayers);

                        // Ensure currentYear has a fallback value
                        if (barYear != null) {
                            currentYear = barYear;
                        } else if (minYearText != null) {
                            currentYear = minYearText;
                        } else {
                            currentYear = "2020/21"; // Default fallback
                        }

                        List<Task<Void>> updateTasks = new ArrayList<>();

                        for (DocumentSnapshot ds : docs) {
                            FirstTeamPlayer ftp = ds.toObject(FirstTeamPlayer.class);
                            assert ftp != null;
                            if (ftp.getId() == 0) {
                                maxId++;
                                Task<Void> updateTask = collectionReference.document(ds.getId())
                                        .update("id", maxId);
                                updateTasks.add(updateTask);
                            }
                        }

                        if (!updateTasks.isEmpty()) {
                            com.google.android.gms.tasks.Tasks.whenAllComplete(updateTasks)
                                    .addOnSuccessListener(tasks -> {
                                        Log.d(LOG_TAG, "All player IDs fixed. Proceeding to display.");
                                        listPlayers(0);
                                    });
                        } else {
                            listPlayers(0);
                        }
                    } else {
                        currentYear = (barYear != null) ? barYear : "2020/21";
                        listPlayers(0);
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
        allYearsPlayerList.clear(); // Clear cached all-years list to ensure deleted players don't appear in search
        isLoadingAllPlayers = false; // Reset loading flag
    }

    @Override
    protected void onDestroy() {
        cancelCreatePlayerTimeout();
        super.onDestroy();
    }
}