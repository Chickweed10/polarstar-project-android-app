package com.example.polarstarproject;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.polarstarproject.Domain.Connect;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PermissionActivity extends AppCompatActivity implements View.OnClickListener{
    Button rqBtAllPermission;

    private static final String TAG = "Permission"; //로그용 태그
    private int skipIntentFlag = 0;
    private boolean backgroundPermissionDeniedFlag = false;

    WarningDialog warningDialog;
    PermissionDialog permissionDialog; //다이얼로그
    private PermissionSupport permission; //권한 설정


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_support);

        Intent intent = getIntent();
        skipIntentFlag = intent.getExtras().getInt("skipIntent");

        permission =  new PermissionSupport(this, this);

        warningDialog = new WarningDialog(PermissionActivity.this, "");
        warningDialog.requestWindowFeature(Window.FEATURE_NO_TITLE); //타이틀 제거
        warningDialog.setContentView(R.layout.dialog_warning);

        //다이얼로그 밖 화면 흐리게
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        layoutParams.dimAmount = 0.8f;
        getWindow().setAttributes(layoutParams);

        rqBtAllPermission = (Button) findViewById(R.id.rqBtAllPermission);
        rqBtAllPermission.setOnClickListener(this);
    }

    //권한 체크
    private void permissionCheck(){
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean isPowersaveMode = powerManager.isPowerSaveMode();

        // sdk 23버전 이하 버전에서는 permission이 필요하지 않음
        if(Build.VERSION.SDK_INT >= 23){
            if(!lastPermission()){ //처음 요청하는 경우 그냥 권한 요청
                if(!permission.checkPermission()){ //권한 체크한 후에 리턴이 false일 경우 권한 요청을 해준다.
                    permission.requestPermission();
                }
            }

            if(isPowersaveMode) { //절전 모드면 배터리 설정 화면으로 전환
                Intent intent = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
                startActivity(intent);
            }

            else if(permission.checkPermission() && !isPowersaveMode){ //권한 있을 경우
                backgroundLocationPermission(); //백그라운드 위치 권한 체크
            }

        }
        else { //권한 있을 경우
            skipScreen(LoginActivity.class);
        }
    }
    
    //권한 요청
    private boolean lastPermission(){
        if(permission.checkLastPermission()){
            //이전에 거부한 경우 권한 필요성 설명 및 권한 요청
            permissionDialog = new PermissionDialog(PermissionActivity.this,
                    "북극성 앱을 사용하시려면 권한 설정이 필요합니다." + "\n" + "\n"
                            + "[확인] 버튼을 누른후 이동한 화면에서 [권한] 메뉴를 누르고 [위치, 사진 및 동영상, 카메라] 권한을 허용해 주세요.");
            permissionDialog.show(); // 다이얼로그 띄우기
            permissionDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); //모서리 둥글게

            //취소 버튼
            Button btnCancle = permissionDialog.findViewById(R.id.btn_cancle);
            btnCancle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // 원하는 기능 구현
                    permissionDialog.dismiss(); // 다이얼로그 닫기
                }
            });

            //확인 버튼
            Button btnOk = permissionDialog.findViewById(R.id.btn_ok);
            btnOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // 원하는 기능 구현
                    permissionDialog.dismiss(); // 다이얼로그 닫기
                    //앱 정보 화면으로 이동
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            });
            return true; //전에 거부했던 경우
        }
        return false;
    }
    
    //백그라운드 위치 권한
    public void backgroundLocationPermission(){
        //백그라운드 위치 권한 없을 경우
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED){
            permissionDialog = new PermissionDialog(PermissionActivity.this,
                    "북극성 앱을 사용하시려면 백그라운드 위치 권한이 필요합니다." + "\n" + "\n"
                            + "[위치]를 '항상 허용'으로 변경해주세요");
            permissionDialog.show(); // 다이얼로그 띄우기
            permissionDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); //모서리 둥글게

            //취소 버튼
            Button btnCancle = permissionDialog.findViewById(R.id.btn_cancle);
            btnCancle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // 원하는 기능 구현
                    permissionDialog.dismiss(); // 다이얼로그 닫기
                }
            });

            //확인 버튼
            Button btnOk = permissionDialog.findViewById(R.id.btn_ok);
            btnOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // 원하는 기능 구현
                    permissionDialog.dismiss(); // 다이얼로그 닫기
                    
                    //백그라운드 권한을 이미 거절한 경우
                    if(ActivityCompat.shouldShowRequestPermissionRationale(PermissionActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)){
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                        intent.addCategory(Intent.CATEGORY_DEFAULT);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                    else { //거절하지 않은 경우
                        ActivityCompat.requestPermissions(PermissionActivity.this, new String[]{android.Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 1);
                    }
                }
            });
        }
        else {
            skipScreen(LoginActivity.class);
        }
    }
    
    // Request Permission에 대한 결과 값을 받는다.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if(!permission.checkLastPermission()){ //전에 거부하지 않았던 경우
            // 리턴이 false일 경우 다시 권한 요청
            if (!permission.permissionResult(requestCode, permissions, grantResults)){ //권한 거부했을 경우
                warningDialog = new WarningDialog(PermissionActivity.this, "북극성 앱을 사용하시려면 권한 설정이 필요합니다.");
                warningDialog.show(); // 다이얼로그 띄우기
                warningDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); //모서리 둥글게

                //취소 버튼
                Button btnCancle = warningDialog.findViewById(R.id.btn_cancle);
                btnCancle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // 원하는 기능 구현
                        warningDialog.dismiss(); // 다이얼로그 닫기
                    }
                });

                //확인 버튼
                Button btnOk = warningDialog.findViewById(R.id.btn_ok);
                btnOk.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // 원하는 기능 구현
                        warningDialog.dismiss(); // 다이얼로그 닫기
                        permission.requestPermission(); // 다시 권한 요청
                    }
                });
            }
            else { //권한 있을 경우
                backgroundLocationPermission(); //백그라운드 위치 권한 체크 후 화면 넘어가기
            }
        }
    }

    private void skipScreen(Class skipClass){ //권한 있으면 화면 넘어가기
        if(skipIntentFlag != 0){
            if(skipIntentFlag == 1){ //연결 화면으로 넘어가기
                Intent intent = new Intent(PermissionActivity.this, ConnectActivity.class);
                startActivity(intent);
                finish();
            }
            else if(skipIntentFlag == 2){ //메인 화면으로 넘어가기
                Intent intent = new Intent(PermissionActivity.this, RealTimeLocationActivity.class);
                startActivity(intent);
                finish();
            }
        }
        else {
            Intent intent = new Intent(PermissionActivity.this, skipClass);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rqBtAllPermission: //권한 설정
                permissionCheck();
                break;
        }
    }
}
