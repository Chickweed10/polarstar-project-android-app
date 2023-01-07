package com.example.polarstarproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.polarstarproject.Domain.Clientage;
import com.example.polarstarproject.Domain.Guardian;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
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

public class FindActivity extends AppCompatActivity {
    Toolbar toolbar;

    EditText findPhoneNum, findPNCk;
    Button findPNReq, findPNReqCk, goLoginEmail;
    TextView tEmail;
    
    String Id = ((LoginActivity)LoginActivity.context_login).setId;
    String Password = ((LoginActivity)LoginActivity.context_login).setPassword;
    
    String findEmail = new String(); //찾은 이메일

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference reference = database.getReference();
    private FirebaseAuth mAuth;

    private static final String TAG = "Find";
    String VID = "";
    int certificationFlag = 0, verificationCodeFlag = 0; //인증 여부 판단, 이메일 중복 여부 판단 (0: 기본값, 1: 중복, 2: 통과), 인증번호 요청 예외 처리

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find);//회원가입 xml 파일 이름

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //뒤로가기
        getSupportActionBar().setTitle("이메일 찾기");

        mAuth = FirebaseAuth.getInstance();

        findPhoneNum = (EditText) findViewById(R.id.findPhoneNum); //전화번호
        findPNReq = (Button) findViewById(R.id.findPNReq); //전화번호 인증
        findPNCk = (EditText) findViewById(R.id.findPNCk); //인증번호 요청
        findPNReqCk = (Button) findViewById(R.id.findPNReqCk); //인증번호 확인
        tEmail = (TextView) findViewById(R.id.tEmail);
        goLoginEmail = (Button) findViewById(R.id.goLoginEmail); //확인

        findPNReq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                verificationCodeFlag = 1;
                sendVerificationCode();
            }
        });

        findPNReqCk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(findPNCk.getText().toString().isEmpty()){
                    Toast.makeText(FindActivity.this, "인증번호를 입력해주세요.",
                            Toast.LENGTH_SHORT).show();
                }
                else if(verificationCodeFlag == 0){
                    Toast.makeText(FindActivity.this, "인증요청을 해주세요.",
                            Toast.LENGTH_SHORT).show();
                }
                else if(verificationCodeFlag != 0 && !findPNCk.getText().toString().isEmpty()){
                    signInWithPhoneAuthCredential(PhoneAuthProvider.getCredential(VID, findPNCk.getText().toString()));
                }
            }
        });

        goLoginEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
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

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) { //인증번호 확인
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "인증 성공");
                            Toast.makeText(FindActivity.this, "인증 성공",
                                    Toast.LENGTH_SHORT).show();

                            //이메일 보이게함
                            String phoneNumber = findPhoneNum.getText().toString();

                            Query clientageQuery = reference.child("clientage").orderByChild("phoneNumber").equalTo(phoneNumber); //장애인 테이블 조회
                            clientageQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    Clientage clientage = new Clientage();
                                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                                        clientage = ds.getValue(Clientage.class);
                                    }
                                    if(clientage.getEmail() != null && !clientage.getEmail().isEmpty()){
                                        findEmail = clientage.getEmail();
                                        tEmail.setText(findEmail);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                            Query guardianQuery = reference.child("guardian").orderByChild("phoneNumber").equalTo(phoneNumber); //보호자 테이블 조회
                            guardianQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    Guardian guardian = new Guardian();
                                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                                        guardian = ds.getValue(Guardian.class);
                                    }
                                    if(guardian.getEmail() != null && !guardian.getEmail().isEmpty()){
                                        findEmail = guardian.getEmail();
                                        tEmail.setText(findEmail);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });

                            certificationFlag = 1;
                        } else {
                            Toast.makeText(FindActivity.this, "인증 실패",
                                    Toast.LENGTH_SHORT).show();
                            Log.w(TAG, "인증 실패", task.getException());
                        }
                    }
                });
    }

    private void sendVerificationCode(){ //인증번호 전송
        PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                Toast.makeText(FindActivity.this, "인증번호가 전송되었습니다. 60초 이내에 입력해주세요.",
                        Toast.LENGTH_SHORT).show();
                Log.d(TAG, "인증번호 전송 성공");
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Toast.makeText(FindActivity.this, "인증번호 전송 실패",
                        Toast.LENGTH_SHORT).show();
                Log.w(TAG, "인증번호 전송 실패", e);
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                Log.d(TAG, "onCodeSent:" + verificationId);

                VID = verificationId;
            }
        };

        String pn = findPhoneNum.getText().toString();
        if (TextUtils.isEmpty(pn)) { //전화번호 editText가 공란이면
            findPhoneNum.setError("전화번호를 입력해주세요.");
        } else {
            findPhoneNum.setError(null);
            
            // 데베에 있는 전화번호인지 확인이 필요
            if(pn.charAt(0) == '0'){
                pn = pn.substring(1);
            }

            PhoneAuthOptions options =
                    PhoneAuthOptions.newBuilder(mAuth)
                            .setPhoneNumber("+82"+ pn)       //핸드폰 번호
                            .setTimeout(60L, TimeUnit.SECONDS) //시간 제한
                            .setActivity(this)
                            .setCallbacks(mCallbacks)
                            .build();
            PhoneAuthProvider.verifyPhoneNumber(options);
            mAuth.setLanguageCode("kr");
        }

    }

}
