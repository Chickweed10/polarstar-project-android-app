package com.example.polarstarproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class DisabledMenuActivity extends AppCompatActivity implements View.OnClickListener {
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

            /*case R.id.menuBtSettings:
                Intent intentSettings = new Intent(DisabledMenuActivity.this, MenuSettingActivity.class);
                startActivity(intentSettings);
                finish();
                break;*/
        }
    }
}
