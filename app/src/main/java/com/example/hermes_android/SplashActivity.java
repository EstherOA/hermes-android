package com.example.hermes_android;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //check if connected
        //check if addresses are preloaded
        //fetch route
        //check if authenticated

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
//        Intent intent = new Intent(this, MainActivity.class);
//        startActivity(intent);
//        finish();
    }
}