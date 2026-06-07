package com.heben.client.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.heben.client.R;

public class BuyerGuideActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_guide);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }
}
