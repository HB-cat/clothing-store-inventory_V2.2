package com.heben.client.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.heben.client.R;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.DraftBill;
import com.heben.client.model.Models.DraftBillItem;
import com.heben.client.network.ApiClient;

import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewOrderEditActivity extends AppCompatActivity {

    private long draftId;
    private TextInputEditText etBuyerName;
    private LinearLayout layoutItems;
    private DraftBill currentDraft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_order_edit);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        draftId = getIntent().getLongExtra("draftId", 0);
        etBuyerName = findViewById(R.id.et_buyer_name);
        layoutItems = findViewById(R.id.layout_draft_items);

        findViewById(R.id.btn_add_product_to_draft).setOnClickListener(v -> {
            Intent intent = new Intent(this, ProductsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btn_draft_submit).setOnClickListener(v -> {
            saveDraft(true);
        });

        loadDraft();
    }

    private void loadDraft() {
        if (draftId <= 0) return;
        ApiClient.getService().getDrafts().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<DraftBill>>> call, Response<ApiResponse<List<DraftBill>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<DraftBill> drafts = response.body().getData();
                    if (drafts != null) {
                        for (DraftBill d : drafts) {
                            if (d.id.equals(draftId)) {
                                currentDraft = d;
                                etBuyerName.setText("");
                                renderItems(d.items);
                                break;
                            }
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<DraftBill>>> call, Throwable t) {}
        });
    }

    private void renderItems(List<DraftBillItem> items) {
        layoutItems.removeAllViews();
        if (items == null) return;
        for (DraftBillItem item : items) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_draft_item, layoutItems, false);
            ((TextView) row.findViewById(R.id.tv_draft_item_name)).setText(item.productName);
            ((TextView) row.findViewById(R.id.tv_draft_item_qty)).setText("x" + item.quantity);
            ((TextView) row.findViewById(R.id.tv_draft_item_price)).setText("¥" + String.format("%.2f", item.unitPrice));
            row.findViewById(R.id.tv_draft_item_qty).setOnClickListener(v -> {
                Intent intent = new Intent(NewOrderEditActivity.this, NewOrderEditQtyActivity.class);
                intent.putExtra("draftId", draftId);
                intent.putExtra("itemId", item.id);
                intent.putExtra("productName", item.productName);
                intent.putExtra("currentQty", item.quantity);
                startActivity(intent);
            });
            layoutItems.addView(row);
        }
    }

    private void saveDraft(boolean submit) {
        String buyerName = etBuyerName.getText().toString().trim();
        HashMap<String, Object> body = new HashMap<>();
        body.put("buyerName", buyerName);

        ApiClient.getService().updateDraft(draftId, body).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> resp) {
                if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                    if (submit) {
                        ApiClient.getService().submitDraft(draftId).enqueue(new Callback<>() {
                            @Override public void onResponse(Call<ApiResponse<Void>> c, Response<ApiResponse<Void>> r) {
                                if (r.isSuccessful() && r.body() != null && r.body().isSuccess()) {
                                    Toast.makeText(NewOrderEditActivity.this, "提交成功", Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            }
                            @Override public void onFailure(Call<ApiResponse<Void>> c, Throwable t) {}
                        });
                    } else {
                        Toast.makeText(NewOrderEditActivity.this, "已保存", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
        });
    }
}
