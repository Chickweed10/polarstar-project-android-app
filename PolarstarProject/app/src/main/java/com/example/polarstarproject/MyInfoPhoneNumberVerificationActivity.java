package com.example.polarstarproject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.polarstarproject.Domain.Clientage;
import com.example.polarstarproject.Domain.Connect;
import com.example.polarstarproject.Domain.Guardian;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.TimeUnit;


public class MyInfoPhoneNumberVerificationActivity extends AppCompatActivity implements View.OnClickListener{
    EditText editPNReq, editPNReqEnt;
    Button editBtnPNReq, editBtPNOk;

    private static final String TAG = "MyInfoPhoneNumberVerification";

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference reference = database.getReference();
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    String defaultUserUID;

    Connect myConnect;
    public int classificationUserFlag = 0, verificationCodeFlag = 0, phoneNumberDuplicateCheckFlag = 0;
    //장애인 보호자 구별 (0: 기본값, 1: 장애인, 2: 보호자), 인증번호 요청 예외 처리, 전화번호 중복 여부 판단 (0: 기본값, 1: 중복)
    String VID = "";

    @SuppressLint("LongLogTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_myinfo_pnreq);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        defaultUserUID = user.getUid();

        editPNReq = (EditText) findViewById(R.id.editPNReq); //전화번호 입력
        editPNReqEnt = (EditText) findViewById(R.id.editPNReqEnt); //인증번호 입력
        editBtnPNReq = (Button) findViewById(R.id.editBtnPNReq); //인증번호 전송
        editBtPNOk = (Button) findViewById(R.id.editBtPNOk); //확인

        editPNReqEnt.setEnabled(false); //초기 인증번호란 비활성화

        editBtnPNReq.setOnClickListener(this);
        editBtPNOk.setOnClickListener(this);

        classificationUser(user.getUid()); //사용자 구별
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
        skipScreen(); //사용자 구별 후 내 정보 화면으로 돌아감
    }

    /////////////////////////////////////////화면 넘어가기////////////////////////////////////////
    public void skipScreen(){
        if(classificationUserFlag == 1){ //피보호자일 경우
            Intent intent = new Intent(getApplicationContext(), ClientageMyInfoActivity.class);
            startActivity(intent);
            finish();
        }
        else if(classificationUserFlag == 2){ //보호자일 경우
            Intent intent = new Intent(getApplicationContext(), GuardianMyInfoActivity.class);
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
                myConnect = new Connect();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    myConnect = ds.getValue(Connect.class);
                }

                if(myConnect.getMyCode() != null && !myConnect.getMyCode().isEmpty()) {
                    classificationUserFlag = 1;
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

                if(myConnect.getMyCode() != null && !myConnect.getMyCode().isEmpty()) {
                    classificationUserFlag = 2;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /////////////////////////////////////////전화번호////////////////////////////////////////
    private void phoneNumberDuplicateCheck(String phoneNumber){ //전화번호 공백 & 중복 검사
        phoneNumberDuplicateCheckFlag = 0; //전화번호 중복 flag 값 초기화

        if(phoneNumber == null || phoneNumber.isEmpty()){
            Toast.makeText(MyInfoPhoneNumberVerificationActivity.this, "전화번호를 입력해주세요.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        reference.child("clientage").orderByChild("phoneNumber").equalTo(phoneNumber). //장애인 user 검사
                addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {

                } else { //전화번호 중복시
                    phoneNumberDuplicateCheckFlag = 1; //전화번호 중복 flag 값 1로 변경
                    Toast.makeText(MyInfoPhoneNumberVerificationActivity.this, "중복된 전화번호입니다.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        reference.child("guardian").orderByChild("phoneNumber").equalTo(phoneNumber). //보호자 user 검사
                addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (phoneNumberDuplicateCheckFlag != 1 && !snapshot.exists()) { //장애인 user 전화번호 중복 검사 통과 & 보호자 user 전화번호 중복 검사 통과
                    sendVerificationCode(); //인증번호 보내기
                } else { //전화번호 중복시
                    phoneNumberDuplicateCheckFlag = 1; //전화번호 중복 flag 값 1로 변경
                    Toast.makeText(MyInfoPhoneNumberVerificationActivity.this, "중복된 전화번호입니다.",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendVerificationCode(){ //인증번호 전송
        PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @SuppressLint("LongLogTag")
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                editPNReq.setEnabled(false); //전화번호란 비활성화
                editPNReqEnt.setEnabled(true); //인증번호란 활성화
                Toast.makeText(MyInfoPhoneNumberVerificationActivity.this, "인증번호가 전송되었습니다." + '\n' + "60초 이내에 입력해주세요.",
                        Toast.LENGTH_LONG).show();
                Log.d(TAG, "인증번호 전송 성공");
            }

            @SuppressLint("LongLogTag")
            @Override
            public void onVerificationFailed(FirebaseException e) {
                Toast.makeText(MyInfoPhoneNumberVerificationActivity.this, "인증번호 전송 실패",
                        Toast.LENGTH_SHORT).show();
                Log.w(TAG, "인증번호 전송 실패", e);
            }

            @SuppressLint("LongLogTag")
            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                editPNReq.setEnabled(false); //전화번호란 비활성화
                editPNReqEnt.setEnabled(true); //인증번호란 활성화
                Toast.makeText(MyInfoPhoneNumberVerificationActivity.this, "인증번호가 전송되었습니다." + '\n' + "60초 이내에 입력해주세요.",
                        Toast.LENGTH_LONG).show();
                Log.d(TAG, "onCodeSent:" + verificationId);
                VID = verificationId;
            }
        };

        String pn = editPNReq.getText().toString(); //국가번호 변환
        if(pn.charAt(0) == '0'){ //앞자리 0으로 시작할 시
            pn = pn.substring(1); //앞자라 0 제외
        }

        mAuth.setLanguageCode("kr"); //인증문자 메시지 언어 한국어로
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber("+82"+ pn)       //핸드폰 번호
                        .setTimeout(60L, TimeUnit.SECONDS) //시간 제한
                        .setActivity(this)
                        .setCallbacks(mCallbacks)
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) { //인증번호 확인
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @SuppressLint("LongLogTag")
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) { //인증번호 일치
                            //firebase에 전화번호 변경
                            if(classificationUserFlag == 1){ //피보호자일 경우
                                try{
                                    reference.child("clientage").child(defaultUserUID).child("phoneNumber").setValue(editPNReq.getText().toString());
                                    Toast.makeText(MyInfoPhoneNumberVerificationActivity.this, "전화번호가 변경되었습니다.",
                                            Toast.LENGTH_SHORT).show();

                                    //firebase 인증 다시 전환
                                    authenticationTransition();
                                } catch (Exception e){
                                    Toast.makeText(MyInfoPhoneNumberVerificationActivity.this, "전화번호 변경 실패",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                            else if(classificationUserFlag == 2) { //보호자일 경우
                                try{
                                    reference.child("guardian").child(defaultUserUID).child("phoneNumber").setValue(editPNReq.getText().toString());
                                    Toast.makeText(MyInfoPhoneNumberVerificationActivity.this, "전화번호가 변경되었습니다.",
                                            Toast.LENGTH_SHORT).show();

                                    //firebase 인증 다시 전환
                                    authenticationTransition();
                                } catch (Exception e){
                                    Toast.makeText(MyInfoPhoneNumberVerificationActivity.this, "전화번호 변경 실패",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                        else { //인증번호 불일치
                            Toast.makeText(MyInfoPhoneNumberVerificationActivity.this, "인증번호가 일치하지 않습니다.",
                                    Toast.LENGTH_SHORT).show();
                            Log.w(TAG, "인증 실패", task.getException());
                        }
                    }
                });
    }

    public void authenticationTransition() { //firebase auth 전환하기 위한 함수
        if(classificationUserFlag == 1){
            reference.child("clientage").child(defaultUserUID).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Clientage clientage = snapshot.getValue(Clientage.class);
                    mAuth.signInWithEmailAndPassword(clientage.getEmail(), clientage.getPassword())
                            .addOnCompleteListener(MyInfoPhoneNumberVerificationActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        //화면 넘어가기
                                        skipScreen();
                                    }
                                }
                            });
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
        else if(classificationUserFlag == 2){
            reference.child("guardian").child(defaultUserUID).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Guardian guardian = snapshot.getValue(Guardian.class);
                    mAuth.signInWithEmailAndPassword(guardian.getEmail(), guardian.getPassword())
                            .addOnCompleteListener(MyInfoPhoneNumberVerificationActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        //화면 넘어가기
                                        skipScreen();
                                    }
                                }
                            });
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.editBtnPNReq: //인증번호 전송
                phoneNumberDuplicateCheck(editPNReq.getText().toString());
                verificationCodeFlag = 1;
                break;

            case R.id.editBtPNOk: //인증번호 확인
                if (editPNReqEnt.getText().toString().isEmpty()) { //공란인 경우
                    Toast.makeText(MyInfoPhoneNumberVerificationActivity.this, "인증번호를 입력해주세요.",
                            Toast.LENGTH_SHORT).show();
                } else if (verificationCodeFlag == 0) { //인증요청을 안한 경우
                    Toast.makeText(MyInfoPhoneNumberVerificationActivity.this, "인증요청을 해주세요.",
                            Toast.LENGTH_SHORT).show();
                } else if (verificationCodeFlag != 0 && !editPNReqEnt.getText().toString().isEmpty()) {
                    signInWithPhoneAuthCredential(PhoneAuthProvider.getCredential(VID, editPNReqEnt.getText().toString()));
                }

                break;
        }
    }
}
