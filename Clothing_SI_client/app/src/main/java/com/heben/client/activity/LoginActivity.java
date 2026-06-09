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
import com.heben.client.network.ApiClient;
import com.heben.client.network.ApiService;
import com.heben.client.util.SoundManager;
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

        SharedPreferences sp = getSharedPreferences("login_prefs", MODE_PRIVATE);
        cbRememberPassword.setChecked(sp.getBoolean("remember_password", false));

        if (TokenManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        btnLogin.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            login();
        });
        tvRegister.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            startActivity(new Intent(this, RegisterActivity.class));
        });

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

        doApiLogin(username, password);
    }

    /** 执行 API 登录，失败时回退到离线登录 */
    private void doApiLogin(String username, String password) {
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
                        savePasswordIfChecked(password);
                        if (data.user.role == 3) showBuyerWelcomeDialog();
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
                // API 不可达 → 回退到离线登录
                doOfflineLogin(username, password);
            }
        });
    }

    /** 执行离线登录（服务器不可达时调用） */
    private void doOfflineLogin(String username, String password) {
        if (TokenManager.canOfflineLogin() && TokenManager.checkPassword(username, password)) {
            Toast.makeText(this, "🔴 服务器离线，已使用本地验证登录（功能受限）", Toast.LENGTH_LONG).show();
            navigateToMain();
        } else {
            Toast.makeText(this, "⚠️ 服务器连不上，且本地无缓存的密码。\n请检查网络，或先联网登录并勾选「记住密码」", Toast.LENGTH_LONG).show();
        }
    }

    private void savePasswordIfChecked(String password) {
        if (cbRememberPassword.isChecked()) {
            TokenManager.savePassword(password);
        } else {
            TokenManager.clearPassword();
        }
    }

    private void showBuyerWelcomeDialog() {
        SharedPreferences sp = getSharedPreferences("login_prefs", MODE_PRIVATE);
        if (!sp.getBoolean("first_login_prompted", false)) {
            sp.edit().putBoolean("first_login_prompted", true).apply();
            new androidx.appcompat.app.AlertDialog.Builder(LoginActivity.this)
                    .setTitle("欢迎")
                    .setMessage("建议您先完善个人信息（年龄、穿衣风格等），方便商家为您提供精准推荐。")
                    .setPositiveButton("立即完善", (d, w) -> navigateToMain())
                    .setNegativeButton("稍后", null)
                    .show();
        }
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
