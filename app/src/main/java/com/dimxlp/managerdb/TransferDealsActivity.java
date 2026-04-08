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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import enumeration.PositionEnum;
import model.Manager;
import model.Transfer;
import ui.TransferDealsRecAdapter;
import util.UserApi;

public class TransferDealsActivity extends AppCompatActivity {

    private static final String LOG_TAG = "RAFI|TransferDeals";
    private FirebaseAuth firebaseAuth;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference transfersColRef = db.collection("Transfers");
    private CollectionReference ftPlayersColRef = db.collection("FirstTeamPlayers");
    private CollectionReference ytPlayersColRef = db.collection("YouthTeamPlayers");
    private CollectionReference shPlayersColRef = db.collection("ShortlistedPlayers");
    private CollectionReference managersColRef = db.collection("Managers");

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navView;

    private RecyclerView recyclerView;
    private TransferDealsRecAdapter transferDealsRecAdapter;

    private Button prevButton;
    private Button nextButton;
    private LinearLayout transferTypeContainer;
    private LinearLayout navigationContainer;
    private TextView transferType;
    private TextView transferCount;
    private boolean ftPlayersExist;
    private boolean ytPlayersExist;
    private List<Transfer> transferList;
    private List<Transfer> fullTransferList; // Store all transfers for filtering
    private List<Transfer> allArrivedTransfers; // Store all arrived transfers
    private List<Transfer> allLeftTransfers; // Store all left transfers
    private int maxId;
    private boolean shPlayersExist;
    private long managerId;
    private String team;

    private EditText searchBar;
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
    private String currentTransferType = "Arrived"; // Track current transfer type

    private TextView managerNameHeader;
    private TextView teamHeader;
    // private NativeAdView nativeAdViewBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_deals);
        Log.i(LOG_TAG, "TransferDealsActivity launched.");

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
            Log.d(LOG_TAG, "Extras received: managerId=" + managerId + ", team=" + team);
        } else {
            Log.w(LOG_TAG, "No extras received in intent.");
        }

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_menu);

        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nvView_trf);
        setUpDrawerContent(navView);

        prevButton = findViewById(R.id.prev_button_trf);
        nextButton = findViewById(R.id.next_button_trf);

        transferTypeContainer = findViewById(R.id.transfer_type_container_trf);
        transferType = findViewById(R.id.transfer_type_trf);
        transferCount = findViewById(R.id.player_count_trf);

        List<String> transferTypes = new ArrayList<>();
        transferTypes.add("Arrived");
        transferTypes.add("Left");

        transferTypeContainer.setOnClickListener(v -> {
            if (!transferTypes.isEmpty()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Select Transfer Type");
                builder.setItems(transferTypes.toArray(new String[0]), (dialog, which) -> {
                    String type = transferTypes.get(which);
                    transferType.setText(type);

                    if (type.equals("Arrived")) {
                        listPlayersArrived(1);
                    } else {
                        listPlayersLeft(1);
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

        transferList = new ArrayList<>();
        fullTransferList = new ArrayList<>();
        allArrivedTransfers = new ArrayList<>();
        allLeftTransfers = new ArrayList<>();

        recyclerView = findViewById(R.id.rec_view_trf);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize search and filter UI components
        searchIconButton = findViewById(R.id.search_icon_button_trf);
        filterIconButton = findViewById(R.id.filter_icon_button_trf);
        closeSearchButton = findViewById(R.id.close_search_button_trf);
        navigationContainer = findViewById(R.id.navigation_container_trf);
        searchBarContainer = findViewById(R.id.search_bar_container_trf);
        searchBar = findViewById(R.id.search_bar_trf);
        
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
                        restoreCurrentTypeList();
                    }
                } else {
                    // Search across ALL transfers (both arrived and left)
                    filterTransfersWithSearchAndFilters(query);
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

        View.OnClickListener prevButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateTransferButtons(v);
                String type = transferType.getText().toString().trim();
                if (type.equals("Arrived")) {
                    transferType.setText("Left");
                    listPlayersLeft(1);
                } else {
                    transferType.setText("Arrived");
                    listPlayersArrived(1);
                }
            }
        };

        View.OnClickListener nextButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateTransferButtons(v);
                String type = transferType.getText().toString().trim();
                if (type.equals("Arrived")) {
                    transferType.setText("Left");
                    listPlayersLeft(2);
                } else {
                    transferType.setText("Arrived");
                    listPlayersArrived(2);
                }
            }
        };

        prevButton.setOnClickListener(prevButtonListener);
        nextButton.setOnClickListener(nextButtonListener);
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

    private void animateTransferButtons(View v) {
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

    private void listPlayersLeft(final int buttonInt) {
        Log.d(LOG_TAG, "Listing players who left. ButtonInt: " + buttonInt);

        currentTransferType = "Left";
        transferList.clear();
        Log.d(LOG_TAG, "Transfer list cleared.");

        // Use already-loaded allLeftTransfers instead of fetching again
        transferList.addAll(allLeftTransfers);
        fullTransferList.clear();
        fullTransferList.addAll(allLeftTransfers);
        
        transferDealsRecAdapter = new TransferDealsRecAdapter(TransferDealsActivity.this, transferList, managerId, team, buttonInt);
        recyclerView.setAdapter(transferDealsRecAdapter);
        transferDealsRecAdapter.notifyDataSetChanged();
        transferCount.setText(transferList.size() + " transfer(s)");
        
        Log.d(LOG_TAG, "Displayed " + transferList.size() + " players who left.");
    }

    private void listPlayersArrived(final int buttonInt) {
        Log.d(LOG_TAG, "Listing players who arrived. ButtonInt: " + buttonInt);

        currentTransferType = "Arrived";
        transferList.clear();
        Log.d(LOG_TAG, "Transfer list cleared.");

        // Use already-loaded allArrivedTransfers instead of fetching again
        transferList.addAll(allArrivedTransfers);
        fullTransferList.clear();
        fullTransferList.addAll(allArrivedTransfers);
        
        transferDealsRecAdapter = new TransferDealsRecAdapter(TransferDealsActivity.this, transferList, managerId, team, buttonInt);
        recyclerView.setAdapter(transferDealsRecAdapter);
        transferDealsRecAdapter.notifyDataSetChanged();
        transferCount.setText(transferList.size() + " transfer(s)");
        
        // Scroll to top when transfer type changes
        if (recyclerView != null) {
            recyclerView.scrollToPosition(0);
        }

        Log.d(LOG_TAG, "Displayed " + transferList.size() + " players who arrived.");
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
                Intent homeIntent = new Intent(TransferDealsActivity.this, DashboardActivity.class);
                homeIntent.putExtra("managerId", managerId);
                homeIntent.putExtra("team", team);
                startActivity(homeIntent);
                finish();
                break;
            case R.id.dr_manager_selection:
                Intent managerSelectionIntent = new Intent(TransferDealsActivity.this, SelectManagerActivity.class);
                startActivity(managerSelectionIntent);
                break;
            case R.id.dr_profile:
                Intent profileIntent = new Intent(TransferDealsActivity.this, ProfileActivity.class);
                profileIntent.putExtra("managerId", managerId);
                profileIntent.putExtra("team", team);
                startActivity(profileIntent);
                finish();
                break;
            case R.id.dr_first_team:
                if (ftPlayersExist) {
                    Intent firstIntent = new Intent(TransferDealsActivity.this, FirstTeamListActivity.class);
                    firstIntent.putExtra("managerId", managerId);
                    firstIntent.putExtra("team", team);
                    startActivity(firstIntent);
                    finish();
                } else {
                    Intent firstIntent = new Intent(TransferDealsActivity.this, FirstTeamActivity.class);
                    firstIntent.putExtra("managerId", managerId);
                    firstIntent.putExtra("team", team);
                    startActivity(firstIntent);
                    finish();
                }
                break;
            case R.id.dr_youth_team:
                if (ytPlayersExist) {
                    Intent youthIntent = new Intent(TransferDealsActivity.this, YouthTeamListActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", team);
                    startActivity(youthIntent);
                    finish();
                } else {
                    Intent youthIntent = new Intent(TransferDealsActivity.this, YouthTeamActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", team);
                    startActivity(youthIntent);
                    finish();
                }
                break;
            case R.id.dr_former_players:
                Intent formerIntent = new Intent(TransferDealsActivity.this, FormerPlayersListActivity.class);
                formerIntent.putExtra("managerId", managerId);
                formerIntent.putExtra("team", team);
                startActivity(formerIntent);
                finish();
                break;
            case R.id.dr_shortlist:
                if (shPlayersExist) {
                    Intent shortlistIntent = new Intent(TransferDealsActivity.this, ShortlistPlayersActivity.class);
                    shortlistIntent.putExtra("managerId", managerId);
                    shortlistIntent.putExtra("team", team);
                    startActivity(shortlistIntent);
                    finish();
                } else {
                    Intent shortlistIntent = new Intent(TransferDealsActivity.this, ShortlistActivity.class);
                    shortlistIntent.putExtra("managerId", managerId);
                    shortlistIntent.putExtra("team", team);
                    startActivity(shortlistIntent);
                    finish();
                }
                break;
            case R.id.dr_loaned_out_players:
                Intent loanIntent = new Intent(TransferDealsActivity.this, LoanedOutPlayersActivity.class);
                loanIntent.putExtra("managerId", managerId);
                loanIntent.putExtra("team", team);
                startActivity(loanIntent);
                finish();
                break;
            case R.id.dr_transfer_deals:
                break;
            case R.id.dr_support_info:
                Intent supportIntent = new Intent(TransferDealsActivity.this, SupportActivity.class);
                supportIntent.putExtra("managerId", managerId);
                supportIntent.putExtra("team", team);
                startActivity(supportIntent);
                finish();
                break;
            case R.id.dr_logout:
                if (user != null && firebaseAuth != null) {
                    firebaseAuth.signOut();
                    startActivity(new Intent(TransferDealsActivity.this, MainActivity.class));
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
        Log.d(LOG_TAG, "onStart called: Fetching data for transfers, players, and manager details.");

        shPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            shPlayersExist = !Objects.requireNonNull(task.getResult()).isEmpty();
                            Log.d(LOG_TAG, "Shortlisted players existence: " + shPlayersExist);
                        } else {
                            Log.e(LOG_TAG, "Error fetching shortlisted players.", task.getException());
                        }
                    }
                });

        ftPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            ftPlayersExist = !Objects.requireNonNull(task.getResult()).isEmpty();
                            Log.d(LOG_TAG, "First Team players existence: " + ftPlayersExist);
                        } else {
                            Log.e(LOG_TAG, "Error fetching First Team players.", task.getException());
                        }
                    }
                });

        ytPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            ytPlayersExist = !Objects.requireNonNull(task.getResult()).isEmpty();
                            Log.d(LOG_TAG, "Youth Team players existence: " + ytPlayersExist);
                        } else {
                            Log.e(LOG_TAG, "Error fetching Youth Team players.", task.getException());
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
                                Log.d(LOG_TAG, "Manager data fetched: " + manager.getFullName());
                            }
                            Manager theManager = managerList.get(0);
                            managerNameHeader.setText(theManager.getFullName());
                            teamHeader.setText(theManager.getTeam());
                            Log.d(LOG_TAG, "UI updated with manager details.");
                            findMaxIdAndFetchArrived(team);
                        } else {
                            Log.w(LOG_TAG, "No manager data found for managerId=" + managerId);
                        }
                    }
                });


        }

    private void findMaxIdAndFetchArrived(String managerTeam) {
        transfersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            List<Transfer> transfers = new ArrayList<>();
                            for (DocumentSnapshot doc: queryDocumentSnapshots) {
                                Transfer transfer = doc.toObject(Transfer.class);
                                transfers.add(transfer);
                            }
                            Log.d(LOG_TAG, "Transfers fetched for ID updates. Count: " + transfers.size());
                            findMaxTransferId(transfers);
                            for (DocumentSnapshot ds: queryDocumentSnapshots) {
                                Transfer transfer = ds.toObject(Transfer.class);
                                if (transfer.getId() == 0) {
                                    transfer.setId(maxId+1);
                                    transfersColRef.document(ds.getId()).update("id", transfer.getId());
                                    maxId++;
                                    Log.d(LOG_TAG, "Transfer ID updated: " + transfer.getFullName());
                                }
                            }
                            fetchPlayersArrived(managerTeam);
                        } else {
                            Log.w(LOG_TAG, "No transfers found for ID updates.");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error fetching transfers for ID updates.", e));
    }

    private void fetchPlayersArrived(String managerTeam) {
        Log.d(LOG_TAG, "Filtering arrivals for team: " + managerTeam);
        currentTransferType = "Arrived";
        transferList.clear();
        allArrivedTransfers.clear();
        allLeftTransfers.clear(); // Also clear left transfers to prepare for loading

        transfersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // Load ALL transfers and separate them into arrived and left
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                Transfer transfer = doc.toObject(Transfer.class);
                                
                                // Add to arrived list if current team matches
                                if (managerTeam.equals(transfer.getCurrentTeam())) {
                                    allArrivedTransfers.add(transfer);
                                }
                                
                                // Add to left list if former team matches
                                if (managerTeam.equals(transfer.getFormerTeam())) {
                                    allLeftTransfers.add(transfer);
                                }
                            }
                            Log.d(LOG_TAG, "Players who arrived fetched: " + allArrivedTransfers.size());
                            Log.d(LOG_TAG, "Players who left fetched: " + allLeftTransfers.size());

                            // Sort arrived transfers
                            Collections.sort(allArrivedTransfers, new Comparator<Transfer>() {
                                @Override
                                public int compare(Transfer o1, Transfer o2) {
                                    return o1.getTimeAdded().compareTo(o2.getTimeAdded());
                                }
                            });
                            
                            // Sort left transfers
                            Collections.sort(allLeftTransfers, new Comparator<Transfer>() {
                                @Override
                                public int compare(Transfer o1, Transfer o2) {
                                    return o1.getTimeAdded().compareTo(o2.getTimeAdded());
                                }
                            });
                            
                            Log.d(LOG_TAG, "Transfers sorted by time added.");

                            transferList.addAll(allArrivedTransfers);
                            fullTransferList.clear();
                            fullTransferList.addAll(allArrivedTransfers);
                            
                            transferType.setText("Arrived");
                            transferDealsRecAdapter = new TransferDealsRecAdapter(TransferDealsActivity.this, transferList, managerId, managerTeam, 0);
                            recyclerView.setAdapter(transferDealsRecAdapter);
                            transferDealsRecAdapter.notifyDataSetChanged();
                            transferCount.setText(transferList.size() + " transfer(s)");
                        } else {
                            transferCount.setText(transferList.size() + " transfer(s)");
                            Log.w(LOG_TAG, "No players found who arrived.");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error fetching players who arrived.", e));

    }

    private void findMaxTransferId(List<Transfer> transfers) {
        maxId = transfers.get(0).getId();
        for (Transfer transfer: transfers) {
            if (transfer.getId() > maxId) {
                maxId = transfer.getId();
            }
        }
        Log.d(LOG_TAG, "Max transfer ID determined: " + maxId);
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
        
        // Animate navigation sliding to the right
        navigationContainer.animate()
                .translationX(navigationContainer.getWidth())
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
        
        // Animate navigation sliding back if not in filter mode
        if (!isFilterMode && navigationContainer.getTranslationX() != 0f) {
            navigationContainer.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(300);
        }
        
        // Restore view
        if (!isFilterMode) {
            restoreCurrentTypeList();
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
        
        // Handle dialog dismissal - keep navigation hidden if filters active
        filterDialog.setOnDismissListener(dialog -> {
            if (isFilterMode) {
                navigationContainer.setVisibility(View.INVISIBLE);
                navigationContainer.setAlpha(0f);
                navigationContainer.setTranslationX(navigationContainer.getWidth());
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
            
            // Show navigation with animation
            navigationContainer.setVisibility(View.VISIBLE);
            navigationContainer.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(300);
            
            restoreCurrentTypeList();
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
                // Hide navigation when filters are active
                if (!wasFilterMode) {
                    navigationContainer.animate()
                            .translationX(navigationContainer.getWidth())
                            .alpha(0f)
                            .setDuration(300);
                }
                
                applyFiltersAndSort();
            } else {
                // Show navigation and restore view
                if (wasFilterMode) {
                    navigationContainer.setVisibility(View.VISIBLE);
                    navigationContainer.animate()
                            .translationX(0f)
                            .alpha(1f)
                            .setDuration(300);
                }
                restoreCurrentTypeList();
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
     * Apply filters and sorting to transfers
     * Filters work across ALL transfers (both arrived and left), similar to search
     */
    private void applyFiltersAndSort() {
        transferList.clear();
        
        // Start with ALL transfers (both arrived and left), using deduplication
        Set<Integer> seenIds = new HashSet<>();
        List<Transfer> allTransfers = new ArrayList<>();
        
        // Add arrived transfers
        for (Transfer transfer : allArrivedTransfers) {
            if (transfer.getId() > 0 && !seenIds.contains(transfer.getId())) {
                allTransfers.add(transfer);
                seenIds.add(transfer.getId());
            } else if (transfer.getId() == 0) {
                allTransfers.add(transfer);
            }
        }
        
        // Add left transfers (avoiding duplicates)
        for (Transfer transfer : allLeftTransfers) {
            if (transfer.getId() > 0 && !seenIds.contains(transfer.getId())) {
                allTransfers.add(transfer);
                seenIds.add(transfer.getId());
            } else if (transfer.getId() == 0) {
                boolean isDuplicate = false;
                for (Transfer existing : allTransfers) {
                    if (existing.getId() == 0 && 
                        Objects.equals(existing.getFullName(), transfer.getFullName()) &&
                        Objects.equals(existing.getTimeAdded(), transfer.getTimeAdded())) {
                        isDuplicate = true;
                        break;
                    }
                }
                if (!isDuplicate) {
                    allTransfers.add(transfer);
                }
            }
        }
        
        Log.d(LOG_TAG, "Filtering across all transfers. Total before filters: " + allTransfers.size());
        
        List<Transfer> filteredList = new ArrayList<>(allTransfers);
        
        // Apply position filter (multiple selections)
        if (!selectedPositions.isEmpty()) {
            List<Transfer> temp = new ArrayList<>();
            for (Transfer transfer : filteredList) {
                if (transfer.getPosition() != null && selectedPositions.contains(transfer.getPosition())) {
                    temp.add(transfer);
                }
            }
            filteredList = temp;
        }
        
        // Apply position category filter (multiple selections)
        if (!selectedPositionCategories.isEmpty()) {
            List<Transfer> temp = new ArrayList<>();
            for (Transfer transfer : filteredList) {
                if (transfer.getPosition() != null) {
                    String playerCategory = getPositionCategory(transfer.getPosition());
                    if (selectedPositionCategories.contains(playerCategory)) {
                        temp.add(transfer);
                    }
                }
            }
            filteredList = temp;
        }
        
        // Apply sorting
        switch (currentSortOption) {
            case "name":
                Collections.sort(filteredList, (t1, t2) -> {
                    String name1 = t1.getLastName() != null ? t1.getLastName() : "";
                    String name2 = t2.getLastName() != null ? t2.getLastName() : "";
                    return name1.compareToIgnoreCase(name2);
                });
                break;
            case "position":
                // Sort by position using the same order as position_array
                String[] positionOrder = getResources().getStringArray(R.array.position_array);
                Collections.sort(filteredList, (t1, t2) -> {
                    String pos1 = t1.getPosition() != null ? t1.getPosition() : "";
                    String pos2 = t2.getPosition() != null ? t2.getPosition() : "";
                    
                    int index1 = getPositionIndex(pos1, positionOrder);
                    int index2 = getPositionIndex(pos2, positionOrder);
                    
                    return Integer.compare(index1, index2);
                });
                break;
            case "overall_asc":
                Collections.sort(filteredList, (t1, t2) -> Integer.compare(t1.getOverall(), t2.getOverall()));
                break;
            case "overall_desc":
                Collections.sort(filteredList, (t1, t2) -> Integer.compare(t2.getOverall(), t1.getOverall()));
                break;
            default:
                // Keep default order (by time added)
                break;
        }
        
        transferList.addAll(filteredList);
        
        // Update adapter
        if (transferDealsRecAdapter != null) {
            transferDealsRecAdapter.notifyDataSetChanged();
        }
        
        // Update transfer count
        transferCount.setText(transferList.size() + " transfer(s)");
        
        Log.d(LOG_TAG, "Applied filters and sort across ALL transfers. " + transferList.size() + " transfers found.");
    }

    /**
     * Filter transfers with both search and filters applied
     * Searches across ALL transfers (both arrived and left)
     */
    private void filterTransfersWithSearchAndFilters(String query) {
        Log.d(LOG_TAG, "filterTransfersWithSearchAndFilters called with query: " + query);
        
        transferList.clear();
        
        if (query == null || query.trim().isEmpty()) {
            // If search is empty, restore filtered or current type view
            if (isFilterMode) {
                applyFiltersAndSort();
            } else {
                restoreCurrentTypeList();
            }
            return;
        }
        
        // Search across BOTH arrived AND left transfers (all transfers)
        // Use a Set to avoid duplicates in case a transfer appears in both lists
        Set<Integer> seenIds = new HashSet<>();
        List<Transfer> allTransfers = new ArrayList<>();
        
        // Add arrived transfers
        for (Transfer transfer : allArrivedTransfers) {
            if (transfer.getId() > 0 && !seenIds.contains(transfer.getId())) {
                allTransfers.add(transfer);
                seenIds.add(transfer.getId());
            } else if (transfer.getId() == 0) {
                // If no ID, add anyway (shouldn't happen but handle gracefully)
                allTransfers.add(transfer);
            }
        }
        
        // Add left transfers (avoiding duplicates)
        for (Transfer transfer : allLeftTransfers) {
            if (transfer.getId() > 0 && !seenIds.contains(transfer.getId())) {
                allTransfers.add(transfer);
                seenIds.add(transfer.getId());
            } else if (transfer.getId() == 0) {
                // If no ID, check if it's the same transfer by comparing other fields
                boolean isDuplicate = false;
                for (Transfer existing : allTransfers) {
                    if (existing.getId() == 0 && 
                        Objects.equals(existing.getFullName(), transfer.getFullName()) &&
                        Objects.equals(existing.getTimeAdded(), transfer.getTimeAdded())) {
                        isDuplicate = true;
                        break;
                    }
                }
                if (!isDuplicate) {
                    allTransfers.add(transfer);
                }
            }
        }
        
        Log.d(LOG_TAG, "Combined transfer list size (after deduplication): " + allTransfers.size());
        
        List<Transfer> baseList = new ArrayList<>(allTransfers);
        
        // Apply filters if in filter mode
        if (isFilterMode) {
            // Apply position filter (multiple selections)
            if (!selectedPositions.isEmpty()) {
                List<Transfer> temp = new ArrayList<>();
                for (Transfer transfer : baseList) {
                    if (transfer.getPosition() != null && selectedPositions.contains(transfer.getPosition())) {
                        temp.add(transfer);
                    }
                }
                baseList = temp;
            }
            
            // Apply position category filter (multiple selections)
            if (!selectedPositionCategories.isEmpty()) {
                List<Transfer> temp = new ArrayList<>();
                for (Transfer transfer : baseList) {
                    if (transfer.getPosition() != null) {
                        String playerCategory = getPositionCategory(transfer.getPosition());
                        if (selectedPositionCategories.contains(playerCategory)) {
                            temp.add(transfer);
                        }
                    }
                }
                baseList = temp;
            }
        }
        
        // Now apply search query to the base list
        String searchQuery = query.toLowerCase().trim();
        for (Transfer transfer : baseList) {
            boolean matches = false;
            
            // Search in first name
            if (transfer.getFirstName() != null && 
                transfer.getFirstName().toLowerCase().contains(searchQuery)) {
                matches = true;
            }
            
            // Search in last name
            if (!matches && transfer.getLastName() != null && 
                transfer.getLastName().toLowerCase().contains(searchQuery)) {
                matches = true;
            }
            
            // Search in full name
            if (!matches && transfer.getFullName() != null && 
                transfer.getFullName().toLowerCase().contains(searchQuery)) {
                matches = true;
            }
            
            // Search in current team
            if (!matches && transfer.getCurrentTeam() != null && 
                transfer.getCurrentTeam().toLowerCase().contains(searchQuery)) {
                matches = true;
            }
            
            // Search in former team
            if (!matches && transfer.getFormerTeam() != null && 
                transfer.getFormerTeam().toLowerCase().contains(searchQuery)) {
                matches = true;
            }
            
            if (matches) {
                transferList.add(transfer);
            }
        }
        
        // Apply sorting if in filter mode
        if (isFilterMode && !currentSortOption.equals("none")) {
            switch (currentSortOption) {
                case "name":
                    Collections.sort(transferList, (t1, t2) -> {
                        String name1 = t1.getLastName() != null ? t1.getLastName() : "";
                        String name2 = t2.getLastName() != null ? t2.getLastName() : "";
                        return name1.compareToIgnoreCase(name2);
                    });
                    break;
                case "position":
                    String[] positionOrder = getResources().getStringArray(R.array.position_array);
                    Collections.sort(transferList, (t1, t2) -> {
                        String pos1 = t1.getPosition() != null ? t1.getPosition() : "";
                        String pos2 = t2.getPosition() != null ? t2.getPosition() : "";
                        
                        int index1 = getPositionIndex(pos1, positionOrder);
                        int index2 = getPositionIndex(pos2, positionOrder);
                        
                        return Integer.compare(index1, index2);
                    });
                    break;
                case "overall_asc":
                    Collections.sort(transferList, (t1, t2) -> Integer.compare(t1.getOverall(), t2.getOverall()));
                    break;
                case "overall_desc":
                    Collections.sort(transferList, (t1, t2) -> Integer.compare(t2.getOverall(), t1.getOverall()));
                    break;
            }
        }
        
        // Update adapter
        if (transferDealsRecAdapter != null) {
            transferDealsRecAdapter.notifyDataSetChanged();
        }
        
        // Update transfer count
        transferCount.setText(transferList.size() + " transfer(s)");
        
        Log.d(LOG_TAG, "Search found " + transferList.size() + " transfers for query: " + query);
    }

    /**
     * Restore current type list (Arrived or Left)
     */
    private void restoreCurrentTypeList() {
        transferList.clear();
        transferList.addAll(fullTransferList);
        
        if (transferDealsRecAdapter != null) {
            transferDealsRecAdapter.notifyDataSetChanged();
        }
        
        transferCount.setText(transferList.size() + " transfer(s)");
        
        Log.d(LOG_TAG, "Restored current type list (" + currentTransferType + "). " + transferList.size() + " transfers.");
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
    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume called: Clearing transfer list.");
        transferList.clear();
    }
}