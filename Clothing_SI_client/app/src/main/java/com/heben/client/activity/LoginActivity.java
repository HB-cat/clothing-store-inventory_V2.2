package com.heben.client.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.heben.client.R;
import com.heben.client.local.sync.NetworkUtil;
import com.heben.client.network.ApiClient;
import com.heben.client.network.ApiService;
import com.heben.client.model.ApiResponse;
import com.heben.client.model.Models.LoginRequest;
import com.heben.client.model.Models.LoginResponse;
import com.heben.client.util.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private CheckBox cbRememberPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);
        cbRememberPassword = findViewById(R.id.cb_remember_password);

        // 恢复记住密码状态
        SharedPreferences sp = getSharedPreferences("login_prefs", MODE_PRIVATE);
        cbRememberPassword.setChecked(sp.getBoolean("remember_password", false));

        // 如果已登录，直接跳转主页
        if (TokenManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        btnLogin.setOnClickListener(v -> login());
        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));

        // 记住密码状态变更时持久化
        cbRememberPassword.setOnCheckedChangeListener((buttonView, isChecked) ->
                sp.edit().putBoolean("remember_password", isChecked).apply());
    }

    private void login() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
            return;
        }

        // 离线登录：用本地缓存的密码验证
        if (!NetworkUtil.isConnected(this)) {
            if (TokenManager.canOfflineLogin() && TokenManager.checkPassword(username, password)) {
                // 本地验证通过，用缓存的token/信息走离线会话
                Toast.makeText(this, "离线模式登录成功（部分功能受限）", Toast.LENGTH_LONG).show();
                navigateToMain();
            } else {
                Toast.makeText(this, "离线登录失败：未记住密码或密码错误，请联网后重试", Toast.LENGTH_LONG).show();
            }
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("登录中...");

        ApiService api = ApiClient.getService();
        api.login(new LoginRequest(username, password)).enqueue(new Callback<ApiResponse<LoginResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LoginResponse>> call, Response<ApiResponse<LoginResponse>> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("登录");
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    LoginResponse data = response.body().getData();
                    if (data != null && data.user != null) {
                        TokenManager.saveLogin(
                                data.token,
                                String.valueOf(data.user.id),
                                data.user.username,
                                data.user.shopName,
                                data.user.role,
                                data.user.nickname
                        );
                        // 勾选了"记住密码"才缓存密码供离线登录
                        if (cbRememberPassword.isChecked()) {
                            TokenManager.savePassword(password);
                        } else {
                            TokenManager.clearPassword();
                        }
                        // 买家首次登录弹窗
                        if (data.user.role == 3) {
                            SharedPreferences sp = getSharedPreferences("login_prefs", MODE_PRIVATE);
                            if (!sp.getBoolean("first_login_prompted", false)) {
                                sp.edit().putBoolean("first_login_prompted", true).apply();
                                new androidx.appcompat.app.AlertDialog.Builder(LoginActivity.this)
                                        .setTitle("欢迎")
                                        .setMessage("建议您先完善个人信息（年龄、穿衣风格等），方便商家为您提供精准推荐。")
                                        .setPositiveButton("立即完善", (d, w) -> navigateToMain())
                                        .setNegativeButton("稍后", null)
                                        .show();
                                return;
                            }
                        }
                    }
                    Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "登录失败";
                    Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LoginResponse>> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("登录");
                Toast.makeText(LoginActivity.this, "网络错误，已切换到离线登录模式，请点击「登录」按钮重试", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToMain() {
        int role = TokenManager.getRole();
        Class<?> target = (role == 3) ? BuyerHomeActivity.class : MainActivity.class;
        Intent intent = new Intent(this, target);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
