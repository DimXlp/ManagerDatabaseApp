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

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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
    private TextView typeOfTransfer;
    private boolean ftPlayersExist;
    private boolean ytPlayersExist;
    private List<Transfer> transferList;
    private int maxId;
    private boolean shPlayersExist;
    private long managerId;
    private String team;

    private TextView managerNameHeader;
    private TextView teamHeader;

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
        typeOfTransfer = findViewById(R.id.type_of_transfer_trf);

        View headerLayout = null;
        if (navView.getHeaderCount() > 0) {
            headerLayout = navView.getHeaderView(0);
        }
        managerNameHeader = headerLayout.findViewById(R.id.manager_name_header);
        teamHeader = headerLayout.findViewById(R.id.team_name_header);

        transferList = new ArrayList<>();

        recyclerView = findViewById(R.id.rec_view_trf);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> Log.d(LOG_TAG, "Mobile Ads SDK initialized."));

        // Load Banner Ads
        AdView transfersBanner = findViewById(R.id.transfers_banner);
        AdRequest adBannerRequest = new AdRequest.Builder().build();
        transfersBanner.loadAd(adBannerRequest);

        View.OnClickListener prevButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateTransferButtons(v);
                String type = typeOfTransfer.getText().toString().trim();
                if (type.equals("Arrived")) {
                    typeOfTransfer.setText("Left");
                    listPlayersLeft(1);
                } else {
                    typeOfTransfer.setText("Arrived");
                    listPlayersArrived(1);
                }
            }
        };

        View.OnClickListener nextButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateTransferButtons(v);
                String type = typeOfTransfer.getText().toString().trim();
                if (type.equals("Arrived")) {
                    typeOfTransfer.setText("Left");
                    listPlayersLeft(2);
                } else {
                    typeOfTransfer.setText("Arrived");
                    listPlayersArrived(2);
                }
            }
        };

        prevButton.setOnClickListener(prevButtonListener);
        nextButton.setOnClickListener(nextButtonListener);
    }

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

        transferList.clear();
        Log.d(LOG_TAG, "Transfer list cleared.");

        transfersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            Log.d(LOG_TAG, "Transfers fetched from Firestore for players who left.");
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                Transfer transfer = doc.toObject(Transfer.class);
                                if (transfer.isFormerPlayer()) {
                                    transferList.add(transfer);
                                    Log.d(LOG_TAG, "Player added to list: " + transfer.getFullName());
                                }
                            }
                            Collections.sort(transferList, new Comparator<Transfer>() {
                                @Override
                                public int compare(Transfer o1, Transfer o2) {
                                    return o1.getTimeAdded().compareTo(o2.getTimeAdded());
                                }
                            });
                            Log.d(LOG_TAG, "Transfer list sorted by time added.");

                            transferDealsRecAdapter = new TransferDealsRecAdapter(TransferDealsActivity.this, transferList, managerId, team, buttonInt);
                            recyclerView.setAdapter(transferDealsRecAdapter);
                            transferDealsRecAdapter.notifyDataSetChanged();
                        } else {
                            Log.w(LOG_TAG, "No players found who left.");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error fetching players who left from Firestore.", e));
    }

    private void listPlayersArrived(final int buttonInt) {
        Log.d(LOG_TAG, "Listing players who arrived. ButtonInt: " + buttonInt);

        transferList.clear();
        Log.d(LOG_TAG, "Transfer list cleared.");

        transfersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            Log.d(LOG_TAG, "Transfers fetched from Firestore for players who arrived.");
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                Transfer transfer = doc.toObject(Transfer.class);
                                if (!transfer.isFormerPlayer()) {
                                    transferList.add(transfer);
                                    Log.d(LOG_TAG, "Player added to list: " + transfer.getFullName());
                                }
                            }
                            Collections.sort(transferList, new Comparator<Transfer>() {
                                @Override
                                public int compare(Transfer o1, Transfer o2) {
                                    return o1.getTimeAdded().compareTo(o2.getTimeAdded());
                                }
                            });
                            Log.d(LOG_TAG, "Transfer list sorted by time added.");

                            transferDealsRecAdapter = new TransferDealsRecAdapter(TransferDealsActivity.this, transferList, managerId, team, buttonInt);
                            recyclerView.setAdapter(transferDealsRecAdapter);
                            transferDealsRecAdapter.notifyDataSetChanged();
                        } else {
                            Log.w(LOG_TAG, "No players found who arrived.");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error fetching players who arrived from Firestore.", e));
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
                Intent homeIntent = new Intent(TransferDealsActivity.this, ManageTeamActivity.class);
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
                        } else {
                            Log.w(LOG_TAG, "No manager data found for managerId=" + managerId);
                        }
                    }
                });

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
                            refreshTransfers();
                        } else {
                            Log.w(LOG_TAG, "No transfers found for ID updates.");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error fetching transfers for ID updates.", e));

        transfersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                Transfer transfer = doc.toObject(Transfer.class);
                                if (!transfer.isFormerPlayer()) {
                                    transferList.add(transfer);
                                }
                            }
                            Log.d(LOG_TAG, "Players who arrived fetched: " + transferList.size());

                            Collections.sort(transferList, new Comparator<Transfer>() {
                                @Override
                                public int compare(Transfer o1, Transfer o2) {
                                    return o1.getTimeAdded().compareTo(o2.getTimeAdded());
                                }
                            });
                            Log.d(LOG_TAG, "Transfers sorted by time added.");

                            typeOfTransfer.setText("Arrived");
                            transferDealsRecAdapter = new TransferDealsRecAdapter(TransferDealsActivity.this, transferList, managerId, team, 0);
                            recyclerView.setAdapter(transferDealsRecAdapter);
                            transferDealsRecAdapter.notifyDataSetChanged();
                        } else {
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

    private void refreshTransfers() {
        Log.d(LOG_TAG, "Refreshing transfers list.");
        transfersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    transferList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Transfer transfer = doc.toObject(Transfer.class);
                        transferList.add(transfer);
                    }
                    Log.d(LOG_TAG, "Transfer list refreshed. Count: " + transferList.size());

                    Collections.sort(transferList, Comparator.comparing(Transfer::getTimeAdded));
                    transferDealsRecAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Failed to refresh transfer list.", e));
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume called: Clearing transfer list.");
        transferList.clear();
    }
}