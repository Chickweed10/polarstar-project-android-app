package com.example.polarstarproject;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class PermissionActivity extends AppCompatActivity implements View.OnClickListener{
    Button rqBtAllPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_support);

        rqBtAllPermission = (Button) findViewById(R.id.rqBtAllPermission);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rqBtAllPermission: //권한 설정
                
                break;
        }
    }
}
