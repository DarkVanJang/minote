package net.micode.notes.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import net.micode.notes.R;

public class PasswordView extends AppCompatActivity {

    private static final String ACTION_SET_PASSWORD = "SET_PASSWORD";
    private static final String ACTION_VERIFY_PASSWORD = "VERIFY_PASSWORD";

    private EditText etPassword;
    private TextView tvTitle;
    private Button btnSave;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_encryption);

        // 初始化视图
        etPassword = findViewById(R.id.et_password);
        tvTitle = findViewById(R.id.tv_title);
        btnSave = findViewById(R.id.btnSave);

        // 获取传入的操作类型
        String actionType = getIntent().getStringExtra("ACTION_TYPE");

        // 根据操作类型调整界面
        if (ACTION_SET_PASSWORD.equals(actionType)) {
            setupSetPasswordUI();
        } else if (ACTION_VERIFY_PASSWORD.equals(actionType)) {
            setupVerifyPasswordUI();
        }
    }

    /**
     * 设置密码界面
     */
    private void setupSetPasswordUI() {
        setTitle("设置密码");
        tvTitle.setText("请输入新密码（至少3位）");
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newPassword = etPassword.getText().toString();
                if (newPassword.length() >= 3) { // 检查密码长度
                    Intent replyIntent = new Intent();
                    replyIntent.putExtra("NEW_PASSWORD", newPassword);
                    setResult(RESULT_OK, replyIntent);
                    finish(); // 返回到上一个 Activity
                } else {
                    Toast.makeText(PasswordView.this, "密码必须至少包含3个字符", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * 验证密码界面
     */
    private void setupVerifyPasswordUI() {
        setTitle("输入密码");
        tvTitle.setText("请输入密码以解锁");

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputPassword = etPassword.getText().toString();
                String correctPassword = getIntent().getStringExtra("CURRENT_PASSWORD"); // 获取正确的密码

                if (inputPassword.equals(correctPassword)) {
                    setResult(RESULT_OK); // 验证成功
                    Toast.makeText(PasswordView.this, "密码正确", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(PasswordView.this, "密码错误，请重试", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}