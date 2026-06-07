package com.heben.client.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.heben.client.R;
import com.heben.client.local.sync.NetworkUtil;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.DraftBill;
import com.heben.client.model.Models.DraftBillItem;
import com.heben.client.model.Models.Product;
import com.heben.client.network.ApiClient;
import com.heben.client.util.SoundManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewOrderActivity extends AppCompatActivity {

    private List<DraftBill> draftList = new ArrayList<>();
    private DraftBill currentDraft;
    private LinearLayout layoutDraftCards, layoutDraftItems, layoutBuyerDisplay;
    private TextView tvDraftTitle, tvDraftTotal, tvSelectedBuyer;
    private String selectedBuyerName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_order);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_submit_top).setOnClickListener(v -> submitDraft());

        layoutDraftCards = findViewById(R.id.layout_draft_cards);
        layoutDraftItems = findViewById(R.id.layout_draft_items);
        layoutBuyerDisplay = findViewById(R.id.layout_buyer_display);
        tvDraftTitle = findViewById(R.id.tv_draft_title);
        tvDraftTotal = findViewById(R.id.tv_draft_total);
        tvSelectedBuyer = findViewById(R.id.tv_selected_buyer);

        // Segment control
        // 添加商品 → 弹出商品选择器（直接加，不进页面）
        findViewById(R.id.btn_add_item).setOnClickListener(v -> showProductPicker());

        // 选择买家
        findViewById(R.id.btn_select_buyer).setOnClickListener(v -> showBuyerPicker());

        // 清除买家
        findViewById(R.id.btn_clear_buyer).setOnClickListener(v -> {
            selectedBuyerName = "";
            layoutBuyerDisplay.setVisibility(View.GONE);
            renderCurrentDraft();
        });

        // 提交
        findViewById(R.id.btn_submit_order).setOnClickListener(v -> submitDraft());

        loadDrafts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDrafts();
    }

    private void loadDrafts() {
        if (!NetworkUtil.isConnected(this)) return;
        ApiClient.getService().getDrafts().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<DraftBill>>> call, Response<ApiResponse<List<DraftBill>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<DraftBill> data = response.body().getData();
                    long prevId = currentDraft != null ? currentDraft.id : 0;
                    draftList.clear();
                    if (data != null) draftList.addAll(data);

                    // 恢复选中的草稿
                    currentDraft = null;
                    if (prevId != 0) {
                        for (DraftBill d : draftList) {
                            if (d.id.equals(prevId)) { currentDraft = d; break; }
                        }
                    }
                    if (currentDraft == null && !draftList.isEmpty()) {
                        currentDraft = draftList.get(0);
                    }
                    // 同步当前草稿的买家名
                    if (currentDraft != null && currentDraft.buyerName != null
                            && !currentDraft.buyerName.isEmpty() && !"散客".equals(currentDraft.buyerName)) {
                        selectedBuyerName = currentDraft.buyerName;
                        layoutBuyerDisplay.setVisibility(View.VISIBLE);
                        tvSelectedBuyer.setText("买家: " + currentDraft.buyerName);
                    } else if (currentDraft == null) {
                        selectedBuyerName = "";
                        layoutBuyerDisplay.setVisibility(View.GONE);
                    }
                    renderDraftCards();
                    renderCurrentDraft();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<DraftBill>>> call, Throwable t) {}
        });
    }

    private String buyerSuffix() {
        if (!selectedBuyerName.isEmpty()) return " - " + selectedBuyerName;
        if (currentDraft != null && currentDraft.buyerName != null
                && !currentDraft.buyerName.isEmpty() && !"散客".equals(currentDraft.buyerName)) {
            return " - " + currentDraft.buyerName;
        }
        return "";
    }

    private void renderDraftCards() {
        layoutDraftCards.removeAllViews();
        for (DraftBill d : draftList) {
            View card = LayoutInflater.from(this).inflate(R.layout.item_draft_card, layoutDraftCards, false);
            String cardTitle = "#" + d.draftNo;
            if (d.buyerName != null && !d.buyerName.isEmpty() && !"散客".equals(d.buyerName)) {
                cardTitle += " - " + d.buyerName;
            }
            ((TextView) card.findViewById(android.R.id.text1)).setText(cardTitle);
            String info = (d.itemCount > 0 ? d.itemCount + "件" : "空") + " · ¥" + String.format("%.0f", d.totalAmount);
            ((TextView) card.findViewById(android.R.id.text2)).setText(info);
            card.setSelected(d.id.equals(currentDraft != null ? currentDraft.id : 0));
            card.setOnClickListener(v -> {
                currentDraft = d;
                // 同步本地买家名
                if (d.buyerName != null && !d.buyerName.isEmpty() && !"散客".equals(d.buyerName)) {
                    selectedBuyerName = d.buyerName;
                    layoutBuyerDisplay.setVisibility(View.VISIBLE);
                    tvSelectedBuyer.setText("买家: " + d.buyerName);
                }
                renderDraftCards();
                renderCurrentDraft();
            });
            layoutDraftCards.addView(card);
        }
    }

    private void renderCurrentDraft() {
        if (currentDraft == null) {
            tvDraftTitle.setText("📄 暂无草稿");
            tvDraftTotal.setText("合计: ¥0");
            layoutDraftItems.removeAllViews();
            return;
        }
        tvDraftTitle.setText("📄 草稿 #" + currentDraft.draftNo + buyerSuffix());
        tvDraftTotal.setText("合计: ¥" + String.format("%.0f", currentDraft.totalAmount));

        layoutDraftItems.removeAllViews();
        if (currentDraft.items == null || currentDraft.items.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("暂未添加商品，点击下方「添加商品」开始");
            empty.setTextSize(13f);
            empty.setTextColor(0xFF95A5A6);
            empty.setPadding(16, 24, 16, 24);
            empty.setGravity(android.view.Gravity.CENTER);
            layoutDraftItems.addView(empty);
            return;
        }
        for (DraftBillItem item : currentDraft.items) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_draft_item, layoutDraftItems, false);
            ((TextView) row.findViewById(R.id.tv_draft_item_name)).setText(item.productName);
            ((TextView) row.findViewById(R.id.tv_draft_item_qty)).setText("x" + item.quantity);
            ((TextView) row.findViewById(R.id.tv_draft_item_price)).setText("¥" + String.format("%.2f", item.unitPrice));
            row.findViewById(R.id.tv_draft_item_qty).setOnClickListener(v -> deleteItem(item));
            layoutDraftItems.addView(row);
        }
    }

    private void deleteItem(DraftBillItem item) {
        if (!NetworkUtil.isConnected(this)) {
            Toast.makeText(this, "离线模式不可操作", Toast.LENGTH_SHORT).show();
            return;
        }
        ApiClient.getService().removeDraftItem(currentDraft.id, item.id).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                loadDrafts();
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(NewOrderActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showProductPicker() {
        if (!NetworkUtil.isConnected(this)) {
            Toast.makeText(this, "离线模式暂不支持添加商品", Toast.LENGTH_LONG).show();
            return;
        }

        // 确保有当前草稿
        ensureCurrentDraft(() -> {
            // 加载商品列表
            ApiClient.getService().getProducts(null, null).enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Product>>> call, Response<ApiResponse<List<Product>>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        List<Product> products = response.body().getData();
                        if (products == null || products.isEmpty()) {
                            Toast.makeText(NewOrderActivity.this, "暂无商品，请先添加", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String[] names = new String[products.size()];
                        for (int i = 0; i < products.size(); i++) {
                            names[i] = products.get(i).name + "  ¥" + String.format("%.0f", products.get(i).sellingPrice);
                        }
                        new AlertDialog.Builder(NewOrderActivity.this)
                                .setTitle("选择商品")
                                .setItems(names, (dialog, which) -> {
                                    Product p = products.get(which);
                                    addProductToDraft(p);
                                })
                                .setNegativeButton("取消", null)
                                .show();
                    } else {
                        Toast.makeText(NewOrderActivity.this, "加载商品失败", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<ApiResponse<List<Product>>> call, Throwable t) {
                    Toast.makeText(NewOrderActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void ensureCurrentDraft(Runnable callback) {
        if (currentDraft != null) {
            callback.run();
            return;
        }
        // 先创建草稿
        ApiClient.getService().createDraft().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<DraftBill>> call, Response<ApiResponse<DraftBill>> resp) {
                if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                    DraftBill newDraft = resp.body().getData();
                    if (newDraft != null) {
                        // 直接从响应设置当前草稿，不等 loadDrafts
                        draftList.add(newDraft);
                        currentDraft = newDraft;
                        renderDraftCards();
                        renderCurrentDraft();
                    }
                    callback.run();
                } else {
                    Toast.makeText(NewOrderActivity.this, "创建草稿失败", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<DraftBill>> call, Throwable t) {
                Toast.makeText(NewOrderActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addProductToDraft(Product product) {
        if (currentDraft == null) return;
        HashMap<String, Object> body = new HashMap<>();
        body.put("productId", product.id);
        body.put("quantity", 1);
        body.put("unitPrice", product.sellingPrice);

        ApiClient.getService().addDraftItem(currentDraft.id, body).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(NewOrderActivity.this, "已添加", Toast.LENGTH_SHORT).show();
                    loadDrafts(); // 刷新展示商品列表
                } else {
                    Toast.makeText(NewOrderActivity.this, "添加失败", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(NewOrderActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showBuyerPicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择买家");

        EditText input = new EditText(this);
        input.setHint("输入买家姓名");
        input.setPadding(40, 20, 40, 20);
        builder.setView(input);

        builder.setPositiveButton("确定", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                selectedBuyerName = name;
                layoutBuyerDisplay.setVisibility(View.VISIBLE);
                tvSelectedBuyer.setText("买家: " + name);
                renderCurrentDraft();
                Toast.makeText(this, "已选择买家: " + name, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void submitDraft() {
        if (!NetworkUtil.isConnected(this)) {
            Toast.makeText(this, "离线模式暂不支持提交", Toast.LENGTH_LONG).show();
            return;
        }
        if (currentDraft == null) {
            Toast.makeText(this, "没有可提交的草稿", Toast.LENGTH_SHORT).show();
            return;
        }
        HashMap<String, Object> body = new HashMap<>();
        String buyer = selectedBuyerName;
        if (buyer.isEmpty() && currentDraft.buyerName != null && !currentDraft.buyerName.isEmpty()) {
            buyer = currentDraft.buyerName;
        }
        body.put("buyerName", buyer.isEmpty() ? "散客" : buyer);

        ApiClient.getService().submitDraft(currentDraft.id, body).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> resp) {
                if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                    Toast.makeText(NewOrderActivity.this, "账单已提交 ✅", Toast.LENGTH_SHORT).show();
                    SoundManager.getInstance(NewOrderActivity.this).playSaleSuccess();
                    selectedBuyerName = "";
                    layoutBuyerDisplay.setVisibility(View.GONE);
                    currentDraft = null;
                    loadDrafts();
                } else {
                    String msg = resp.body() != null ? resp.body().getMessage() : "提交失败";
                    Toast.makeText(NewOrderActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(NewOrderActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
