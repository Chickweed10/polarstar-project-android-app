package com.example.polarstarproject;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

public class ConnectActivity extends AppCompatActivity implements View.OnClickListener{
    TextView tvMyCode;
    EditText editOtherCode;
    Button btnConnect, btnCopy;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference reference = database.getReference();
    private FirebaseAuth mAuth;
    private FirebaseUser user;

    private static final String TAG = "Connect";
    int classificationUserFlag = 0; //장애인 보호자 구별 (0: 기본값, 1: 장애인, 2: 보호자)
    String findMyCode = null; //내 코드값

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        tvMyCode = (TextView) findViewById(R.id.tvMyCode);
        editOtherCode = (EditText) findViewById(R.id.editOtherCode);
        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnCopy = (Button) findViewById(R.id.btnCopy);

        btnConnect.setOnClickListener(this);
        btnCopy.setOnClickListener(this);

        classificationUser(user.getUid());
    }

    /////////////////////////////////////////사용자 구별, 내 코드값 가져오기////////////////////////////////////////
    private void classificationUser(String uid){ //firebase select 조회 함수, 내 코드값 저장
        Query disabledQuery = reference.child("connect").child("disabled").orderByKey().equalTo(uid);
        disabledQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Connect connectUser = new Connect();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    connectUser = ds.getValue(Connect.class);
                }

                if(connectUser.getMyCode() != null){
                    classificationUserFlag = 1;
                    findMyCode = connectUser.getMyCode();
                    tvMyCode.setText(findMyCode); //내 코드 화면에 뿌려주기
                }
                else {

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        Query guardianQuery = reference.child("connect").child("guardian").orderByKey().equalTo(uid);
        guardianQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Connect connectUser = new Connect();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    connectUser = ds.getValue(Connect.class);
                }

                if(connectUser.getMyCode() != null){
                    classificationUserFlag = 2;
                    findMyCode = connectUser.getMyCode();
                    tvMyCode.setText(findMyCode); //내 코드 화면에 뿌려주기
                }
                else {

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /////////////////////////////////////////1:1 매칭////////////////////////////////////////
    private void matchingUser(String counterpartyCode){
        if(classificationUserFlag == 1) { //내가 장애인이고, 상대방이 보호자일 경우
            Query query = reference.child("connect").child("guardian").orderByChild("myCode").equalTo(counterpartyCode);
            query.addListenerForSingleValueEvent(new ValueEventListener() { //보호자 테이블에서 counterpartyCode 존재 검사
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String counterpartyUID = null;
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        counterpartyUID = ds.getKey();
                    }

                    if(counterpartyUID != null){
                        Connect myConnect = new Connect(findMyCode, counterpartyCode); //내 코드에 상대 코드 연결
                        reference.child("connect").child("disabled").child(user.getUid()).setValue(myConnect);

                        Connect counterpartyConnect = new Connect(counterpartyCode, findMyCode); //상대 코드에 내 코드 연결
                        reference.child("connect").child("guardian").child(counterpartyUID).setValue(counterpartyConnect);

                        //매칭 성공시 가입 화면을 빠져나감.
                        /*Intent intent = new Intent(ConnectActivity.this, 장애인 메인 기능화면.class);
                        startActivity(intent);
                        finish();*/
                        Toast.makeText(ConnectActivity.this, "연결 성공", Toast.LENGTH_SHORT).show();

                    }
                    else {
                        Toast.makeText(ConnectActivity.this, "잘못된 코드를 입력하셨습니다.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        else if(classificationUserFlag == 2) { //내가 보호자고, 상대방이 장애인일 경우
            Query query = reference.child("connect").child("disabled").orderByChild("myCode").equalTo(counterpartyCode);
            query.addListenerForSingleValueEvent(new ValueEventListener() { //장애인 테이블에서 counterpartyCode 존재 검사
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String counterpartyUID = null;
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        counterpartyUID = ds.getKey();
                    }

                    if(counterpartyUID != null){
                        Connect myConnect = new Connect(findMyCode, counterpartyCode); //내 코드에 상대 코드 연결
                        reference.child("connect").child("guardian").child(user.getUid()).setValue(myConnect);

                        Connect counterpartyConnect = new Connect(counterpartyCode, findMyCode); //상대 코드에 내 코드 연결
                        reference.child("connect").child("disabled").child(counterpartyUID).setValue(counterpartyConnect);

                        //매칭 성공시 가입 화면을 빠져나감.
                        Intent intent = new Intent(ConnectActivity.this, RealTimeLocationActivity.class);
                        startActivity(intent);
                        finish();
                        Toast.makeText(ConnectActivity.this, "연결 성공", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(ConnectActivity.this, "잘못된 코드를 입력하셨습니다.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        else { //올바르지 않은 사용자
            Log.w(TAG, "미확인 유저(uid 오류)");
        }
    }
    
    /////////////////////////////////////////버튼 클릭 이벤트////////////////////////////////////////
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnCopy: //코드 복사
                String copyText = tvMyCode.getText().toString();

                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("copyText",copyText);
                clipboardManager.setPrimaryClip(clipData);

                Toast.makeText(getApplicationContext(),"텍스트가 복사되었습니다.",Toast.LENGTH_SHORT).show();
                break;

            case R.id.btnConnect: //보호자, 장애인 연결
                matchingUser(editOtherCode.getText().toString());
        }
    }
}
