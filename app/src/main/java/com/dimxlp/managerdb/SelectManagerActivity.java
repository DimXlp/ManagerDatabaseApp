package com.dimxlp.managerdb;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import model.Manager;
import ui.FirstTeamPlayerRecAdapter;
import ui.ManagerSelectionRecAdapter;
import util.ManagerSelectionButton;
import util.UserApi;

public class SelectManagerActivity extends AppCompatActivity {

    private static final String LOG_TAG = "RAFI|SelectManager";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference managersColRef = db.collection("Managers");

    private RecyclerView recyclerView;
    private List<Manager> managerList;
    private ManagerSelectionRecAdapter managerSelectionRecAdapter;
    private FloatingActionButton addManagerFab;
    private AdView selectManagerBanner;
    private AdRequest adBannerRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_manager);
        Log.i(LOG_TAG, "SelectManagerActivity launched.");

        managerList = new ArrayList<>();

        recyclerView = findViewById(R.id.rec_view_select);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        addManagerFab = findViewById(R.id.add_manager_fab);
        addManagerFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Add Manager FAB clicked.");
                startActivity(new Intent(SelectManagerActivity.this, CreateManagerActivity.class));
            }
        });

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> Log.d(LOG_TAG, "Mobile Ads SDK initialized."));

        // Load Banner Ad
        selectManagerBanner = findViewById(R.id.select_manager_banner);
        adBannerRequest = new AdRequest.Builder().build();
        selectManagerBanner.loadAd(adBannerRequest);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "onStart called: Fetching manager data.");

        managerList.clear();
        managersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Log.d(LOG_TAG, "Manager data fetched successfully.");
                        for (QueryDocumentSnapshot doc: queryDocumentSnapshots) {
                            Manager manager = doc.toObject(Manager.class);
                            managerList.add(manager);
                        }
                        managerSelectionRecAdapter = new ManagerSelectionRecAdapter(SelectManagerActivity.this, managerList);
                        recyclerView.setAdapter(managerSelectionRecAdapter);
                        managerSelectionRecAdapter.notifyDataSetChanged();
                        Log.d(LOG_TAG, "RecyclerView updated with manager data.");
                    } else {
                        Log.w(LOG_TAG, "No manager data found for userId=" + UserApi.getInstance().getUserId());
                    }
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error fetching manager data.", e));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume called.");
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "onDestroy called: Cleaning up resources.");

        // Destroy AdView to prevent memory leaks
        if (selectManagerBanner != null) {
            selectManagerBanner.destroy();
            Log.d(LOG_TAG, "Banner ad destroyed.");
        }

        super.onDestroy();
        Log.d(LOG_TAG, "SelectManagerActivity destroyed.");
    }
}