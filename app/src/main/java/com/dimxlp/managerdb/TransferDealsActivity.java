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

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            managerId = extras.getLong("managerId");
            team = extras.getString("team");
            Log.d("RAFI", "managerId = " + managerId + "\nteam = " + team);
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
        transferList.clear();

        transfersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                Transfer transfer = doc.toObject(Transfer.class);
                                if (transfer.isFormerPlayer()) {
                                    transferList.add(transfer);
                                }
                            }
                            Collections.sort(transferList, new Comparator<Transfer>() {
                                @Override
                                public int compare(Transfer o1, Transfer o2) {
                                    return o1.getTimeAdded().compareTo(o2.getTimeAdded());
                                }
                            });
                            transferDealsRecAdapter = new TransferDealsRecAdapter(TransferDealsActivity.this, transferList, managerId, team, buttonInt);
                            recyclerView.setAdapter(transferDealsRecAdapter);
                            transferDealsRecAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void listPlayersArrived(final int buttonInt) {
        transferList.clear();

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
                            Collections.sort(transferList, new Comparator<Transfer>() {
                                @Override
                                public int compare(Transfer o1, Transfer o2) {
                                    return o1.getTimeAdded().compareTo(o2.getTimeAdded());
                                }
                            });
                            transferDealsRecAdapter = new TransferDealsRecAdapter(TransferDealsActivity.this, transferList, managerId, team, buttonInt);
                            recyclerView.setAdapter(transferDealsRecAdapter);
                            transferDealsRecAdapter.notifyDataSetChanged();
                        }
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
            case R.id.dr_logout:
                if (user != null && firebaseAuth != null) {
                    firebaseAuth.signOut();
                    startActivity(new Intent(TransferDealsActivity.this, MainActivity.class));
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

        shPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (Objects.requireNonNull(task.getResult()).size() > 0) {
                                shPlayersExist = true;
                            } else {
                                shPlayersExist = false;
                            }
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
                            if (task.getResult().size() > 0) {
                                ftPlayersExist = true;
                            } else {
                                ftPlayersExist = false;
                            }
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
                            if (task.getResult().size() > 0) {
                                ytPlayersExist = true;
                            } else {
                                ytPlayersExist = false;
                            }
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
                            }
                            Manager theManager = managerList.get(0);
                            managerNameHeader.setText(theManager.getFullName());
                            teamHeader.setText(theManager.getTeam());
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
                            findMaxTransferId(transfers);
                            for (DocumentSnapshot ds: queryDocumentSnapshots) {
                                Transfer transfer = ds.toObject(Transfer.class);
                                if (transfer.getId() == 0) {
                                    transfer.setId(maxId+1);
                                    transfersColRef.document(ds.getId()).update("id", transfer.getId());
                                    maxId++;
                                }
                            }
                            refreshTransfers();
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
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                Transfer transfer = doc.toObject(Transfer.class);
                                if (!transfer.isFormerPlayer()) {
                                    transferList.add(transfer);
                                }
                            }
                            Collections.sort(transferList, new Comparator<Transfer>() {
                                @Override
                                public int compare(Transfer o1, Transfer o2) {
                                    return o1.getTimeAdded().compareTo(o2.getTimeAdded());
                                }
                            });
                            typeOfTransfer.setText("Arrived");
                            transferDealsRecAdapter = new TransferDealsRecAdapter(TransferDealsActivity.this, transferList, managerId, team, 0);
                            recyclerView.setAdapter(transferDealsRecAdapter);
                            transferDealsRecAdapter.notifyDataSetChanged();
                        }
                    }
                });

    }

    private void findMaxTransferId(List<Transfer> transfers) {
        maxId = transfers.get(0).getId();
        for (Transfer transfer: transfers) {
            if (transfer.getId() > maxId) {
                maxId = transfer.getId();
            }
        }
    }

    private void refreshTransfers() {
        transfersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    transferList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Transfer transfer = doc.toObject(Transfer.class);
                        transferList.add(transfer);
                    }
                    Collections.sort(transferList, Comparator.comparing(Transfer::getTimeAdded));
                    transferDealsRecAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("RAFI", "Failed to refresh transfer list", e));
    }


    @Override
    protected void onResume() {
        super.onResume();

        transferList.clear();
    }
}