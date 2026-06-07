package com.heben.client.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.heben.client.R;

public class SellerGuideActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_guide);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }
}
