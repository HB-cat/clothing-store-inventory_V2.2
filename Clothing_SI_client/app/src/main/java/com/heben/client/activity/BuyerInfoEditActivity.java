package com.heben.client.activity;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.heben.client.R;
import com.heben.client.model.ApiResponse;
import com.heben.client.network.ApiClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BuyerInfoEditActivity extends AppCompatActivity {

    private EditText etAge, etHeight, etWeight, etBust, etWaist, etHip, etStyle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_info_edit);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        etAge = findViewById(R.id.et_age);
        etHeight = findViewById(R.id.et_height);
        etWeight = findViewById(R.id.et_weight);
        etBust = findViewById(R.id.et_bust);
        etWaist = findViewById(R.id.et_waist);
        etHip = findViewById(R.id.et_hip);
        etStyle = findViewById(R.id.et_style);

        // 加载已有数据
        ApiClient.getService().getBuyerInfo().enqueue(new Callback<>() {
            @SuppressWarnings("unchecked")
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                                   Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Map<String, Object> info = response.body().getData();
                    if (info != null && !info.isEmpty()) {
                        if (info.get("age") != null) etAge.setText(String.valueOf(info.get("age")));
                        if (info.get("height") != null) etHeight.setText(String.valueOf(info.get("height")));
                        if (info.get("weight") != null) etWeight.setText(String.valueOf(info.get("weight")));
                        if (info.get("bust") != null) etBust.setText(String.valueOf(info.get("bust")));
                        if (info.get("waist") != null) etWaist.setText(String.valueOf(info.get("waist")));
                        if (info.get("hip") != null) etHip.setText(String.valueOf(info.get("hip")));
                        if (info.get("stylePreference") != null) etStyle.setText((String) info.get("stylePreference"));
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
            }
        });

        findViewById(R.id.btn_save).setOnClickListener(v -> saveInfo());
    }

    private void saveInfo() {
        HashMap<String, Object> body = new HashMap<>();
        if (!etAge.getText().toString().trim().isEmpty())
            body.put("age", Integer.parseInt(etAge.getText().toString().trim()));
        if (!etHeight.getText().toString().trim().isEmpty())
            body.put("height", Integer.parseInt(etHeight.getText().toString().trim()));
        if (!etWeight.getText().toString().trim().isEmpty())
            body.put("weight", Integer.parseInt(etWeight.getText().toString().trim()));
        if (!etBust.getText().toString().trim().isEmpty())
            body.put("bust", etBust.getText().toString().trim());
        if (!etWaist.getText().toString().trim().isEmpty())
            body.put("waist", etWaist.getText().toString().trim());
        if (!etHip.getText().toString().trim().isEmpty())
            body.put("hip", etHip.getText().toString().trim());
        body.put("stylePreference", etStyle.getText().toString().trim());

        ApiClient.getService().saveBuyerInfo(body).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(BuyerInfoEditActivity.this, "信息已保存", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(BuyerInfoEditActivity.this, "保存失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(BuyerInfoEditActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
