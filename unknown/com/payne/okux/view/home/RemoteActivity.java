package com.payne.okux.view.home;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.payne.okux.R;

public class RemoteActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);

        String brandName = getIntent().getStringExtra("brand_name");

        TextView tvBrand = findViewById(R.id.tv_brand);
        tvBrand.setText("Brand: " + brandName);

        TextView tvHexCode = findViewById(R.id.tv_hex_code);
        tvHexCode.setText("HEX Code: 0x123456");
    }
}
