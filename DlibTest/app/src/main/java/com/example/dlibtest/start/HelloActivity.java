package com.example.dlibtest.start;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.dlibtest.start.LoginActivity;
import com.example.dlibtest.R;

public class HelloActivity extends AppCompatActivity {

    private final int SPLASH_DISPLAY_LENGHT = 2000; // 两秒后进入系统
    Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hello);

        new android.os.Handler().postDelayed(new Runnable() {
            public void run() {


                Intent Intent = new Intent(HelloActivity.this,
                        LoginActivity.class);
                startActivity(Intent);
                finish();
            }

        }, SPLASH_DISPLAY_LENGHT);

    }
}
