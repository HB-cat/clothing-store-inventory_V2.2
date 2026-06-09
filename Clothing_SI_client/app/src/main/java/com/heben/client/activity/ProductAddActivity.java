package com.heben.client.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.cardview.widget.CardView;

import com.google.android.material.textfield.TextInputEditText;
import com.heben.client.R;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.AttributeGroup;
import com.heben.client.model.Models.AttributeValue;
import com.heben.client.model.Models.Category;
import com.heben.client.model.Models.Product;
import com.heben.client.network.ApiClient;
import com.heben.client.util.SoundManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductAddActivity extends AppCompatActivity {

    private TextInputEditText etName, etPurchasePrice, etSellingPrice;

    // 分类
    private AppCompatSpinner spinnerCategory;
    private List<Category> categoryList = new ArrayList<>();
    private List<String> categoryNames = new ArrayList<>();

    // 属性
    private LinearLayout layoutAttrGroups, layoutCombinations;
    private CardView cardCombinations;
    private ScrollView scrollView;

    // 数据
    private List<AttributeGroup> attrGroupList = new ArrayList<>();
    // 每组选中的值: groupIdx → list of value strings
    private Map<Integer, List<String>> selectedValues = new HashMap<>();
    // 组合 stock 输入: attrsJson → EditText
    private Map<String, EditText> stockInputs = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_add);

        scrollView = findViewById(R.id.scrollView);
        etName = findViewById(R.id.et_product_name);
        etPurchasePrice = findViewById(R.id.et_purchase_price);
        etSellingPrice = findViewById(R.id.et_selling_price);
        spinnerCategory = findViewById(R.id.spinner_category);
        layoutAttrGroups = findViewById(R.id.layout_attr_groups);
        layoutCombinations = findViewById(R.id.layout_combinations);
        cardCombinations = findViewById(R.id.card_combinations);

        findViewById(R.id.btn_back).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            finish();
        });
        findViewById(R.id.btn_save_top).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            saveProduct();
        });

        loadCategories();
        loadAttributeGroups();
    }

    // ======================== 加载分类 ========================

    private void loadCategories() {
        ApiClient.getService().getCategories().enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Category>>> call, Response<ApiResponse<List<Category>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Category> data = response.body().getData();
                    if (data != null) {
                        categoryList.clear();
                        categoryList.addAll(data);
                        categoryNames.clear();
                        for (Category c : data) categoryNames.add(c.name);
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(ProductAddActivity.this,
                                android.R.layout.simple_spinner_item, categoryNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerCategory.setAdapter(adapter);
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Category>>> call, Throwable t) {
                Toast.makeText(ProductAddActivity.this, "加载分类失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ======================== 加载属性 ========================

    private void loadAttributeGroups() {
        ApiClient.getService().getAttributeGroups().enqueue(new Callback<ApiResponse<List<AttributeGroup>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<AttributeGroup>>> call, Response<ApiResponse<List<AttributeGroup>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<AttributeGroup> data = response.body().getData();
                    if (data != null) {
                        attrGroupList.clear();
                        attrGroupList.addAll(data);
                        renderAttributeChips();
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<AttributeGroup>>> call, Throwable t) {
                Toast.makeText(ProductAddActivity.this, "加载属性失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ======================== 渲染属性芯片 ========================

    private void renderAttributeChips() {
        layoutAttrGroups.removeAllViews();
        selectedValues.clear();

        for (int gi = 0; gi < attrGroupList.size(); gi++) {
            AttributeGroup group = attrGroupList.get(gi);
            if (group.values == null || group.values.isEmpty()) continue;

            // 组标题
            TextView tvLabel = new TextView(this);
            tvLabel.setText(group.name);
            tvLabel.setTextSize(13);
            tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            tvLabel.setTextColor(0xFF95A5A6);
            tvLabel.setPadding(0, 0, 0, 6);
            layoutAttrGroups.addView(tvLabel);

            // 芯片容器
            LinearLayout chipRow = new LinearLayout(this);
            chipRow.setOrientation(LinearLayout.HORIZONTAL);
            chipRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            chipRow.setPadding(0, 0, 0, 12);

            int px8 = (int) (8 * getResources().getDisplayMetrics().density + 0.5f);
            int px10 = (int) (10 * getResources().getDisplayMetrics().density + 0.5f);

            for (int vi = 0; vi < group.values.size(); vi++) {
                AttributeValue av = group.values.get(vi);
                final int groupIdx = gi;
                final String valStr = av.value;

                TextView chip = new TextView(this);
                chip.setText(valStr);
                chip.setTextSize(12);
                chip.setGravity(android.view.Gravity.CENTER);
                chip.setPadding(px10, 6, px10, 6);
                chip.setBackgroundResource(R.drawable.segment_bg);
                chip.setTextColor(0xFF2D3436);
                // tag 记录选中状态: 0=未选, 1=已选
                chip.setTag(0);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, (int) (36 * getResources().getDisplayMetrics().density + 0.5f));
                if (vi > 0) lp.leftMargin = px8;
                chip.setLayoutParams(lp);

                chip.setOnClickListener(v -> {
                    int selected = (int) chip.getTag();
                    if (selected == 0) {
                        chip.setTag(1);
                        chip.setBackgroundColor(0xFF4CAF50);
                        chip.setTextColor(0xFFFFFFFF);
                        addSelectedValue(groupIdx, valStr);
                    } else {
                        chip.setTag(0);
                        chip.setBackgroundResource(R.drawable.segment_bg);
                        chip.setTextColor(0xFF2D3436);
                        removeSelectedValue(groupIdx, valStr);
                    }
                    regenerateCombinations();
                });

                chipRow.addView(chip);
            }
            layoutAttrGroups.addView(chipRow);
        }
    }

    private void addSelectedValue(int groupIdx, String val) {
        List<String> list = selectedValues.get(groupIdx);
        if (list == null) {
            list = new ArrayList<>();
            selectedValues.put(groupIdx, list);
        }
        if (!list.contains(val)) list.add(val);
    }

    private void removeSelectedValue(int groupIdx, String val) {
        List<String> list = selectedValues.get(groupIdx);
        if (list != null) {
            list.remove(val);
            if (list.isEmpty()) selectedValues.remove(groupIdx);
        }
    }

    // ======================== 生成组合表 ========================

    private void regenerateCombinations() {
        stockInputs.clear();
        layoutCombinations.removeAllViews();
        cardCombinations.setVisibility(View.GONE);

        // 只有 >=2 组有选中值才生成
        int groupCount = 0;
        for (int gi = 0; gi < attrGroupList.size(); gi++) {
            if (selectedValues.containsKey(gi) && !selectedValues.get(gi).isEmpty()) {
                groupCount++;
            }
        }
        if (groupCount < 2) return;

        // 收集有选中值的组
        List<Integer> activeGroups = new ArrayList<>();
        for (int gi = 0; gi < attrGroupList.size(); gi++) {
            if (selectedValues.containsKey(gi) && !selectedValues.get(gi).isEmpty()) {
                activeGroups.add(gi);
            }
        }

        // 笛卡尔积生成组合
        List<List<String>> allCombos = cartesianProduct(activeGroups, 0);
        if (allCombos.isEmpty()) return;

        // 渲染表格标题行
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setPadding(0, 8, 0, 8);

        for (int gi : activeGroups) {
            TextView th = new TextView(this);
            th.setText(attrGroupList.get(gi).name);
            th.setTextSize(13);
            th.setTypeface(null, android.graphics.Typeface.BOLD);
            th.setTextColor(0xFF95A5A6);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            th.setLayoutParams(lp);
            headerRow.addView(th);
        }
        TextView thStock = new TextView(this);
        thStock.setText("库存");
        thStock.setTextSize(13);
        thStock.setTypeface(null, android.graphics.Typeface.BOLD);
        thStock.setTextColor(0xFF95A5A6);
        LinearLayout.LayoutParams lpStock = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        thStock.setLayoutParams(lpStock);
        headerRow.addView(thStock);

        layoutCombinations.addView(headerRow);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(0xFFE8EAED);
        layoutCombinations.addView(divider);

        // 每一行
        for (List<String> combo : allCombos) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 10, 0, 10);

            // attrsJson: {"属性组名":"值",...}
            StringBuilder jsonSb = new StringBuilder("{");
            for (int i = 0; i < combo.size(); i++) {
                if (i > 0) jsonSb.append(",");
                int gIdx = activeGroups.get(i);
                jsonSb.append("\"").append(escapeJson(attrGroupList.get(gIdx).name))
                        .append("\":\"")
                        .append(escapeJson(combo.get(i)))
                        .append("\"");
            }
            jsonSb.append("}");
            final String attrsJson = jsonSb.toString();

            for (int i = 0; i < combo.size(); i++) {
                TextView td = new TextView(this);
                td.setText(combo.get(i));
                td.setTextSize(13);
                td.setTextColor(0xFF2D3436);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                td.setLayoutParams(lp);
                row.addView(td);
            }

            // 库存输入
            EditText etStock = new EditText(this);
            etStock.setHint("0");
            etStock.setText("0");
            etStock.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            etStock.setTextSize(13);
            etStock.setBackgroundResource(R.drawable.segment_bg);
            etStock.setPadding(8, 4, 8, 4);
            LinearLayout.LayoutParams lpEdt = new LinearLayout.LayoutParams(0,
                    (int) (36 * getResources().getDisplayMetrics().density + 0.5f), 1);
            lpEdt.leftMargin = 8;
            etStock.setLayoutParams(lpEdt);
            row.addView(etStock);
            stockInputs.put(attrsJson, etStock);

            layoutCombinations.addView(row);
        }

        cardCombinations.setVisibility(View.VISIBLE);

        // 滚动到底部看到组合表
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private List<List<String>> cartesianProduct(List<Integer> groups, int depth) {
        List<List<String>> result = new ArrayList<>();
        if (depth >= groups.size()) {
            result.add(new ArrayList<>());
            return result;
        }
        int gIdx = groups.get(depth);
        List<String> vals = selectedValues.get(gIdx);
        if (vals == null || vals.isEmpty()) return result;

        List<List<String>> sub = cartesianProduct(groups, depth + 1);
        for (String v : vals) {
            for (List<String> s : sub) {
                List<String> row = new ArrayList<>();
                row.add(v);
                row.addAll(s);
                result.add(row);
            }
        }
        return result;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ======================== 保存 ========================

    private void saveProduct() {
        String name = etName.getText().toString().trim();
        String purchaseStr = etPurchasePrice.getText().toString().trim();
        String sellingStr = etSellingPrice.getText().toString().trim();
        int catPos = spinnerCategory.getSelectedItemPosition();

        if (name.isEmpty()) { Toast.makeText(this, "请输入商品名称", Toast.LENGTH_SHORT).show(); return; }
        if (purchaseStr.isEmpty()) { Toast.makeText(this, "请输入进价", Toast.LENGTH_SHORT).show(); return; }
        if (sellingStr.isEmpty()) { Toast.makeText(this, "请输入售价", Toast.LENGTH_SHORT).show(); return; }
        if (catPos < 0 || catPos >= categoryList.size()) { Toast.makeText(this, "请选择分类", Toast.LENGTH_SHORT).show(); return; }

        double purchasePrice = Double.parseDouble(purchaseStr);
        double sellingPrice = Double.parseDouble(sellingStr);
        long categoryId = categoryList.get(catPos).id;

        // 构建组合列表
        List<Map<String, Object>> combos = new ArrayList<>();
        if (!stockInputs.isEmpty()) {
            for (Map.Entry<String, EditText> entry : stockInputs.entrySet()) {
                String stockStr = entry.getValue().getText().toString().trim();
                int stock = 0;
                if (!stockStr.isEmpty()) {
                    try { stock = Integer.parseInt(stockStr); } catch (NumberFormatException ignored) {}
                }
                Map<String, Object> item = new HashMap<>();
                item.put("attrsJson", entry.getKey());
                item.put("stock", stock);
                combos.add(item);
            }
        } else {
            // 无属性组合时也创建一个默认组合
            Map<String, Object> item = new HashMap<>();
            item.put("attrsJson", "{}");
            item.put("stock", 0);
            combos.add(item);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("categoryId", categoryId);
        body.put("purchasePrice", purchasePrice);
        body.put("sellingPrice", sellingPrice);
        body.put("combinations", combos);

        ApiClient.getService().addProduct(body).enqueue(new Callback<ApiResponse<Product>>() {
            @Override
            public void onResponse(Call<ApiResponse<Product>> call, Response<ApiResponse<Product>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(ProductAddActivity.this, "✅ 添加成功", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "添加失败";
                    Toast.makeText(ProductAddActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Product>> call, Throwable t) {
                Toast.makeText(ProductAddActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
