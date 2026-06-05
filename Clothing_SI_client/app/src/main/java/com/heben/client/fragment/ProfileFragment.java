package com.heben.client.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.heben.client.R;
import com.heben.client.activity.*;
import com.heben.client.util.TokenManager;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        // 设置昵称
        TextView tvNick = v.findViewById(R.id.tv_nickname);
        String nick = TokenManager.getNickname();
        if (nick != null && !nick.isEmpty()) tvNick.setText(nick);

        // 头像点击编辑
        v.findViewById(R.id.avatar_layout).setOnClickListener(x -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
            builder.setTitle("修改昵称")
                    .setItems(new String[]{"修改头像", "修改昵称"}, (dialog, which) -> {
                        if (which == 0) android.widget.Toast.makeText(getActivity(), "点击更换头像", android.widget.Toast.LENGTH_SHORT).show();
                        else {
                            android.app.AlertDialog.Builder input = new android.app.AlertDialog.Builder(getActivity());
                            input.setTitle("修改昵称");
                            final android.widget.EditText et = new android.widget.EditText(getActivity());
                            et.setText(tvNick.getText());
                            input.setView(et);
                            input.setPositiveButton("确定", (d, w) -> tvNick.setText(et.getText().toString()));
                            input.setNegativeButton("取消", null);
                            input.show();
                        }
                    }).show();
        });

        // 6个模块按钮
        v.findViewById(R.id.module_analytics).setOnClickListener(x -> startActivity(new Intent(getActivity(), AnalyticsActivity.class)));
        v.findViewById(R.id.module_products).setOnClickListener(x -> startActivity(new Intent(getActivity(), ProductsActivity.class)));
        v.findViewById(R.id.module_add_product).setOnClickListener(x -> startActivity(new Intent(getActivity(), ProductAddActivity.class)));
        v.findViewById(R.id.module_stock).setOnClickListener(x -> startActivity(new Intent(getActivity(), PurchaseActivity.class)));
        v.findViewById(R.id.module_categories).setOnClickListener(x -> startActivity(new Intent(getActivity(), CategoriesActivity.class)));
        v.findViewById(R.id.module_attributes).setOnClickListener(x -> startActivity(new Intent(getActivity(), AttributesActivity.class)));

        // 设置
        v.findViewById(R.id.settings_data).setOnClickListener(x -> startActivity(new Intent(getActivity(), DataManageActivity.class)));
        v.findViewById(R.id.btn_logout).setOnClickListener(x -> {
            TokenManager.logout();
            Intent i = new Intent(getActivity(), LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });

        return v;
    }
}
