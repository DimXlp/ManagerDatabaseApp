package com.dimxlp.managerdb;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import model.Manager;
import model.ShortlistedPlayer;
import util.UserApi;
import util.ValueFormatter;

public class ShortlistActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navView;

    private AlertDialog.Builder builder;
    private AlertDialog dialog;

    private FloatingActionButton addPlayerFab;
    private TextView addPlayerText;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference shortlistColRef = db.collection("ShortlistedPlayers");
    private CollectionReference ftPlayersColRef = db.collection("FirstTeamPlayers");
    private CollectionReference ytPlayersColRef = db.collection("YouthTeamPlayers");
    private CollectionReference managersColRef = db.collection("Managers");
    private boolean ftPlayersExist;
    private boolean ytPlayersExist;
    private long managerId;
    private String myTeam;

    private EditText firstName;
    private EditText lastName;
    private Spinner positionSpinner;
    private EditText team;
    private EditText nationality;
    private EditText overall;
    private EditText potLow;
    private EditText potHigh;
    private TextInputLayout valueTil;
    private EditText value;
    private TextInputLayout wageTil;
    private EditText wage;
    private EditText comments;
    private Button createButton;
    private String currency;

    private TextView managerNameHeader;
    private TextView teamHeader;
    private String barPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shortlist);

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            managerId = extras.getLong("managerId");
            myTeam = extras.getString("team");
            Log.d("RAFI", "managerId = " + managerId + "\nteam = " + myTeam);
        }

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_menu);

        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nvView_sh);
        setUpDrawerContent(navView);

        View headerLayout = null;
        if (navView.getHeaderCount() > 0) {
            headerLayout = navView.getHeaderView(0);
        }
        managerNameHeader = headerLayout.findViewById(R.id.manager_name_header);
        teamHeader = headerLayout.findViewById(R.id.team_name_header);

        addPlayerFab = findViewById(R.id.add_player_button_sh);
        addPlayerText = findViewById(R.id.add_player_text_sh);

        addPlayerFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createPopupDialog();
            }
        });
    }

    private void createPopupDialog() {
        builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.create_shortlisted_player_popup, null);

        firstName = view.findViewById(R.id.first_name_shp_create);
        lastName = view.findViewById(R.id.last_name_shp_create);
        positionSpinner = view.findViewById(R.id.position_spinner_shp_create);
        team = view.findViewById(R.id.team_shp_create);
        nationality = view.findViewById(R.id.nationality_shp_create);
        overall = view.findViewById(R.id.overall_shp_create);
        potLow = view.findViewById(R.id.potential_low_shp_create);
        potHigh = view.findViewById(R.id.potential_high_shp_create);
        valueTil = view.findViewById(R.id.value_til_shp_create);
        value = view.findViewById(R.id.value_shp_create);
        wageTil = view.findViewById(R.id.wage_til_shp_create);
        wage = view.findViewById(R.id.wage_shp_create);
        comments = view.findViewById(R.id.comments_shp_create);
        createButton = view.findViewById(R.id.create_sh_player_button);

        ValueFormatter.formatValue(value);
        ValueFormatter.formatValue(wage);

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
                            currency = managerList.get(0).getCurrency();
                            valueTil.setHint("Value (in " + currency + ")");
                            wageTil.setHint("Wage (in " + currency + ")");
                        }
                    }
                });

        ArrayAdapter<CharSequence> positionAdapter = ArrayAdapter.createFromResource(this, R.array.position_array, android.R.layout.simple_spinner_item);
        positionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        positionSpinner.setAdapter(positionAdapter);

        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!lastName.getText().toString().isEmpty() &&
                        !nationality.getText().toString().isEmpty() &&
                        !positionSpinner.getSelectedItem().toString().isEmpty() &&
                        !team.getText().toString().isEmpty() &&
                        !overall.getText().toString().isEmpty()) {
                    createPlayer();
                } else {
                    Toast.makeText(ShortlistActivity.this, "Last Name/Nickname, Nationality, Position, Team and Overall are required", Toast.LENGTH_LONG)
                            .show();
                }
            }
        });

        builder.setView(view);
        dialog = builder.create();
        dialog.show();
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
        final String positionPlayer = positionSpinner.getSelectedItem().toString().trim();
        String nationalityPlayer = nationality.getText().toString().trim();
        String overallPlayer = overall.getText().toString().trim();
        String potLowPlayer = potLow.getText().toString().trim();
        String potHighPlayer = potHigh.getText().toString().trim();
        String teamPlayer = team.getText().toString().trim();
        String valuePlayer = value.getText().toString().trim().replaceAll(",", "");
        String wagePlayer = wage.getText().toString().trim().replaceAll(",", "");
        String commentsPlayer = comments.getText().toString().trim();

        final ShortlistedPlayer player = new ShortlistedPlayer();

        player.setId(1);
        player.setFirstName(firstNamePlayer);
        player.setLastName(lastNamePlayer);
        player.setFullName(fullNamePlayer);
        player.setPosition(positionPlayer);
        player.setNationality(nationalityPlayer);
        player.setOverall(Integer.parseInt(overallPlayer));
        if (!potLowPlayer.isEmpty()) {
            player.setPotentialLow(Integer.parseInt(potLowPlayer));
        }
        if (!potHighPlayer.isEmpty()) {
            player.setPotentialHigh(Integer.parseInt(potHighPlayer));
        }
        player.setTeam(teamPlayer);
        if (!valuePlayer.isEmpty()) {
            player.setValue(Integer.parseInt(valuePlayer));
        }
        if (!wagePlayer.isEmpty()) {
            player.setWage(Integer.parseInt(wagePlayer));
        }
        player.setComments(commentsPlayer);
        player.setManagerId(managerId);
        player.setUserId(UserApi.getInstance().getUserId());
        player.setTimeAdded(new Timestamp(new Date()));

        shortlistColRef.add(player)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Intent intent = new Intent(ShortlistActivity.this, ShortlistPlayersActivity.class);
                        intent.putExtra("managerId", managerId);
                        intent.putExtra("team", myTeam);
                        switch (positionPlayer) {
                            case "GK":
                                barPosition = "Goalkeepers";
                                break;
                            case "CB":
                                barPosition = "Center Backs";
                                break;
                            case "RB":
                            case "RWB":
                                barPosition = "Right Backs";
                                break;
                            case "LB":
                            case "LWB":
                                barPosition = "Left Backs";
                                break;
                            case "CDM":
                                barPosition = "Center Defensive Mids";
                                break;
                            case "CM":
                                barPosition = "Center Midfielders";
                                break;
                            case "CAM":
                                barPosition = "Center Attacking Mids";
                                break;
                            case "RM":
                            case "RW":
                                barPosition = "Right Wingers";
                                break;
                            case "LM":
                            case "LW":
                                barPosition = "Left Wingers";
                                break;
                            case "ST":
                            case "CF":
                            case "RF":
                            case "LF":
                                barPosition = "Strikers";
                                break;

                        }
                        intent.putExtra("barPosition", barPosition);
                        startActivity(intent);
                        finish();
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
                Intent homeIntent = new Intent(ShortlistActivity.this, ManageTeamActivity.class);
                homeIntent.putExtra("managerId", managerId);
                homeIntent.putExtra("team", myTeam);
                startActivity(homeIntent);
                finish();
                break;
            case R.id.dr_profile:
                Intent profileIntent = new Intent(ShortlistActivity.this, ProfileActivity.class);
                profileIntent.putExtra("managerId", managerId);
                profileIntent.putExtra("team", myTeam);
                startActivity(profileIntent);
                finish();
                break;
            case R.id.dr_first_team:
                if (ftPlayersExist) {
                    Intent firstIntent = new Intent(ShortlistActivity.this, FirstTeamListActivity.class);
                    firstIntent.putExtra("managerId", managerId);
                    firstIntent.putExtra("team", myTeam);
                    startActivity(firstIntent);
                    finish();
                } else {
                    Intent firstIntent = new Intent(ShortlistActivity.this, FirstTeamActivity.class);
                    firstIntent.putExtra("managerId", managerId);
                    firstIntent.putExtra("team", myTeam);
                    startActivity(firstIntent);
                    finish();
                }
                break;
            case R.id.dr_youth_team:
                if (ytPlayersExist) {
                    Intent youthIntent = new Intent(ShortlistActivity.this, YouthTeamListActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", myTeam);
                    startActivity(youthIntent);
                    finish();
                } else {
                    Intent youthIntent = new Intent(ShortlistActivity.this, YouthTeamActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", myTeam);
                    startActivity(youthIntent);
                    finish();
                }
                break;
            case R.id.dr_former_players:
                Intent formerIntent = new Intent(ShortlistActivity.this, FormerPlayersListActivity.class);
                formerIntent.putExtra("managerId", managerId);
                formerIntent.putExtra("team", myTeam);
                startActivity(formerIntent);
                finish();
                break;
            case R.id.dr_shortlist:
                break;
            case R.id.dr_loaned_out_players:
                Intent loanIntent = new Intent(ShortlistActivity.this, LoanedOutPlayersActivity.class);
                loanIntent.putExtra("managerId", managerId);
                loanIntent.putExtra("team", myTeam);
                startActivity(loanIntent);
                finish();
                break;
            case R.id.dr_transfer_deals:
                Intent transferIntent = new Intent(ShortlistActivity.this, TransferDealsActivity.class);
                transferIntent.putExtra("managerId", managerId);
                transferIntent.putExtra("team", myTeam);
                startActivity(transferIntent);
                finish();
                break;
            case R.id.dr_logout:
                if (user != null && firebaseAuth != null) {
                    firebaseAuth.signOut();
                    startActivity(new Intent(ShortlistActivity.this, MainActivity.class));
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

        ftPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (Objects.requireNonNull(task.getResult()).size() > 0) {
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
                            if (Objects.requireNonNull(task.getResult()).size() > 0) {
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
    }
}