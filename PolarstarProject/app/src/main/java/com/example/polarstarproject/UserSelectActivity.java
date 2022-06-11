package com.example.polarstarproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class UserSelectActivity extends AppCompatActivity{
    //Button btNonDisU, btDisU;

    /*@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.); //보호자, 장애인 선택 xml

        btNonDisU = (Button) findViewById(R.id.btNonDisU); //보호자 버튼
        btDisU = (Button) findViewById(R.id.btDisU); //장애인 버튼

        btNonDisU.setOnClickListener(this);
        btDisU.setOnClickListener(this);
    }*/
    /*@Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btNonDisU: //보호자 버튼 클릭 시
                Intent GuardianIntent = new Intent(this, GuardianRegisterActivity.class);
                startActivity(GuardianIntent);
                break;
            case R.id.btDisU: //장애인 버튼 클릭 시
                Intent DisabledIntent = new Intent(this, DisabledRegisterActivity.class);
                startActivity(DisabledIntent);
        }
    }*/
}
