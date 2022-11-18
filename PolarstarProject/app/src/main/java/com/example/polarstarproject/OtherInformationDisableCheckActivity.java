package com.example.polarstarproject;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.polarstarproject.Domain.Connect;
import com.example.polarstarproject.Domain.Disabled;
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

public class OtherInformationDisableCheckActivity extends AppCompatActivity{ //장애인 정보 (본인이 보호자)
    Toolbar toolbar;

    ImageView othProflN;
    EditText othProflNameN, othProflPhoneNumN, othProflAddressN, othProflDetailAddN, othProflBirthN;
    RadioGroup othProflBtGenderN;
    RadioButton othProflBtGenderMN, othProflBtGenderFN;

    Spinner othProflDrDisGN;

    String sex, cSex, cDrDisG;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otherinfo_duser_n);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //뒤로가기
        getSupportActionBar().setTitle("상대 정보");

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        othProflN = (ImageView) findViewById(R.id.othProflN); //프로필 사진

        othProflNameN = (EditText) findViewById(R.id.othProflNameN); //이름
        othProflPhoneNumN = (EditText) findViewById(R.id.othProflPhoneNumN); //핸드폰번호
        othProflAddressN = (EditText) findViewById(R.id.othProflAddressN); //주소
        othProflDetailAddN = (EditText) findViewById(R.id.othProflDetailAddN); //상세 주소
        othProflBirthN = (EditText) findViewById(R.id.othProflBirthN); //생년월일

        othProflBtGenderN = findViewById(R.id.othProflBtGenderN); //성별
        othProflBtGenderMN = findViewById( R.id.othProflBtGenderMN);
        othProflBtGenderFN = findViewById( R.id.othProflBtGenderFN);

        othProflDrDisGN = (Spinner)findViewById(R.id.othProflDrDisGN); //장애등급

        storage = FirebaseStorage.getInstance(); //프로필 사진 가져오기
        storageRef = storage.getReference();

        classificationUser(user.getUid());
    }

    /////////////////////////////////////////액티비티 뒤로가기 설정////////////////////////////////////////
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home: { //toolbar의 back키를 눌렀을 때 동작
                skipScreen(); //사용자 구별 후 실시간 위치 화면으로 돌아감

                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed() { //뒤로가기 했을 때
        skipScreen(); //사용자 구별 후 실시간 위치 화면으로 돌아감
    }

    /////////////////////////////////////////화면 넘어가기////////////////////////////////////////
    public void skipScreen(){
        if(classificationUserFlag == 1){ //장애인
            Intent intent = new Intent(getApplicationContext(), DisabledRealTimeLocationActivity.class);
            startActivity(intent);
            finish();
        }
        else if(classificationUserFlag == 2){ //보호자
            Intent intent = new Intent(getApplicationContext(), GuardianRealTimeLocationActivity.class);
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
                        Toast.makeText(OtherInformationDisableCheckActivity.this, "오류", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "상대방 인적사항 확인 오류");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        else if(classificationUserFlag == 2) { //내가 보호자고, 상대방이 장애인일 경우
            Query query = reference.child("connect").child("disabled").orderByChild("myCode").equalTo(myConnect.getCounterpartyCode());
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
                        Toast.makeText(OtherInformationDisableCheckActivity.this, "오류", Toast.LENGTH_SHORT).show();
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
        Query guardianQuery = reference.child("disabled").orderByKey().equalTo(counterpartyUID); //장애인 테이블 조회
        guardianQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Disabled disabled = new Disabled();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    disabled = ds.getValue(Disabled.class);
                }

                if(disabled != null){
                    otherstorageRef = storageRef.child("profile").child(counterpartyUID);
                    if (otherstorageRef != null) {
                        otherstorageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                //이미지 로드 성공시
                                Glide.with(OtherInformationDisableCheckActivity.this).load(uri).into(othProflN);

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                //이미지 로드 실패시
                                Log.w(TAG, "프로필 사진 로드 실패");
                            }
                        });
                    }

                    othProflNameN.setText(disabled.getName());
                    othProflPhoneNumN.setText(disabled.getPhoneNumber());
                    othProflAddressN.setText(disabled.getAddress());
                    othProflDetailAddN.setText(disabled.getDetailAddress());
                    othProflBirthN.setText(disabled.getBirth());
                    cSex = disabled.getSex();

                    if(cSex.equals("여")) {
                        othProflBtGenderFN.setChecked(true);
                    }
                    else {
                        othProflBtGenderMN.setChecked(true);
                    }

                    cDrDisG = disabled.getDisabilityLevel();
                    if(cDrDisG.equals("경증")) {
                        othProflDrDisGN.setSelection(1);
                    }
                    else {
                        othProflDrDisGN.setSelection(0);
                    }
                }
                else {
                    Toast.makeText(OtherInformationDisableCheckActivity.this, "상대방 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}