package com.example.polarstarproject;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.polarstarproject.Domain.AddressGeocoding;
import com.example.polarstarproject.Domain.Connect;
import com.example.polarstarproject.Domain.Disabled;
import com.example.polarstarproject.Domain.InOutStatus;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class MenuSettingActivity extends AppCompatActivity {
    Toolbar toolbar;

    private static final String TAG = "MenuSetting";

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private FirebaseAuth firebaseAuth;
    private DatabaseReference reference = database.getReference();
    private FirebaseUser user;
    private String cUid;
    private String counterpartyUID;
    Connect myConnect;

    SharedPreferences autoM = ((MainActivity)MainActivity.context_main).auto;
    SharedPreferences.Editor autoEdit;
    Button userBtLinkDisConnect, btLogout, btWithdrawal;

    int classificationUserFlag = 0; //장애인 보호자 구별 (0: 기본값, 1: 장애인, 2: 보호자)

    String checkBoxFlag = "f";//아이디 기억 체크박스 구별 (f: 기본값, 기억 안함 / t: 기억)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_settings);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //뒤로가기
        getSupportActionBar().setTitle("설정");

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        cUid = user.getUid();

        userBtLinkDisConnect = findViewById(R.id.userBtLinkDisConnect);
        btLogout = findViewById(R.id.btLogout);
        btWithdrawal = findViewById(R.id.btWithdrawal);

        classificationUser(user.getUid()); //사용자 구별

        userBtLinkDisConnect.setOnClickListener(new View.OnClickListener() { //연결 끊기
            @Override
            public void onClick(View v) {
                foregroundService(1); //포그라운드 서비스 종료
            }
        });

        btLogout.setOnClickListener(new View.OnClickListener() { //로그아웃
            @Override
            public void onClick(View v) {
                foregroundService(0); //포그라운드 서비스 종료

                autoM = getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
                autoEdit = autoM.edit();

                autoEdit.clear();
                autoEdit.commit();

                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);

            }
        });

        btWithdrawal.setOnClickListener(new View.OnClickListener() { //회원 탈퇴
            @Override
            public void onClick(View v) {
                foregroundService(2); //포그라운드 서비스 종료
            }
        });
    }

    private void foregroundService(int num) {
        RefactoringForegroundService.stopLocationService(this); //포그라운드 서비스 종료

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(num == 1){ //연결 끊기
                    disConnectUser(cUid); //연결 끊기
                }
                else if(num == 2){ //회원 탈퇴
                    secessionUser(cUid); //회원 탈퇴
                }
            }
        },1000);
    }

    /////////////////////////////////////////사용자 구별////////////////////////////////////////
    private void classificationUser(String uid){ //firebase select 조회 함수, 내 connect 테이블 조회
        Query disabledQuery = reference.child("connect").child("disabled").orderByKey().equalTo(uid); //장애인 테이블 조회
        disabledQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                myConnect = new Connect();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    myConnect = ds.getValue(Connect.class);
                }

                if(myConnect.getMyCode() != null && !myConnect.getMyCode().isEmpty()){
                    classificationUserFlag = 1;
                    getOtherUID(user.getUid()); //상대방 UID 가져오기
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
                myConnect = new Connect();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    myConnect = ds.getValue(Connect.class);
                }

                if(myConnect.getMyCode() != null && !myConnect.getMyCode().isEmpty()){
                    classificationUserFlag = 2;
                    getOtherUID(user.getUid()); //상대방 UID 가져오기
                }
                else {

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /////////////////////////////////////////액티비티 뒤로가기 설정////////////////////////////////////////
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home: { //toolbar의 back키를 눌렀을 때 동작
                skipScreen(); //실시간 위치 화면으로 돌아감

                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed() { //뒤로가기 했을 때
        skipScreen(); //실시간 위치 화면으로 돌아감
    }

    public void skipScreen() {
        Intent intent = new Intent(MenuSettingActivity.this, RealTimeLocationActivity.class);
        startActivity(intent);
        finish();
    }

    /////////////////////////////////////////상대방 UID 가져오기////////////////////////////////////////
    private void getOtherUID(String uid){
        if(classificationUserFlag == 1) { //내가 피보호자고, 상대방이 보호자일 경우
            Query query = reference.child("connect").child("guardian").orderByChild("myCode").equalTo(myConnect.getCounterpartyCode());

            query.addListenerForSingleValueEvent(new ValueEventListener() { //보호자 코드로 보호자 uid 가져오기
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        counterpartyUID = ds.getKey();
                        Log.w(TAG, "counterpartyUID: "+counterpartyUID);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        else if(classificationUserFlag == 2) { //내가 보호자고, 상대방이 피보호자일 경우
            Query query = reference.child("connect").child("disabled").orderByChild("myCode").equalTo(myConnect.getCounterpartyCode());
            query.addListenerForSingleValueEvent(new ValueEventListener() { //피보호자 코드로 장애인 uid 가져오기
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        counterpartyUID = ds.getKey();
                        Log.w(TAG, "counterpartyUID: "+counterpartyUID);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        else { //올바르지 않은 사용자
            Log.w(TAG, "상대방 인적사항 확인 오류");
        }
    }

    /////////////////////////////////////////연결 해제////////////////////////////////////////
    private void disConnectUser(String uid){ //firebase select 조회 함수, 내 connect 테이블 조회
        if(classificationUserFlag == 1 ){ //내가 피보호자일 경우
            Query disabledQuery = reference.child("connect").child("disabled").orderByKey().equalTo(uid); //장애인 테이블 조회
            disabledQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    myConnect = new Connect();
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        myConnect = ds.getValue(Connect.class);
                    }

                    if(myConnect.getCounterpartyCode() == null || myConnect.getCounterpartyCode().isEmpty()){ //상대가 먼저 연결 해제했을 경우
                        Toast.makeText(getApplicationContext(),"이미 상대방과의 연결이 해제되었습니다." , Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getApplicationContext(), ConnectActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    else {
                        InOutStatus inOutStatus = new InOutStatus(false, false);
                        reference.child("inoutstatus").child(uid).setValue(inOutStatus); //복귀이탈 플래그 초기화
                        reference.child("range").child(counterpartyUID).setValue(null); //상대 보호자 보호구역 초기화
                        reference.child("connect").child("disabled").child(uid).child("counterpartyCode").setValue(null); //내 상대코드 초기화
                        reference.child("connect").child("guardian").child(counterpartyUID).child("counterpartyCode").setValue(null); //상대 상대코드 초기화
                        Intent intent = new Intent(getApplicationContext(), ConnectActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        else if(classificationUserFlag == 2){ //내가 보호자일 경우
            Query guardianQuery = reference.child("connect").child("guardian").orderByKey().equalTo(uid); //보호자 테이블 조회
            guardianQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    myConnect = new Connect();
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        myConnect = ds.getValue(Connect.class);
                    }

                    if(myConnect.getCounterpartyCode() == null || myConnect.getCounterpartyCode().isEmpty()){ //상대가 먼저 연결 해제했을 경우
                        Toast.makeText(getApplicationContext(),"이미 상대방과의 연결이 해제되었습니다." , Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getApplicationContext(), ConnectActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    else{
                        InOutStatus inOutStatus = new InOutStatus(false, false);
                        reference.child("inoutstatus").child(counterpartyUID).setValue(inOutStatus); //상대 피보호자 복귀이탈 플래그 초기화
                        reference.child("range").child(uid).setValue(null); //보호구역 초기화
                        reference.child("connect").child("guardian").child(uid).child("counterpartyCode").setValue(null); //내 상대코드 초기화
                        reference.child("connect").child("disabled").child(counterpartyUID).child("counterpartyCode").setValue(null); //상대 상대코드 초기화
                        Intent intent = new Intent(getApplicationContext(), ConnectActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    /////////////////////////////////////////회원 탈퇴////////////////////////////////////////
    private void secessionUser(String uid){
        secessionDisConnectUser(uid); //연결 끊기
    }

    /////////////////////////////////////////탈퇴 연결 해제////////////////////////////////////////
    private void secessionDisConnectUser(String uid){ //firebase select 조회 함수, 내 connect 테이블 조회
        if(classificationUserFlag == 1 ){ //내가 피보호자일 경우
            Query disabledQuery = reference.child("connect").child("disabled").orderByKey().equalTo(uid); //장애인 테이블 조회
            disabledQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(counterpartyUID != null){
                        reference.child("connect").child("guardian").child(counterpartyUID).child("counterpartyCode").setValue(null); //보호자 상대 코드 지우기
                    }
                    reference.child("connect").child("disabled").child(uid).removeValue(); //내 연결 코드 지우기
                    deleteUserDB(uid); //DB 삭제
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        else if(classificationUserFlag == 2){ //내가 보호자일 경우
            Query guardianQuery = reference.child("connect").child("guardian").orderByKey().equalTo(uid); //보호자 테이블 조회
            guardianQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(counterpartyUID != null){
                        reference.child("connect").child("disabled").child(counterpartyUID).child("counterpartyCode").setValue(null); //피보호자 상대 코드 지우기
                    }
                    reference.child("connect").child("guardian").child(cUid).removeValue(); //내 연결 코드 지우기
                    deleteUserDB(cUid); //DB 삭제
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    /////////////////////////////////////////탈퇴 DB 삭제////////////////////////////////////////
    private void deleteUserDB(String uid) {
        //firebase Authentication 삭제
        user.delete().addOnCompleteListener(new OnCompleteListener<Void>() { //firebase Authentication
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "User account deleted.");
                }
            }
        });

        //firebase Storage 삭제
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference myPro = storageRef.child("profile").child(uid);
        if (myPro != null) {
            myPro.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "firebase Storage delete");
                    }
                }
            });
        }

        //firebase Realtime Database 삭제
        reference.child("emailverified").child(uid).removeValue() //emailverified 삭제
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "firebase emailverified delete");
                        }
                    }
                });
        reference.child("realtimelocation").child(uid).removeValue() //realtimelocation 삭제
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "firebase realtimelocation delete");
                        }
                    }
                });

        if(classificationUserFlag == 1){ //피보호자
            reference.child("range").child(counterpartyUID).setValue(null) //상대 보호자 보호구역 삭제
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "firebase range null");
                            }
                        }
                    });
            reference.child("addressgeocoding").child(uid).removeValue() //addressgeocoding 삭제
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "firebase addressgeocoding delete");
                            }
                        }
                    });
            reference.child("departurearrivalstatus").child(uid).removeValue() //departurearrivalstatus 삭제
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "firebase departurearrivalstatus delete");
                            }
                        }
                    });
            reference.child("inoutstatus").child(uid).removeValue() //inoutstatus 삭제
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "firebase inoutstatus delete");
                            }
                        }
                    });
            reference.child("route").child(uid).removeValue() //route 삭제
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "firebase route delete");
                            }
                        }
                    });
            reference.child("trackingstatus").child(uid).removeValue() //trackingstatus 삭제
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "firebase trackingstatus delete");
                            }
                        }
                    });
            reference.child("disabled").child(uid).removeValue() //disabled 삭제
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "firebase disabled delete");
                            }
                        }
                    });
        }

        else if(classificationUserFlag == 2){ //보호자
            reference.child("range").child(uid).removeValue() //range 삭제
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "firebase range delete");
                            }
                        }
                    });
            reference.child("guardian").child(uid).removeValue() //guardian 삭제
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "firebase guardian delete");
                            }
                        }
                    });
        }

        //로그인 화면으로 이동
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
        finish();
    }
}