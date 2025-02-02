package com.dimxlp.managerdb;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
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

import model.Manager;
import util.UserApi;

public class SupportActivity extends AppCompatActivity {

    private static final String LOG_TAG = "RAFI|Support";
    private FirebaseAuth firebaseAuth;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersCollection = db.collection("Users");
    private CollectionReference managersCollection = db.collection("Managers");
    private CollectionReference firstTeamPlayersCollection = db.collection("FirstTeamPlayers");
    private CollectionReference youthTeamPlayersCollection = db.collection("YouthTeamPlayers");
    private CollectionReference shortlistedPlayersCollection = db.collection("ShortlistedPlayers");
    private CollectionReference formerPlayersCollection = db.collection("FormerPlayers");
    private CollectionReference loanedOutPlayersCollection = db.collection("LoanedOutPlayers");
    private CollectionReference transferDealsCollection = db.collection("Transfers");

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navView;
    private TextView managerNameHeader;
    private TextView teamHeader;
    private long managerId;
    private String team;
    private boolean ftPlayersExist;
    private boolean ytPlayersExist;
    private boolean shPlayersExist;
    private InterstitialAd interstitialAd;
    private NativeAd nativeAdBottom;
    private NativeAdView nativeAdViewBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support);
        Log.i(LOG_TAG, "SupportActivity launched.");

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

        drawerLayout = findViewById(R.id.drawer_layout_sup);
        navView = findViewById(R.id.nvView_sup);
        setUpDrawerContent(navView);

        View headerLayout = null;
        if (navView.getHeaderCount() > 0) {
            headerLayout = navView.getHeaderView(0);
        }
        managerNameHeader = headerLayout.findViewById(R.id.manager_name_header);
        teamHeader = headerLayout.findViewById(R.id.team_name_header);

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> Log.d(LOG_TAG, "Mobile Ads SDK initialized."));

        nativeAdViewBottom = findViewById(R.id.native_ad_view_bottom);
        loadNativeAd("ca-app-pub-8349697523222717/2314594181", nativeAdViewBottom);

        // Load Interstitial Ad
        InterstitialAd.load(this, "ca-app-pub-8349697523222717/2712951956", new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        interstitialAd.show(SupportActivity.this);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {

                    }
                });

        // Handle Send Feedback button click
        Button sendFeedbackButton = findViewById(R.id.send_feedback_button);
        sendFeedbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Send Feedback button clicked.");
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:managerdbapp@gmail.com")); // Replace with your email
                intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback for ManagerDB");
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                    Log.d(LOG_TAG, "Feedback email intent started.");
                } else {
                    Log.w(LOG_TAG, "No email apps installed.");
                    Toast.makeText(SupportActivity.this, "No email apps installed.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Handle App Guides button click
        Button appGuidesButton = findViewById(R.id.app_guides_button);
        appGuidesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "App Guides button clicked.");
                Toast.makeText(SupportActivity.this, "App guides are not supported yet.", Toast.LENGTH_SHORT).show();
            }
        });

        // Delete Account Button
        Button deleteAccountButton = findViewById(R.id.delete_account_button);
        deleteAccountButton.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteUserAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUserAccount() {
        if (user == null) {
            Log.w(LOG_TAG, "No authenticated user found.");
            Toast.makeText(this, "No authenticated user.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Delete all user data
        deleteUserData(user.getUid(), new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // Delete Firebase Authentication account
                    user.delete()
                            .addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    Log.d(LOG_TAG, "User account deleted.");
                                    Toast.makeText(SupportActivity.this, "Account deleted successfully.", Toast.LENGTH_LONG).show();

                                    // Redirect to login
                                    Intent intent = new Intent(SupportActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finishAffinity();
                                } else {
                                    Log.e(LOG_TAG, "Error deleting Firebase Auth account.", task1.getException());
                                    Toast.makeText(SupportActivity.this, "Error deleting account.", Toast.LENGTH_LONG).show();
                                }
                            });
                } else {
                    Log.e(LOG_TAG, "Error deleting user data.", task.getException());
                    Toast.makeText(SupportActivity.this, "Error deleting account data.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void deleteUserData(String userId, OnCompleteListener<Void> onCompleteListener) {
        Log.d(LOG_TAG, "Deleting user data for userId: " + userId);

        Task<Void> deleteUser = deleteCollection(usersCollection.whereEqualTo("userId", userId));
        Task<Void> deleteManagers = deleteCollection(managersCollection.whereEqualTo("userId", userId));
        Task<Void> deleteFirstTeam = deleteCollection(firstTeamPlayersCollection.whereEqualTo("userId", userId));
        Task<Void> deleteYouthTeam = deleteCollection(youthTeamPlayersCollection.whereEqualTo("userId", userId));
        Task<Void> deleteShortlisted = deleteCollection(shortlistedPlayersCollection.whereEqualTo("userId", userId));
        Task<Void> deleteFormerPlayer = deleteCollection(formerPlayersCollection.whereEqualTo("userId", userId));
        Task<Void> deleteLoanedOutPlayer = deleteCollection(loanedOutPlayersCollection.whereEqualTo("userId", userId));
        Task<Void> deleteTransferDeals = deleteCollection(transferDealsCollection.whereEqualTo("userId", userId));

        Tasks.whenAll(deleteUser, deleteManagers, deleteFirstTeam, deleteYouthTeam, deleteShortlisted, deleteFormerPlayer, deleteLoanedOutPlayer, deleteTransferDeals)
                .addOnCompleteListener(onCompleteListener);
    }

    private Task<Void> deleteCollection(Query query) {
        return query.get().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                return Tasks.forException(task.getException());
            }
            List<Task<Void>> deleteTasks = new ArrayList<>();
            for (QueryDocumentSnapshot document : task.getResult()) {
                deleteTasks.add(document.getReference().delete());
            }
            return Tasks.whenAll(deleteTasks);
        });
    }

    private void loadNativeAd(String adUnitId, NativeAdView nativeAdView) {
        AdLoader adLoader = new AdLoader.Builder(this, adUnitId)
                .forNativeAd(ad -> {
                    if (isDestroyed()) {
                        ad.destroy();
                        return;
                    }
                    if (nativeAdView == nativeAdViewBottom) {
                        nativeAdBottom = ad;
                    } else {
                        nativeAdBottom = ad;
                    }
                    populateNativeAdView(ad, nativeAdView);
                })
                .withAdListener(new com.google.android.gms.ads.AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {

                    }
                })
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private void populateNativeAdView(NativeAd nativeAd, NativeAdView nativeAdView) {
        nativeAdView.setHeadlineView(nativeAdView.findViewById(R.id.ad_headline_bottom));
        nativeAdView.setBodyView(nativeAdView.findViewById(R.id.ad_body_bottom));
        nativeAdView.setCallToActionView(nativeAdView.findViewById(R.id.ad_call_to_action_bottom));

        ((TextView) nativeAdView.getHeadlineView()).setText(nativeAd.getHeadline());
        if (nativeAd.getBody() != null) {
            ((TextView) nativeAdView.getBodyView()).setText(nativeAd.getBody());
        }
        if (nativeAd.getCallToAction() != null) {
            ((Button) nativeAdView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }

        nativeAdView.setNativeAd(nativeAd);
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "onDestroy called: Cleaning up resources.");
        if (nativeAdBottom != null) nativeAdBottom.destroy();
        super.onDestroy();
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
                Intent homeIntent = new Intent(SupportActivity.this, ManageTeamActivity.class);
                homeIntent.putExtra("managerId", managerId);
                homeIntent.putExtra("team", team);
                startActivity(homeIntent);
                finish();
                break;
            case R.id.dr_manager_selection:
                Intent managerSelectionIntent = new Intent(SupportActivity.this, SelectManagerActivity.class);
                startActivity(managerSelectionIntent);
                break;
            case R.id.dr_profile:
                Intent profileIntent = new Intent(SupportActivity.this, ProfileActivity.class);
                profileIntent.putExtra("managerId", managerId);
                profileIntent.putExtra("team", team);
                startActivity(profileIntent);
                finish();
                break;
            case R.id.dr_first_team:
                if (ftPlayersExist) {
                    Intent firstIntent = new Intent(SupportActivity.this, FirstTeamListActivity.class);
                    firstIntent.putExtra("managerId", managerId);
                    firstIntent.putExtra("team", team);
                    startActivity(firstIntent);
                    finish();
                } else {
                    Intent firstIntent = new Intent(SupportActivity.this, FirstTeamActivity.class);
                    firstIntent.putExtra("managerId", managerId);
                    firstIntent.putExtra("team", team);
                    startActivity(firstIntent);
                    finish();
                }
                break;
            case R.id.dr_youth_team:
                if (ytPlayersExist) {
                    Intent youthIntent = new Intent(SupportActivity.this, YouthTeamListActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", team);
                    startActivity(youthIntent);
                    finish();
                } else {
                    Intent youthIntent = new Intent(SupportActivity.this, YouthTeamActivity.class);
                    youthIntent.putExtra("managerId", managerId);
                    youthIntent.putExtra("team", team);
                    startActivity(youthIntent);
                    finish();
                }
                break;
            case R.id.dr_former_players:
                Intent formerIntent = new Intent(SupportActivity.this, FormerPlayersListActivity.class);
                formerIntent.putExtra("managerId", managerId);
                formerIntent.putExtra("team", team);
                startActivity(formerIntent);
                finish();
                break;
            case R.id.dr_shortlist:
                if (shPlayersExist) {
                    Intent shortlistIntent = new Intent(SupportActivity.this, ShortlistPlayersActivity.class);
                    shortlistIntent.putExtra("managerId", managerId);
                    shortlistIntent.putExtra("team", team);
                    startActivity(shortlistIntent);
                    finish();
                } else {
                    Intent shortlistIntent = new Intent(SupportActivity.this, ShortlistActivity.class);
                    shortlistIntent.putExtra("managerId", managerId);
                    shortlistIntent.putExtra("team", team);
                    startActivity(shortlistIntent);
                    finish();
                }
                break;
            case R.id.dr_loaned_out_players:
                Intent loanIntent = new Intent(SupportActivity.this, LoanedOutPlayersActivity.class);
                loanIntent.putExtra("managerId", managerId);
                loanIntent.putExtra("team", team);
                startActivity(loanIntent);
                finish();
                break;
            case R.id.dr_transfer_deals:
                Intent transferIntent = new Intent(SupportActivity.this, TransferDealsActivity.class);
                transferIntent.putExtra("managerId", managerId);
                transferIntent.putExtra("team", team);
                startActivity(transferIntent);
                finish();
                break;
            case R.id.dr_support_info:
                break;
            case R.id.dr_logout:
                if (user != null && firebaseAuth != null) {
                    firebaseAuth.signOut();
                    startActivity(new Intent(SupportActivity.this, MainActivity.class));
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
        Log.d(LOG_TAG, "onStart called: Fetching data for teams and manager.");

        shortlistedPlayersCollection.whereEqualTo("userId", UserApi.getInstance().getUserId())
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

        firstTeamPlayersCollection.whereEqualTo("userId", UserApi.getInstance().getUserId())
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

        youthTeamPlayersCollection.whereEqualTo("userId", UserApi.getInstance().getUserId())
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

        managersCollection.whereEqualTo("userId", UserApi.getInstance().getUserId())
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
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error fetching manager data.", e));
    }
}
