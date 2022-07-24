package com.example.polarstarproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class GuardianMenuActivity extends AppCompatActivity implements View.OnClickListener {
    Button menuBtMyProflN, menuBtOthProflN, menuBtMapCkN, menuBtLocationRecordN, menuBtProtectedAreaN, menuBtSettingsN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_duser);

        menuBtMyProflN = (Button) findViewById(R.id.menuBtMyProflN); //내정보
        menuBtOthProflN = (Button) findViewById(R.id.menuBtOthProflN); //상대정보
        menuBtMapCkN = (Button) findViewById(R.id.menuBtMapCkN); //실시간 위치
        menuBtLocationRecordN = (Button) findViewById(R.id.menuBtLocationRecordN); //위치기록
        menuBtProtectedAreaN = (Button) findViewById(R.id.menuBtProtectedAreaN); //보호구역
        menuBtSettingsN = (Button) findViewById(R.id.menuBtSettingsN); //설정

        menuBtMyProflN.setOnClickListener(this);
        menuBtOthProflN.setOnClickListener(this);
        menuBtMapCkN.setOnClickListener(this);
        menuBtLocationRecordN.setOnClickListener(this);
        menuBtProtectedAreaN.setOnClickListener(this);
        menuBtSettingsN.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.menuBtMyProfl:
                Intent intentMyProfl = new Intent(GuardianMenuActivity.this, Myinfo_DuserActivity.class);
                startActivity(intentMyProfl);
                finish();
                break;

            case R.id.menuBtOthProfl:
                Intent intentOthProfl = new Intent(GuardianMenuActivity.this, OtherInformationDisableCheckActivity.class);
                startActivity(intentOthProfl);
                finish();
                break;

            case R.id.menuBtMapCk:
                Intent intentMapCk = new Intent(GuardianMenuActivity.this, RealTimeLocationActivity.class);
                startActivity(intentMapCk);
                finish();
                break;

            case R.id.menuBtLocationRecordN:
                Intent intentLocationRecordN = new Intent(GuardianMenuActivity.this, RouteActivity.class);
                startActivity(intentLocationRecordN);
                finish();
                break;

            case R.id.menuBtProtectedAreaN:
                Intent intentProtectedAreaN = new Intent(GuardianMenuActivity.this, RangeSettingActivity.class);
                startActivity(intentProtectedAreaN);
                finish();
                break;

            /*case R.id.menuBtSettings:
                Intent intentSettings = new Intent(GuardianMenuActivity.this, MenuSettingActivity.class);
                startActivity(intentSettings);
                finish();
                break;*/
        }
    }
}
