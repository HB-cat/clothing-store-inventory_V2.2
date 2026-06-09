package com.heben.client.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.heben.client.R;
import com.heben.client.activity.*;
import com.heben.client.util.TokenManager;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private SharedPreferences prefs;
    private List<ModuleItem> moduleList;
    private RecyclerView moduleContainer;
    private ModuleAdapter adapter;
    private int currentRole;
    private TextView tvSortHint;
    private SwipeRefreshLayout swipeRefresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);
        prefs = getActivity().getSharedPreferences("profile_prefs", Context.MODE_PRIVATE);
        currentRole = TokenManager.getRole();

        swipeRefresh = v.findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(() -> {
            swipeRefresh.postDelayed(() -> {
                if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
            }, 500);
        });

        // 店铺信息
        TextView tvNick = v.findViewById(R.id.tv_nickname);
        String nick = TokenManager.getNickname();
        String shop = TokenManager.getShopName();
        tvNick.setText(shop != null && !shop.isEmpty() ? shop : (nick != null ? nick : "用户"));

        // 排序提示 + 模块 RecyclerView
        moduleContainer = v.findViewById(R.id.module_container);
        tvSortHint = v.findViewById(R.id.tv_sort_hint);
        boolean isSeller = currentRole == 1 || currentRole == 2;
        tvSortHint.setVisibility(isSeller ? View.VISIBLE : View.GONE);

        if (isSeller) {
            moduleList = buildModuleList();
            adapter = new ModuleAdapter(moduleList);
            moduleContainer.setLayoutManager(new LinearLayoutManager(getActivity()));
            moduleContainer.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));

            // ItemTouchHelper: 长按拖拽排序
            ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
                @Override
                public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder dragged,
                                      @NonNull RecyclerView.ViewHolder target) {
                    int from = dragged.getAdapterPosition();
                    int to = target.getAdapterPosition();
                    if (from < 0 || to < 0 || from >= moduleList.size() || to >= moduleList.size()) return false;
                    ModuleItem tmp = moduleList.get(from);
                    moduleList.set(from, moduleList.get(to));
                    moduleList.set(to, tmp);
                    adapter.notifyItemMoved(from, to);
                    saveOrder();
                    return true;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
            };

            ItemTouchHelper helper = new ItemTouchHelper(callback);
            helper.attachToRecyclerView(moduleContainer);
            moduleContainer.setAdapter(adapter);
        }

        // 设置区
        v.findViewById(R.id.settings_program).setOnClickListener(x ->
                startActivity(new Intent(getActivity(), ProgramSettingsActivity.class)));
        v.findViewById(R.id.settings_guide).setOnClickListener(x ->
                startActivity(new Intent(getActivity(),
                        currentRole == 3 ? BuyerGuideActivity.class : SellerGuideActivity.class)));
        v.findViewById(R.id.settings_about).setOnClickListener(x -> {
            new android.app.AlertDialog.Builder(getActivity())
                    .setTitle("关于")
                    .setMessage("Clothing-SI v2.2\nHeBen_Cat ©2026")
                    .setPositiveButton("确定", null)
                    .show();
        });

        // 退出
        v.findViewById(R.id.btn_logout).setOnClickListener(x -> {
            TokenManager.logout();
            Intent i = new Intent(getActivity(), LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });

        // 头像点击 → 更换头像
        v.findViewById(R.id.avatar_layout).setOnClickListener(v1 -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 1001);
        });

        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == getActivity().RESULT_OK && data != null) {
            Toast.makeText(getActivity(), "头像已选择", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveOrder() {
        StringBuilder order = new StringBuilder();
        for (ModuleItem m : moduleList) order.append(m.name).append(",");
        prefs.edit().putString("module_order", order.toString()).apply();
    }

    // ui_preview 顺序：经营分析、商品管理、添加商品、库存查看、分类管理、属性管理
    private List<ModuleItem> buildModuleList() {
        List<ModuleItem> all = new ArrayList<>();
        all.add(new ModuleItem("经营分析", "📊", "快报·趋势·画像", AnalyticsActivity.class));
        all.add(new ModuleItem("商品管理", "👕", "列表·搜索·筛选", ProductsActivity.class));
        all.add(new ModuleItem("添加商品", "➕", "三步流程·组合库存", ProductAddActivity.class));
        all.add(new ModuleItem("库存查看", "📦", "实时库存·属性组合", PurchaseActivity.class));
        all.add(new ModuleItem("分类管理", "📂", "动态增删·拖拽排序", CategoriesActivity.class));
        all.add(new ModuleItem("属性管理", "🏷️", "颜色·尺码·材质", AttributesActivity.class));
        // 读取保存的顺序
        String order = prefs.getString("module_order", "");
        if (!order.isEmpty()) {
            String[] ids = order.split(",");
            List<ModuleItem> sorted = new ArrayList<>();
            for (String id : ids) {
                for (ModuleItem m : all) {
                    if (m.name.equals(id)) { sorted.add(m); break; }
                }
            }
            for (ModuleItem m : all) {
                if (!sorted.contains(m)) sorted.add(m);
            }
            return sorted;
        }
        return all;
    }

    // ---------- RecyclerView Adapter ----------
    private class ModuleAdapter extends RecyclerView.Adapter<ModuleAdapter.VH> {
        private final List<ModuleItem> list;

        ModuleAdapter(List<ModuleItem> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(getActivity()).inflate(R.layout.item_profile_module, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ModuleItem item = list.get(position);
            holder.icon.setText(item.icon);
            holder.name.setText(item.name);
            holder.desc.setText(item.desc);
            holder.itemView.setOnClickListener(v -> {
                if (item.cls != null) startActivity(new Intent(getActivity(), item.cls));
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView icon, name, desc;
            VH(View v) {
                super(v);
                icon = v.findViewById(R.id.tv_icon);
                name = v.findViewById(R.id.tv_name);
                desc = v.findViewById(R.id.tv_desc);
            }
        }
    }

    static class ModuleItem {
        String name, icon, desc;
        Class<?> cls;
        ModuleItem(String name, String icon, String desc, Class<?> cls) {
            this.name = name; this.icon = icon; this.desc = desc; this.cls = cls;
        }
    }
}
