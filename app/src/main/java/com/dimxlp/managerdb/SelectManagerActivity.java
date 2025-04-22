package com.dimxlp.managerdb;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import model.Manager;
import ui.ManagerSelectionRecAdapter;
import util.UserApi;

public class SelectManagerActivity extends AppCompatActivity {

    private static final String LOG_TAG = "RAFI|SelectManager";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference managersColRef = db.collection("Managers");

    private RecyclerView recyclerView;
    private List<Manager> managerList;
    private ManagerSelectionRecAdapter managerSelectionRecAdapter;
    private FloatingActionButton addManagerFab;
//    private AdView selectManagerBanner;
//    private AdRequest adBannerRequest;
    private NativeAd nativeAdTop;
    private NativeAdView nativeAdViewTop;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_manager);
        Log.i(LOG_TAG, "SelectManagerActivity launched.");

//        FirebaseOptions options = new FirebaseOptions.Builder()
//                .setApplicationId(FirebaseApp.getInstance().getOptions().getApplicationId())
//                .setApiKey(BuildConfig.DEFAULT_RESTRICTED_API_KEY)  // Reset to restricted API key
//                .setProjectId(FirebaseApp.getInstance().getOptions().getProjectId())
//                .build();
//
//        if (FirebaseApp.getApps(this).isEmpty()) {
//            FirebaseApp.initializeApp(this, options);
//        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Log.d(LOG_TAG, "Firestore initialized with default API Key: " + FirebaseApp.getInstance().getOptions().getApiKey());

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

        nativeAdViewTop = findViewById(R.id.native_ad_view_top);
        loadNativeAd("ca-app-pub-3940256099942544/2247696110", nativeAdViewTop);
    }

    private void loadNativeAd(String adUnitId, NativeAdView nativeAdView) {
        AdLoader adLoader = new AdLoader.Builder(this, adUnitId)
                .forNativeAd(ad -> {
                    if (isDestroyed()) {
                        ad.destroy();
                        return;
                    }
                    nativeAdTop = ad;
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
        int headlineId =  R.id.ad_headline_top;
        nativeAdView.setHeadlineView(nativeAdView.findViewById(headlineId));
        TextView headlineView = (TextView) nativeAdView.getHeadlineView();

        if (nativeAd.getHeadline() != null) {
            headlineView.setText(nativeAd.getHeadline());
            headlineView.setVisibility(View.VISIBLE);
        } else {
            headlineView.setVisibility(View.GONE);
        }

        // Remove body and CTA for compact layout
        nativeAdView.setBodyView(null);
        nativeAdView.setCallToActionView(null);

        nativeAdView.setNativeAd(nativeAd);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "onStart called: Fetching manager data.");
        Log.d(LOG_TAG, "userId: " + UserApi.getInstance().getUserId());

        managerList.clear();
        managersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Log.d(LOG_TAG, "Manager data fetched successfully.");
                        for (QueryDocumentSnapshot doc: queryDocumentSnapshots) {
                            Manager manager = doc.toObject(Manager.class);
                            managerList.add(manager);
                            Log.d(LOG_TAG, "Manager: " + manager.getFullName());
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

        if (nativeAdTop != null) nativeAdTop.destroy();
        super.onDestroy();
        Log.d(LOG_TAG, "SelectManagerActivity destroyed.");
    }
}