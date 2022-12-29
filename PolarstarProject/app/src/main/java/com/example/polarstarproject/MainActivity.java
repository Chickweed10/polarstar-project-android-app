package com.example.polarstarproject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.polarstarproject.Domain.Connect;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity"; //로그용 태그

    public static Context context_main; // 다른 엑티비티에서의 접근을 위해 사용
    public SharedPreferences auto;
    public SharedPreferences.Editor autoLoginEdit;

    private PermissionSupport permission; // 권한설정 클래스 선언
    public String setId, setPassword;
    static final int PERMISSIONS_REQUEST = 0x0000001; //요청에 대한 결과값 확인을 위해 RequestCode를 final로 정의

    private FirebaseAuth firebaseAuth;

    /////////////////////////////화면 넘어가기 용 변수////////////////////////////
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference reference = database.getReference();
    private FirebaseUser user;
    int classificationUserFlag = 0, connectCheckFlag = 0; //장애인 보호자 구별 (0: 기본값, 1: 장애인, 2: 보호자), 연결 여부 확인 (0: 연결안됨, 1: 연결됨)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context_main = this;

        connectCheckFlag = 0;

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();

        // SharedPreferences 사용해서 앱에 데이터 저장&불러오기
        auto = getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        autoLoginEdit = auto.edit();

        // 로그인 데이터 불러오기
        setId = auto.getString("Id", null);
        setPassword = auto.getString("Password", null);

        if(setId!=null && setPassword!=null) {
            firebaseAuth.signInWithEmailAndPassword(setId, setPassword)
                    .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                /*Toast.makeText(MainActivity.this,
                                        "로그인 성공",
                                        Toast.LENGTH_SHORT).show(); */
                                user = firebaseAuth.getCurrentUser();
                                classificationUser(user.getUid()); //연결 여부 확인 후 화면 넘어가기
                            }
                        }
                    });

        }else {
            Log.w(TAG, "실행");
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }

    }

    /////////////////////////////////////////사용자 구별////////////////////////////////////////
    private void classificationUser(String uid){ //firebase select 조회 함수, 내 connect 테이블 조회
        Query disabledQuery = reference.child("connect").child("disabled").orderByKey().equalTo(uid); //장애인 테이블 조회
        disabledQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Connect myConnect = new Connect();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    myConnect = ds.getValue(Connect.class);
                }

                if(myConnect.getMyCode() != null && !myConnect.getMyCode().isEmpty()){
                    classificationUserFlag = 1;
                    connectCheck(user, classificationUserFlag); //연결 여부 확인
                }
                else {

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        Query guardianQuery = reference.child("connect").child("guardian").orderByKey().equalTo(uid); //보호자 테이블 조회
        guardianQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Connect myConnect = new Connect();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    myConnect = ds.getValue(Connect.class);
                }

                if(myConnect.getMyCode() != null && !myConnect.getMyCode().isEmpty()){
                    classificationUserFlag = 2;
                    connectCheck(user, classificationUserFlag); //연결 여부 확인
                }
                else {

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /////////////////////////////////////////연결 여부 확인////////////////////////////////////////
    private void connectCheck(FirebaseUser user, int classificationUserFlag){
        if(classificationUserFlag == 1) { //장애인 테이블 검사
            Query disabledQuery = reference.child("connect").child("disabled").orderByKey().equalTo(user.getUid()); //장애인 테이블 조회
            disabledQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Connect myConnect = new Connect();
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        myConnect = ds.getValue(Connect.class);
                    }

                    if(myConnect.getCounterpartyCode() != null && !myConnect.getCounterpartyCode().isEmpty()){
                        connectCheckFlag = 1;
                        skipScreen(connectCheckFlag); //화면 넘어가기
                    }
                    else {
                        skipScreen(connectCheckFlag); //화면 넘어가기
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        else if(classificationUserFlag == 2){ //보호자 테이블 검사
            Query disabledQuery = reference.child("connect").child("guardian").orderByKey().equalTo(user.getUid()); //보호자 테이블 조회
            disabledQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Connect myConnect = new Connect();
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        myConnect = ds.getValue(Connect.class);
                    }

                    if(myConnect.getCounterpartyCode() != null && !myConnect.getCounterpartyCode().isEmpty()){
                        connectCheckFlag = 1;
                        skipScreen(connectCheckFlag); //화면 넘어가기
                    }
                    else {
                        skipScreen(connectCheckFlag); //화면 넘어가기
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    public void skipScreen(int connectCheckFlag){
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean isPowersaveMode = powerManager.isPowerSaveMode();

        if ( //권한이 모두 있는 경우
            //위치 접근 권한
                ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
                        //카메라 접근 권한
                        && ActivityCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        //저장소 접근 권한
                        && ActivityCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this,android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        //절전모드
                        && isPowersaveMode == false)
        {
            if(connectCheckFlag == 0){ //상대방과 연결되어있지 않은 경우
                Intent intent = new Intent(MainActivity.this, ConnectActivity.class);
                startActivity(intent);
            }
            else { //이미 연결되어 있는 경우
                if(classificationUserFlag == 1 || classificationUserFlag == 2){
                    //메인 화면으로 이동
                    Intent intent = new Intent(MainActivity.this, RealTimeLocationActivity.class);
                    startActivity(intent);
                    finish();
                }
                else {
                    Log.w(TAG, "본인 확인 오류");
                }
            }
        }
        else { //권한 없는 경우
            if(connectCheckFlag == 0){ //연결 안 된 경우
                Intent intent = new Intent(MainActivity.this, PermissionActivity.class);
                intent.putExtra("skipIntent", 1);
                startActivity(intent);
                finish();
            }
            else { //이미 연결되어 있는 경우
                if(classificationUserFlag == 1 || classificationUserFlag == 2){
                    Intent intent = new Intent(MainActivity.this, PermissionActivity.class);
                    intent.putExtra("skipIntent", 2);
                    startActivity(intent);
                    finish();
                }
            }
        }
    }

}