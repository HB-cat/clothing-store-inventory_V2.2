package com.heben.client.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.heben.client.R;
import com.heben.client.adapter.ChatMessageAdapter;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.ChatMessage;
import com.heben.client.network.ApiClient;
import com.heben.client.util.TokenManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatDetailActivity extends AppCompatActivity {

    private RecyclerView rv;
    private ChatMessageAdapter adapter;
    private List<ChatMessage> messages = new ArrayList<>();
    private EditText etMessage;
    private MaterialButton btnSend;
    private TextView btnPickProduct;

    private long sessionId;
    private Long otherUserId;
    private String otherName;

    /** 卖家商品缓存 */
    private List<Map<String, Object>> sellerProducts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        sessionId = getIntent().getLongExtra("sessionId", 0);
        otherUserId = getIntent().hasExtra("otherUserId") ? getIntent().getLongExtra("otherUserId", 0) : null;
        otherName = getIntent().getStringExtra("otherName");

        if (otherName != null) {
            TextView tvTitle = findViewById(R.id.tv_chat_title);
            if (tvTitle != null) tvTitle.setText(otherName);
        }

        rv = findViewById(R.id.rv_messages);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatMessageAdapter(messages);
        rv.setAdapter(adapter);

        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnSend.setOnClickListener(v -> sendMessage());

        // 商品选择按钮
        btnPickProduct = findViewById(R.id.btn_pick_product);
        btnPickProduct.setOnClickListener(v -> showProductPicker());

        loadProducts();

        if (sessionId <= 0 && otherUserId != null) {
            createSessionAndLoad();
        } else if (sessionId > 0) {
            loadMessages();
        } else {
            Toast.makeText(this, "无法开始聊天", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /** 加载卖家商品列表 */
    @SuppressWarnings("unchecked")
    private void loadProducts() {
        if (otherUserId == null) {
            btnPickProduct.setVisibility(android.view.View.GONE);
            return;
        }
        btnPickProduct.setVisibility(android.view.View.VISIBLE);
        ApiClient.getService().getRetailerProducts(otherUserId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Map<String, Object>>>> call,
                                   Response<ApiResponse<List<Map<String, Object>>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Map<String, Object>> data = response.body().getData();
                    if (data != null) sellerProducts = data;
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Map<String, Object>>>> call, Throwable t) {}
        });
    }

    /** 弹出商品选择器 → 选中后发送商品消息 */
    @SuppressWarnings("unchecked")
    private void showProductPicker() {
        if (sellerProducts.isEmpty()) {
            loadProducts();
            Toast.makeText(this, "商品列表加载中，请稍后", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[sellerProducts.size()];
        for (int i = 0; i < sellerProducts.size(); i++) {
            Map<String, Object> p = sellerProducts.get(i);
            String name = (String) p.get("name");
            double price = ((Number) p.get("sellingPrice")).doubleValue();
            names[i] = name + "  ¥" + String.format("%.0f", price);
        }

        new AlertDialog.Builder(this)
                .setTitle("选择商品发送给卖家")
                .setItems(names, (dialog, which) -> {
                    Map<String, Object> p = sellerProducts.get(which);
                    String productMsg = buildProductDetail(p);
                    sendProductMessage(productMsg);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 构建商品详情文本 */
    @SuppressWarnings("unchecked")
    private String buildProductDetail(Map<String, Object> p) {
        StringBuilder sb = new StringBuilder();
        sb.append("📦 ").append(p.get("name"));
        Object catObj = p.get("categoryName");
        if (catObj instanceof String && !((String) catObj).isEmpty()) {
            sb.append("（").append(catObj).append("）");
        }
        Number sellNum = (Number) p.get("sellingPrice");
        Number buyNum = (Number) p.get("purchasePrice");
        double sell = sellNum != null ? sellNum.doubleValue() : 0;
        double buy = buyNum != null ? buyNum.doubleValue() : 0;
        sb.append("\n售价：¥").append(String.format("%.0f", sell));
        sb.append("  进价：¥").append(String.format("%.0f", buy));
        Object combos = p.get("combinations");
        if (combos instanceof List && !((List) combos).isEmpty()) {
            sb.append("\n规格：");
            List<Map<String, Object>> list = (List<Map<String, Object>>) combos;
            for (int i = 0; i < list.size(); i++) {
                Map<String, Object> c = list.get(i);
                if (i > 0) sb.append(" ｜ ");
                String ad = c.get("attrsDisplay") instanceof String ? (String) c.get("attrsDisplay") : null;
                sb.append(ad != null ? ad : c.get("attrsJson"));
                Number stockNum = (Number) c.get("stock");
                sb.append("（库存").append(stockNum != null ? stockNum : "0").append("）");
            }
        }
        return sb.toString();
    }

    /** 发送商品消息 */
    private void sendProductMessage(String text) {
        if (sessionId <= 0) {
            Toast.makeText(this, "会话未就绪", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSend.setEnabled(false);

        HashMap<String, Object> body = new HashMap<>();
        body.put("messageType", 1);
        body.put("content", text);

        ApiClient.getService().sendMessage(sessionId, body).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                btnSend.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    loadMessages();
                } else {
                    Toast.makeText(ChatDetailActivity.this, "发送失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                btnSend.setEnabled(true);
                Toast.makeText(ChatDetailActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createSessionAndLoad() {
        HashMap<String, Object> body = new HashMap<>();
        body.put("otherUserId", otherUserId);

        ApiClient.getService().createChatSession(body).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                                   Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Map<String, Object> data = response.body().getData();
                    if (data != null && data.get("id") != null) {
                        sessionId = ((Number) data.get("id")).longValue();
                        loadMessages();
                    }
                } else {
                    Toast.makeText(ChatDetailActivity.this, "创建会话失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(ChatDetailActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMessages() {
        if (sessionId <= 0) return;

        Long myId = TokenManager.getUserId();

        ApiClient.getService().getChatMessages(sessionId, 1, 50).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ChatMessage>>> call,
                                   Response<ApiResponse<List<ChatMessage>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    messages.clear();
                    List<ChatMessage> data = response.body().getData();
                    if (data != null) {
                        for (ChatMessage m : data) {
                            m.isMine = myId != null && myId.equals(m.senderId);
                            messages.add(m);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    if (!messages.isEmpty()) rv.smoothScrollToPosition(messages.size() - 1);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<ChatMessage>>> call, Throwable t) {
            }
        });
    }

    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "请输入消息", Toast.LENGTH_SHORT).show();
            return;
        }
        if (sessionId <= 0) {
            Toast.makeText(this, "会话未就绪", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSend.setEnabled(false);

        HashMap<String, Object> body = new HashMap<>();
        body.put("messageType", 1);
        body.put("content", content);

        ApiClient.getService().sendMessage(sessionId, body).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                btnSend.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    etMessage.setText("");
                    loadMessages();
                } else {
                    Toast.makeText(ChatDetailActivity.this, "发送失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                btnSend.setEnabled(true);
                Toast.makeText(ChatDetailActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
