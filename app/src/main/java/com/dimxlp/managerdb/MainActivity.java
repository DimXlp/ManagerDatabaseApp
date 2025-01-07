package com.dimxlp.managerdb;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class MainActivity extends AppCompatActivity {

    private Button getStartedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> {});

        // Load Banner Ad
        AdView mainBanner1 = findViewById(R.id.main_banner_1);
        AdRequest adBannerRequest1 = new AdRequest.Builder().build();
        mainBanner1.loadAd(adBannerRequest1);

        AdView mainBanner2 = findViewById(R.id.main_banner_2);
        AdRequest adBannerRequest2 = new AdRequest.Builder().build();
        mainBanner2.loadAd(adBannerRequest2);

        getStartedButton = findViewById(R.id.get_started_button);
        getStartedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();

            }
        });
    }
}