package com.payne.okux.view.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.payne.okux.R;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TvActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_ac).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AcActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_projector).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ProjectorActivity.class);
                startActivity(intent);
            }
        });
    }
}
