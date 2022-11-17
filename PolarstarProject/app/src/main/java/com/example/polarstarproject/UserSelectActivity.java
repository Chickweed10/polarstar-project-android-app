package com.example.polarstarproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

//보호자, 장애인 선택
public class UserSelectActivity extends AppCompatActivity implements View.OnClickListener{
    Toolbar toolbar;
    
    Button btNonDisU, btDisU;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_select); //보호자, 장애인 선택 xml

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //뒤로가기
        getSupportActionBar().setTitle("사용자 선택");

        btNonDisU = (Button) findViewById(R.id.btNonDisU); //보호자 버튼
        btDisU = (Button) findViewById(R.id.btDisU); //장애인 버튼

        btNonDisU.setOnClickListener(this);
        btDisU.setOnClickListener(this);
    }

    /////////////////////////////////////////액티비티 뒤로가기 설정////////////////////////////////////////
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home: { //toolbar의 back키를 눌렀을 때 동작
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish(); //로그인 화면으로 이동

                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed() { //뒤로가기 했을 때
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
        finish(); //로그인 화면으로 이동
    }

    @Override
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
    }
}
