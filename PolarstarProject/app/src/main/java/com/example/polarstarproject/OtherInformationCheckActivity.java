package com.example.polarstarproject;


import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.polarstarproject.Domain.Connect;
import com.example.polarstarproject.Domain.Disabled;
import com.example.polarstarproject.Domain.Guardian;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class OtherInformationCheckActivity  extends AppCompatActivity{
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference reference = database.getReference();
    private FirebaseAuth mAuth;
    private FirebaseUser user;

    private static final String TAG = "OtherInformationCheck";
    int classificationUserFlag = 0; //장애인 보호자 구별 (0: 기본값, 1: 장애인, 2: 보호자)
    String counterpartyUID = "";
    Connect myConnect;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        //UI 변수 선언

        classificationUser(user.getUid());
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
                        Toast.makeText(OtherInformationCheckActivity.this, "오류", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(OtherInformationCheckActivity.this, "오류", Toast.LENGTH_SHORT).show();
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
        if(classificationUserFlag == 1){ //내가 장애인일 경우 보호자 정보 띄움
            Query guardianQuery = reference.child("guardian").orderByKey().equalTo(counterpartyUID); //보호자 테이블 조회
            guardianQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Guardian guardian = new Guardian();
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        guardian = ds.getValue(Guardian.class);
                    }

                    if(guardian != null){
                        //화면에 상대방 정보 띄우기
                    }
                    else {
                        Toast.makeText(OtherInformationCheckActivity.this, "상대방 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        else if(classificationUserFlag == 2){ //내가 보호자일 경우 장애인 정보 띄움
            Query guardianQuery = reference.child("disabled").orderByKey().equalTo(counterpartyUID); //장애인 테이블 조회
            guardianQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Disabled disabled = new Disabled();
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        disabled = ds.getValue(Disabled.class);
                    }

                    if(disabled != null){
                        //화면에 상대방 정보 띄우기
                    }
                    else {
                        Toast.makeText(OtherInformationCheckActivity.this, "상대방 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }
}
