package com.example.polarstarproject;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

        stopLocationService(); //백그라운드 서비스 종료
    }

    @Override
    protected void onPause(){ //Activity가 잠시 멈추면
        super.onPause();


        startLocationService(); //백그라운드 서비스 실행
    }

    @Override
    protected void onStop(){ //Activity가 사용자에게 보이지 않으면
        super.onStop();

        startLocationService(); //백그라운드 서비스 실행
    }

    /////////////////////////////////////////백그라운드 서비스////////////////////////////////////////
    private boolean isLocationServiceRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if (LocationService.class.getName().equals(service.service.getClassName())) {
                    if (service.foreground) {
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    private void startLocationService() { //서비스 실행
        if (!isLocationServiceRunning()) {
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            intent.setAction(Constants.ACTION_START_LOCATION_SERVICE);
            startService(intent);
            Toast.makeText(this, "Location service started", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationService() { //서비스 종료
        if (isLocationServiceRunning()) {
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            intent.setAction(Constants.ACTION_STOP_LOCATION_SERVICE);
            startService(intent);
            Toast.makeText(this, "Location service stopped", Toast.LENGTH_SHORT).show();
        }
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
