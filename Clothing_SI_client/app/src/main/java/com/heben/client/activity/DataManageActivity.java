package com.heben.client.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.heben.client.R;

public class DataManageActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_manage);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        MaterialButton btnExport = findViewById(R.id.btn_export);
        MaterialButton btnImport = findViewById(R.id.btn_import);
        MaterialButton btnBackup = findViewById(R.id.btn_backup);

        btnExport.setOnClickListener(v -> Toast.makeText(this, "导出功能开发中", Toast.LENGTH_SHORT).show());
        btnImport.setOnClickListener(v -> Toast.makeText(this, "导入功能开发中", Toast.LENGTH_SHORT).show());
        btnBackup.setOnClickListener(v -> Toast.makeText(this, "备份功能开发中", Toast.LENGTH_SHORT).show());
    }
}
