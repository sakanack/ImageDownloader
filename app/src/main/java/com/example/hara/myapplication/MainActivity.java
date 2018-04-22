package com.example.hara.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends Activity implements OnClickListener {

    EditText editText;
    Button button1, button2;
    private FirebaseAnalytics mAnalytics;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button1 = (Button) findViewById(R.id.dlbutton);
        button1.setOnClickListener(MainActivity.this);
        button2 = (Button) findViewById(R.id.clearbutton);
        button2.setOnClickListener(MainActivity.this);
        editText = (EditText) findViewById(R.id.editText);

        //firebase analytics
        mAnalytics = FirebaseAnalytics.getInstance(this);

        //AdMob
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        //ファイルアクセス許諾
        if(ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //まだ許可されていないとき
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    protected void onStart(){
        super.onStart();
        if(Intent.ACTION_SEND.equals(getIntent().getAction())) {
            String uri = getIntent().getExtras().getCharSequence(Intent.EXTRA_TEXT).toString();
            editText.setText(uri);
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.dlbutton:
                String str = editText.getText().toString();
                new ImgDL(str, this).execute();
                break;
            case R.id.clearbutton:
                editText.setText(null);
                break;
        }
    }
}
