package com.example.dlibtest.start;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.example.dlibtest.MainActivity;
import com.example.dlibtest.R;
import com.example.dlibtest.start.RegisterActivity;

import org.litepal.crud.DataSupport;

import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private EditText accountEdit;
    private EditText passwordEdit;
    private Button login;
    private Button register;
    private Context mContext;
    private boolean isStop = false;
    private CheckBox rememberPass;
    final String SP_KEY_USERNAME = "username";
    final String SP_KEY_PASSWORD = "password";
    final String SP_KEY_DATETIME = "datetime";

    final long DAY = 1 * 1000 * 60 * 60 * 24;
    List<UserPasspord> userPasswords = DataSupport.findAll(UserPasspord.class);
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        accountEdit = (EditText) findViewById(R.id.login_account);
        passwordEdit = (EditText) findViewById(R.id.login_password);
        rememberPass = (CheckBox) findViewById(R.id.remember_pass);
        login = (Button) findViewById(R.id.login);
        register = (Button) findViewById(R.id.register);
        mContext = this;

        SharedPreferences sp = getPreferences(MODE_PRIVATE);
        String userName = sp.getString(SP_KEY_USERNAME, "");
        String password = sp.getString(SP_KEY_PASSWORD, "");
        long ts = sp.getLong(SP_KEY_DATETIME, 0);

        if (System.currentTimeMillis() - ts <= DAY) {
            accountEdit.setText(userName);
            if (rememberPass.isChecked()) {
                passwordEdit.setText(password);
            }
        }

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                for (UserPasspord userPassword : userPasswords) {
                    if (accountEdit.getText().toString().equals(userPassword.getUser())) {
                        if (passwordEdit.getText().toString().equals(userPassword.getPassword())) {
                            if (rememberPass.isChecked()) {
                                SharedPreferences sp = getPreferences(MODE_PRIVATE);
                                SharedPreferences.Editor editor = sp.edit();
                                editor.putString(SP_KEY_USERNAME, accountEdit.getText().toString());
                                editor.putString(SP_KEY_PASSWORD, passwordEdit.getText().toString());
                                editor.putLong(SP_KEY_DATETIME, System.currentTimeMillis());
                                editor.apply();
                            }
                            isStop = true;
                            MainActivity.actionStart(LoginActivity.this, userPassword.getUser());
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "密码错误",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                }


            }
        });
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
    }





