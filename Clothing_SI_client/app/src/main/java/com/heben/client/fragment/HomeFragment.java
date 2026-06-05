package com.heben.client.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.heben.client.R;
import com.heben.client.activity.*;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        v.findViewById(R.id.btn_purchase).setOnClickListener(x -> startActivity(new Intent(getActivity(), PurchaseActivity.class)));
        v.findViewById(R.id.btn_new_order).setOnClickListener(x -> startActivity(new Intent(getActivity(), NewOrderActivity.class)));
        v.findViewById(R.id.btn_sale).setOnClickListener(x -> startActivity(new Intent(getActivity(), SaleActivity.class)));
        v.findViewById(R.id.todo_card).setOnClickListener(x -> startActivity(new Intent(getActivity(), NewOrderActivity.class)));
        v.findViewById(R.id.et_quick_cmd).setOnClickListener(x -> startActivity(new Intent(getActivity(), AgentPanelActivity.class)));
        v.findViewById(R.id.et_quick_cmd).setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                startActivity(new Intent(getActivity(), AgentPanelActivity.class));
            }
        });

        return v;
    }
}
