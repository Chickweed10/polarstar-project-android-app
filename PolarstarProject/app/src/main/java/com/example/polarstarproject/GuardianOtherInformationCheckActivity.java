package com.example.polarstarproject;


import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.polarstarproject.Domain.Connect;
import com.example.polarstarproject.Domain.Guardian;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

import java.util.Timer;
import java.util.TimerTask;

public class GuardianOtherInformationCheckActivity extends AppCompatActivity implements View.OnClickListener{ //보호자 정보 (본인이 장애인)
    Toolbar toolbar;

    ImageView othProfl;
    TextView othProflName, othProflPhoneNum, othProflAddress, othProflDetailAdd, othProflBirth;
    RadioGroup othProflBtGender;
    RadioButton othProflBtGenderM, othProflBtGenderF;
    String sex,  cSex;
    Button othProflBtEdit;

    private DisconnectDialog disconnectDialog; //연결끊기 다이얼로그 팝업

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference reference = database.getReference();
    private FirebaseAuth mAuth;
    private FirebaseUser user;

    private static final String TAG = "OtherInformationCheck";
    int classificationUserFlag = 0; //장애인 보호자 구별 (0: 기본값, 1: 장애인, 2: 보호자)
    String counterpartyUID = "";
    Connect myConnect;

    FirebaseStorage storage;
    StorageReference storageRef;
    StorageReference otherstorageRef;

    Timer timer; //상대방과 매칭 검사를 위한 타이머
    TimerTask timerTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otherinfo_duser);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //뒤로가기
        getSupportActionBar().setTitle("상대 정보");

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        disconnectDialog = new DisconnectDialog(this);
        disconnectDialog.requestWindowFeature(Window.FEATURE_NO_TITLE); //타이틀 제거

        othProfl = (ImageView) findViewById(R.id.othProfl); //프로필 사진

        othProflName = (TextView) findViewById(R.id.othProflName); //이름
        othProflPhoneNum = (TextView) findViewById(R.id.othProflPhoneNum); //핸드폰번호
        othProflAddress = (TextView) findViewById(R.id.othProflAddress); //주소
        othProflDetailAdd = (TextView) findViewById(R.id.othProflDetailAdd); //상세 주소
        othProflBirth = (TextView) findViewById(R.id.othProflBirth); //생년월일

        othProflBtGender = findViewById(R.id.othProflBtGender); //성별
        othProflBtGenderM = findViewById( R.id.othProflBtGenderM);
        othProflBtGenderF = findViewById( R.id.othProflBtGenderF);

        othProflBtEdit = (Button) findViewById( R.id.othProflBtEdit); //확인 버튼

        othProflBtEdit.setOnClickListener(this);

        storage = FirebaseStorage.getInstance(); //프로필 사진 가져오기
        storageRef = storage.getReference();

        skipScreen();
    }

    /////////////////////////////////////////액티비티 뒤로가기 설정////////////////////////////////////////
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home: { //toolbar의 back키를 눌렀을 때 동작
                //메인 화면으로 돌아감
                Intent intent = new Intent(getApplicationContext(), RealTimeLocationActivity.class);
                startActivity(intent);
                finish();
                timer.cancel();
                timerTask.cancel(); //타이머 종료

                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed() { //뒤로가기 했을 때
        //메인 화면으로 돌아감
        Intent intent = new Intent(getApplicationContext(), RealTimeLocationActivity.class);
        startActivity(intent);
        finish();
        timer.cancel();
        timerTask.cancel(); //타이머 종료
    }

    /////////////////////////////////////////연결 체크////////////////////////////////////////
    private void startDisconnectDialog(){
        RefactoringForegroundService.stopLocationService(this); //포그라운드 서비스 종료
        disconnectDialog = new DisconnectDialog(this);
        disconnectDialog.setCancelable(false);
        disconnectDialog.show();
        disconnectDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); //모서리 둥글게
    }

    /////////////////////////////////////////연결 여부 확인 후 화면 넘어가기////////////////////////////////////////
    private void skipScreen(){
        timer = new Timer();

        timerTask = new TimerTask() {
            @Override
            public void run() {
                //3초마다 실행
                connectionCheck(); //상대방과 매칭 여부 확인
                Log.w(TAG, "돌아감");
            }
        };
        timer.schedule(timerTask,0,3000);

        classificationUser(user.getUid()); //사용자 구별
    }

    /////////////////////////////////////////연결 여부 확인////////////////////////////////////////
    private void connectionCheck(){ //firebase select 조회 함수, 내 connect 테이블 조회
        Query clientageQuery = reference.child("connect").child("clientage").orderByKey().equalTo(user.getUid()); //장애인 테이블 조회
        clientageQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Connect myConnect = new Connect();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    myConnect = ds.getValue(Connect.class);
                }

                if(myConnect.getMyCode() != null && !myConnect.getMyCode().isEmpty()){
                    if(myConnect.getCounterpartyCode() == null){ //상대방이 연결 끊었을 경우
                        if(! GuardianOtherInformationCheckActivity.this.isFinishing()){ //finish 오류 방지
                            startDisconnectDialog();
                            timer.cancel();
                            timerTask.cancel(); //타이머 종료
                        }
                        Log.w(TAG, "상대 보호자 없음");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /////////////////////////////////////////사용자 구별////////////////////////////////////////
    private void classificationUser(String uid){ //firebase select 조회 함수, 내 connect 테이블 조회
        Query clientageQuery = reference.child("connect").child("clientage").orderByKey().equalTo(uid); //장애인 테이블 조회
        clientageQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                myConnect = new Connect();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    myConnect = ds.getValue(Connect.class);
                }

                if(myConnect.getMyCode() != null && !myConnect.getMyCode().isEmpty()){
                    classificationUserFlag = 1;
                    getOtherUID();
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
                    getOtherUID();
                }
                else {
                    Log.w(TAG, "본인 확인 오류");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /////////////////////////////////////////상대방 UID 가져오기////////////////////////////////////////
    private void getOtherUID(){
        if(classificationUserFlag == 1) { //내가 장애인이고, 상대방이 보호자일 경우
            Query query = reference.child("connect").child("guardian").orderByChild("myCode").equalTo(myConnect.getCounterpartyCode());
            query.addListenerForSingleValueEvent(new ValueEventListener() { //보호자 코드로 보호자 uid 가져오기
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        counterpartyUID = ds.getKey();
                    }

                    if(counterpartyUID != null && !counterpartyUID.isEmpty()){
                        otherInformationCheck(); //상대방 정보 가져오기
                    }
                    else {
                        Toast.makeText(GuardianOtherInformationCheckActivity.this, "오류", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "상대방 인적사항 확인 오류");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        else if(classificationUserFlag == 2) { //내가 보호자고, 상대방이 장애인일 경우
            Query query = reference.child("connect").child("clientage").orderByChild("myCode").equalTo(myConnect.getCounterpartyCode());
            query.addListenerForSingleValueEvent(new ValueEventListener() { //장애인 코드로 장애인 uid 가져오기
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        counterpartyUID = ds.getKey();
                    }

                    if(counterpartyUID != null && !counterpartyUID.isEmpty()){
                        otherInformationCheck(); //상대방 정보 가져오기
                    }
                    else {
                        Toast.makeText(GuardianOtherInformationCheckActivity.this, "오류", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "상대방 인적사항 확인 오류");
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

    /////////////////////////////////////////상대방 정보 가져오기////////////////////////////////////////
    private void otherInformationCheck(){
        Query guardianQuery = reference.child("guardian").orderByKey().equalTo(counterpartyUID); //보호자 테이블 조회
        guardianQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Guardian guardian = new Guardian();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    guardian = ds.getValue(Guardian.class);
                }

                if(guardian != null){
                    otherstorageRef = storageRef.child("profile").child(counterpartyUID);
                    if (otherstorageRef != null) {
                        otherstorageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                //이미지 로드 성공시
                                Glide.with(GuardianOtherInformationCheckActivity.this).load(uri).into(othProfl);

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                //이미지 로드 실패시
                                Log.w(TAG, "프로필 사진 로드 실패");
                            }
                        });
                    }

                    othProflName.setText(guardian.getName());
                    othProflPhoneNum.setText(guardian.getPhoneNumber());
                    othProflAddress.setText(guardian.getAddress());
                    othProflDetailAdd.setText(guardian.getDetailAddress());
                    othProflBirth.setText(guardian.getBirth());
                    cSex = guardian.getSex();

                    if(cSex.equals("여")) {
                        othProflBtGenderF.setChecked(true);
                        othProflBtGenderM.setEnabled(false);
                    }
                    else {
                        othProflBtGenderM.setChecked(true);
                        othProflBtGenderF.setEnabled(false);
                    }

                }
                else {
                    Toast.makeText(GuardianOtherInformationCheckActivity.this, "상대방 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.othProflBtEdit: //확인 버튼 클릭 시
                //메인 화면으로 돌아감
                Intent intent = new Intent(getApplicationContext(), RealTimeLocationActivity.class);
                startActivity(intent);
                finish();
                timer.cancel();
                timerTask.cancel(); //타이머 종료

                break;
        }
    }
}