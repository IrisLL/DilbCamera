package com.example.dlibtest.start;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.dlibtest.R;

import org.litepal.crud.DataSupport;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {

   /* @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
    }*/
    private String password;
    private String account;
    private String rePassword;
    private Bitmap bm = null;


    // 请求码
    private static final int IMAGE_REQUEST_CODE = 0;
    private static final int CAMERA_REQUEST_CODE = 2;
    private static final int RESULT_REQUEST_CODE = 3;

    private int test = 0;

    private EditText registerAccount;
    private EditText registerPassword;
    private EditText registerRePassword;
    private Context mContext;

    List<UserPasspord> userPasswords = DataSupport.findAll(UserPasspord.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Button register = (Button) findViewById(R.id.register);
        Button back = (Button) findViewById(R.id.register_back);
        registerPassword = (EditText) findViewById(R.id.register_password);
        registerAccount = (EditText) findViewById(R.id.register_account);
        registerRePassword = (EditText) findViewById(R.id.register_rewrite_password);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                account = registerAccount.getText().toString();
                password = registerPassword.getText().toString();
                rePassword = registerRePassword.getText().toString();

                test = 0;
                for (UserPasspord userPassword : userPasswords) {
                    if (account.equals(userPassword.getUser())) {
                        Toast.makeText(RegisterActivity.this, "账户重复", Toast.LENGTH_SHORT).show();
                        test = 1;
                    }
                }
                if (!password.equals(rePassword)) {
                    Toast.makeText(RegisterActivity.this, "两次输入的密码不相同", Toast.LENGTH_SHORT).show();
                    test = 2;
                }
                if (test == 0) {
                    UserPasspord userPassword = new UserPasspord();
                    userPassword.setUser(account);
                    userPassword.setPassword(password);
                    ;
                    userPassword.save();
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });



    }
}
