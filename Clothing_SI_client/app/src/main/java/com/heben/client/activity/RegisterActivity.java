package com.heben.client.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.heben.client.R;
import com.heben.client.network.ApiClient;
import com.heben.client.network.ApiService;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.LoginResponse;
import com.heben.client.model.Models.RegisterRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private MaterialButton chipSeller, chipWholesaler, chipBuyer;
    private com.google.android.material.textfield.TextInputLayout tilName;
    private EditText etName, etPassword;
    private Button btnRegister;
    private TextView tvToLogin;

    private int selectedRole = 1; // 默认卖家

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        chipSeller = findViewById(R.id.chip_seller);
        chipWholesaler = findViewById(R.id.chip_wholesaler);
        chipBuyer = findViewById(R.id.chip_buyer);
        tilName = findViewById(R.id.til_name);
        etName = findViewById(R.id.et_name);
        etPassword = findViewById(R.id.et_password);
        btnRegister = findViewById(R.id.btn_register);
        tvToLogin = findViewById(R.id.tv_to_login);

        // 默认选中卖家
        selectRole(chipSeller);

        chipSeller.setOnClickListener(v -> selectRole(chipSeller));
        chipWholesaler.setOnClickListener(v -> selectRole(chipWholesaler));
        chipBuyer.setOnClickListener(v -> selectRole(chipBuyer));

        btnRegister.setOnClickListener(v -> register());

        tvToLogin.setOnClickListener(v -> finish());
    }

    private void selectRole(MaterialButton selected) {
        // 重置所有按钮样式
        resetChipStyle(chipSeller);
        resetChipStyle(chipWholesaler);
        resetChipStyle(chipBuyer);

        // 高亮选中的（MD3 Primary）
        int primaryColor = ContextCompat.getColor(this, R.color.md_primary);
        int onPrimaryColor = ContextCompat.getColor(this, R.color.md_on_primary);
        selected.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryColor));
        selected.setTextColor(onPrimaryColor);
        selected.setStrokeColor(android.content.res.ColorStateList.valueOf(primaryColor));

        selectedRole = Integer.parseInt(selected.getTag().toString());

        // 动态切换提示文字：卖家/批发商→店铺名称，买家→昵称
        if (selectedRole == 3) {
            tilName.setHint("昵称");
            etName.setHint("昵称");
        } else {
            tilName.setHint("店铺名称");
            etName.setHint("店铺名称");
        }
    }

    private void resetChipStyle(MaterialButton chip) {
        int surfaceColor = ContextCompat.getColor(this, R.color.md_surface);
        int onSurfaceVariant = ContextCompat.getColor(this, R.color.md_on_surface_variant);
        int outlineColor = ContextCompat.getColor(this, R.color.md_outline);
        chip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(surfaceColor));
        chip.setTextColor(onSurfaceVariant);
        chip.setStrokeColor(android.content.res.ColorStateList.valueOf(outlineColor));
    }

    private void register() {
        String name = etName.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, selectedRole == 3 ? "请输入昵称" : "请输入店铺名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 8 || password.length() > 12) {
            Toast.makeText(this, "密码长度为8-12位", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("注册中...");

        RegisterRequest request = new RegisterRequest();
        request.password = password;
        request.role = selectedRole;
        if (selectedRole == 3) {
            request.nickname = name;
        } else {
            request.shopName = name;
        }

        ApiService api = ApiClient.getService();
        api.register(request).enqueue(new Callback<ApiResponse<LoginResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LoginResponse>> call, Response<ApiResponse<LoginResponse>> response) {
                btnRegister.setEnabled(true);
                btnRegister.setText("注册");
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    LoginResponse data = response.body().getData();
                    String username = data != null && data.user != null ? data.user.username : "";
                    // 弹窗显示用户名
                    new androidx.appcompat.app.AlertDialog.Builder(RegisterActivity.this)
                            .setTitle("注册成功")
                            .setMessage("您的系统用户名为：\n\n" + username + "\n\n请牢记此用户名，用于后续登录。")
                            .setPositiveButton("知道了", (d, w) -> finish())
                            .setCancelable(false)
                            .show();
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "注册失败";
                    Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LoginResponse>> call, Throwable t) {
                btnRegister.setEnabled(true);
                btnRegister.setText("注册");
                Toast.makeText(RegisterActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
