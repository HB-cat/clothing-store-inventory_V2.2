package com.heben.client.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.heben.client.R;
import com.heben.client.activity.*;
import com.heben.client.agent.AgentSuggestionManager;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.LowStockProduct;
import com.heben.client.network.ApiClient;
import com.heben.client.widget.AgentBottomSheetDialog;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        v.findViewById(R.id.btn_purchase).setOnClickListener(x -> startActivity(new Intent(getActivity(), PurchaseActivity.class)));
        v.findViewById(R.id.btn_new_order).setOnClickListener(x -> startActivity(new Intent(getActivity(), NewOrderActivity.class)));
        v.findViewById(R.id.btn_sale).setOnClickListener(x -> startActivity(new Intent(getActivity(), SaleActivity.class)));
        v.findViewById(R.id.todo_card).setOnClickListener(x -> startActivity(new Intent(getActivity(), NewOrderActivity.class)));

        v.findViewById(R.id.et_quick_cmd).setOnClickListener(x -> showAgentSheet());
        v.findViewById(R.id.et_quick_cmd).setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                view.clearFocus();
                showAgentSheet();
            }
        });

        // 下拉刷新
        SwipeRefreshLayout swipeRefresh = v.findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(() -> {
            checkStockAlert();
            swipeRefresh.postDelayed(() -> {
                if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
            }, 1500);
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 从 SharedPreferences 读取用户自定义状态文字
        SharedPreferences prefs = requireContext().getSharedPreferences("profile_prefs", Context.MODE_PRIVATE);
        String text = prefs.getString("home_status_text", "🎵 天天发财 · 客似云来 · 生意兴隆");
        TextView tvStatus = getView().findViewById(R.id.tv_home_status);
        if (tvStatus != null) {
            tvStatus.setText(text);
        }
        checkStockAlert();
    }

    // ========== 库存预警 ==========

    private void checkStockAlert() {
        ApiClient.getService().getLowStockProducts().enqueue(new Callback<ApiResponse<List<LowStockProduct>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<LowStockProduct>>> call,
                                   Response<ApiResponse<List<LowStockProduct>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<LowStockProduct> list = response.body().getData();
                    AgentSuggestionManager.setStockAlerts(list != null ? list.size() : 0);
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<LowStockProduct>>> call, Throwable t) {}
        });
    }

    private void showAgentSheet() {
        new AgentBottomSheetDialog().show(getParentFragmentManager(), "agent_sheet");
    }
}
