package com.payne.okux.view.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.payne.okux.R;
import com.payne.okux.view.brand.BrandListActivity;

public class TvActivity extends Activity {

    private static final int SELECT_BRAND_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tv);

        findViewById(R.id.btn_select_brand).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TvActivity.this, BrandListActivity.class);
                startActivityForResult(intent, SELECT_BRAND_REQUEST);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_BRAND_REQUEST && resultCode == RESULT_OK) {
            String brandName = data.getStringExtra("brand_name");
            Intent remoteIntent = new Intent(TvActivity.this, RemoteActivity.class);
            remoteIntent.putExtra("brand_name", brandName);
            startActivity(remoteIntent);
        }
    }
}
