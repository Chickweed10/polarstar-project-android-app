package com.example.polarstarproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

//보호자 메뉴
public class GuardianMenuActivity extends AppCompatActivity implements View.OnClickListener {
    Button menuBtMyProflN, menuBtOthProflN, menuBtMapCkN, menuBtLocationRecordN, menuBtProtectedAreaN, menuBtSettingsN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_duser_n);

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
    protected void onResume(){ //Activity가 사용자와 상호작용하면
        super.onResume();

        RefactoringForegroundService.stopLocationService(this); //포그라운드 서비스 종료
    }

    @Override
    protected void onPause(){ //Activity가 잠시 멈추면
        super.onPause();

        RefactoringForegroundService.startLocationService(this); //포그라운드 서비스 실행
    }

    @Override
    protected void onStop(){ //Activity가 사용자에게 보이지 않으면
        super.onStop();

        RefactoringForegroundService.startLocationService(this); //포그라운드 서비스 실행
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.menuBtMyProflN:
                Intent intentMyProfl = new Intent(GuardianMenuActivity.this, Myinfo_Duser_nActivity.class);
                startActivity(intentMyProfl);
                finish();
                break;

            case R.id.menuBtOthProflN:
                Intent intentOthProfl = new Intent(GuardianMenuActivity.this, OtherInformationDisableCheckActivity.class);
                startActivity(intentOthProfl);
                finish();
                break;

            case R.id.menuBtMapCkN:
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

            case R.id.menuBtSettingsN:
                Intent intentSettings = new Intent(GuardianMenuActivity.this, MenuSettingActivity.class);
                startActivity(intentSettings);
                finish();
                break;
        }
    }
}
