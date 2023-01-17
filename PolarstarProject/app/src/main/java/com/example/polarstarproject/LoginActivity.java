package com.example.polarstarproject;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
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

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity"; //로그용 태그

    public static Context context_login; // 다른 엑티비티에서의 접근을 위해 사용
    public SharedPreferences auto;
    public SharedPreferences.Editor autoLoginEdit;

    private PermissionSupport permission; // 권한설정 클래스 선언
    public String setId, setPassword;
    static final int PERMISSIONS_REQUEST = 0x0000001; //요청에 대한 결과값 확인을 위해 RequestCode를 final로 정의

    Button mLoginBtn, mRePasswordBtn, mReFindEmailBtn, ckSign;
    EditText mEmailText, mPasswordText;
    CheckBox autoCheck;
    private FirebaseAuth firebaseAuth;

    /////////////////////////////화면 넘어가기 용 변수////////////////////////////
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference reference = database.getReference();
    private FirebaseUser user;
    int classificationUserFlag = 0, connectCheckFlag = 0; //장애인 보호자 구별 (0: 기본값, 1: 장애인, 2: 보호자), 연결 여부 확인 (0: 연결안됨, 1: 연결됨)
    String email, pwd;
    public String checkBoxFlag = "f";//아이디 기억 체크박스 구별 (f: 기본값, 기억 안함 / t: 기억)


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        context_login = this;

        connectCheckFlag = 0;

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();

        //등록하기
        mLoginBtn = findViewById(R.id.lgnBt);
        mReFindEmailBtn = findViewById(R.id.findEmail);
        mRePasswordBtn = findViewById(R.id.rsPW);
        ckSign = findViewById(R.id.ckSign);

        mEmailText = findViewById(R.id.lgnEmail);
        mPasswordText = findViewById(R.id.lgnPW);
        //autoCheck = findViewById(R.id.lgnCbAuto);

        onCheckPermission(); //권한 메소드

        //autoCheck.setChecked(false); //체크박스 체크 표시 하도록 셋팅

        // SharedPreferences 사용해서 앱에 데이터 저장&불러오기
        auto = getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        autoLoginEdit = auto.edit();

        /*
        // 체크 표시했으면 로그인 데이터 불러오기
        checkBoxFlag = auto.getString("cFlag", null);
        if (checkBoxFlag == "t") {
            autoCheck.setChecked(true); //체크박스 체크 표시 하도록 셋팅
            setId = auto.getString("Id", null);
            //setPassword = auto.getString("Password", null);
            mEmailText.setText(setId);
            //mPasswordText.setText(setPassword);
            //}
        }


        if(setId!=null && setPassword!=null) {
            firebaseAuth.signInWithEmailAndPassword(setId, setPassword)
                    .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this,
                                        "로그인 성공",
                                        Toast.LENGTH_SHORT).show();
                                classificationUser(user.getUid()); //연결 여부 확인 후 화면 넘어가기
                            }
                        }
                    });

        }
         */

        //로그인 버튼이 눌리면
        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try { // 빈칸 로그인 예외처리
                    email = mEmailText.getText().toString().trim();
                    pwd = mPasswordText.getText().toString().trim();

                    firebaseAuth.signInWithEmailAndPassword(email, pwd)
                            .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(LoginActivity.this,
                                                "로그인 성공",
                                                Toast.LENGTH_SHORT).show();
                                        //로그인 정보 저장
                                        autoLoginEdit.putString("Id", mEmailText.getText().toString().trim());
                                        autoLoginEdit.putString("Password", mPasswordText.getText().toString().trim());
                                        autoLoginEdit.commit(); // commit 해야지만 저장됨
                                        /*
                                        if (autoCheck.isChecked()) {
                                            // 로그인 데이터 저장
                                            autoLoginEdit.putString("cFlag", "t");
                                            //checkBoxFlag = 1;
                                        }*/
                                        user = firebaseAuth.getCurrentUser();
                                        classificationUser(user.getUid()); //연결 여부 확인 후 화면 넘어가기

                                    } else {
                                        Toast.makeText(LoginActivity.this,
                                                "아이디 혹은 비밀번호를 잘못 입력하셨습니다.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }catch (IllegalArgumentException e){
                    Toast.makeText(LoginActivity.this,
                            "아이디 및 비밀번호를 입력해주세요.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        //이메일 찾기 버튼이 눌리면
        mReFindEmailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mEmailText.setText(setId); // 자동로그인 해놨을 때만 가능
                Intent intent = new Intent(getApplicationContext(), FindActivity.class);
                startActivity(intent);

            }
        });

        //재설정 버튼이 눌리면
        mRePasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mEmailText.getText().toString().trim();
                if(email.isEmpty()) {
                    Toast.makeText(LoginActivity.this,
                            "이메일을 입력해주세요.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    firebaseAuth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(LoginActivity.this,
                                                "비밀번호 재설정 이메일을 발송했습니다.",
                                                Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(LoginActivity.this,
                                                "가입한 이메일을 입력하세요.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });

        ckSign.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view){
                Intent intent = new Intent(getApplicationContext(), UserSelectActivity.class);
                startActivity(intent);
            }
        });

    }

    //권한 설정
    public void onCheckPermission() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean isPowersaveMode = powerManager.isPowerSaveMode();

        if ( //권한이 모두 있는 경우
            //위치 접근 권한
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
                        //카메라 접근 권한
                        && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        //저장소 접근 권한
                        && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        //절전모드
                        && isPowersaveMode == false) {

        } else { //하나라도 없는 경우
            Intent intent = new Intent(LoginActivity.this, PermissionActivity.class);
            intent.putExtra("skipIntent", 0);
            startActivity(intent);
            finish();
        }
    }
    /////////////////////////////////////////사용자 구별////////////////////////////////////////
    private void classificationUser(String uid){ //firebase select 조회 함수, 내 connect 테이블 조회
        Query clientageQuery = reference.child("connect").child("clientage").orderByKey().equalTo(uid); //장애인 테이블 조회
        clientageQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Connect myConnect = new Connect();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    myConnect = ds.getValue(Connect.class);
                }

                if(myConnect.getMyCode() != null && !myConnect.getMyCode().isEmpty()){
                    classificationUserFlag = 1;
                    reference.child("clientage").child(user.getUid()).child("password").setValue(pwd);
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
                    reference.child("guardian").child(user.getUid()).child("password").setValue(pwd);
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
            Query clientageQuery = reference.child("connect").child("clientage").orderByKey().equalTo(user.getUid()); //장애인 테이블 조회
            clientageQuery.addListenerForSingleValueEvent(new ValueEventListener() {
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
            Query guardianQuery = reference.child("connect").child("guardian").orderByKey().equalTo(user.getUid()); //보호자 테이블 조회
            guardianQuery.addListenerForSingleValueEvent(new ValueEventListener() {
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
        if(connectCheckFlag == 0){ //상대방과 연결되어있지 않은 경우
            Intent intent = new Intent(LoginActivity.this, ConnectActivity.class);
            startActivity(intent);
        }
        else { //이미 연결되어 있는 경우
            if(classificationUserFlag == 1 || classificationUserFlag == 2){
                //메인 화면으로 이동
                Intent intent = new Intent(LoginActivity.this, RealTimeLocationActivity.class);
                startActivity(intent);
                finish();
            }
            else {
                Log.w(TAG, "본인 확인 오류");
            }
        }
    }

}
