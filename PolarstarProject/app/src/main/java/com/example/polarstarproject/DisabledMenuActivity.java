package com.example.polarstarproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

//장애인 메뉴
public class DisabledMenuActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "DisabledMenuActivity";
    Button menuBtMyProfl, menuBtOthProfl, menuBtMapCk, menuBtSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_duser);

        menuBtMyProfl = (Button) findViewById(R.id.menuBtMyProfl); //내정보
        menuBtOthProfl = (Button) findViewById(R.id.menuBtOthProfl); //상대정보
        menuBtMapCk = (Button) findViewById(R.id.menuBtMapCk); //실시간 위치
        menuBtSettings = (Button) findViewById(R.id.menuBtSettings); //설정

        menuBtMyProfl.setOnClickListener(this);
        menuBtOthProfl.setOnClickListener(this);
        menuBtMapCk.setOnClickListener(this);
        menuBtSettings.setOnClickListener(this);
    }

    @Override
    protected void onResume(){ //Activity가 사용자와 상호작용하면
        super.onResume();

        RefactoringForegroundService.stopLocationService(this); //포그라운드 서비스 종료
    }

    @Override
    protected void onPause(){ //Activity가 잠시 멈추면
        super.onPause();

        Log.d(TAG, "멈춤");
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
            case R.id.menuBtMyProfl:
                Intent intentMyProfl = new Intent(DisabledMenuActivity.this, Myinfo_DuserActivity.class);
                startActivity(intentMyProfl);
                finish();
                break;

            case R.id.menuBtOthProfl:
                Intent intentOthProfl = new Intent(DisabledMenuActivity.this, OtherInformationGuardianCheckActivity.class);
                startActivity(intentOthProfl);
                finish();
                break;

            case R.id.menuBtMapCk:
                Intent intentMapCk = new Intent(DisabledMenuActivity.this, RealTimeLocationActivity.class);
                startActivity(intentMapCk);
                finish();
                break;

            case R.id.menuBtSettings:
                Intent intentSettings = new Intent(DisabledMenuActivity.this, MenuSettingActivity.class);
                startActivity(intentSettings);
                finish();
                break;
        }
    }
}
