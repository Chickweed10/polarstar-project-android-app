package com.example.polarstarproject;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
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

import com.example.polarstarproject.Domain.Connect;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Timer;
import java.util.TimerTask;

//상대방과 1:1 연결
public class ConnectActivity extends AppCompatActivity implements View.OnClickListener{
    Toolbar toolbar;

    TextView tvMyCode;
    EditText editOtherCode;
    Button btnConnect, btnCopy; //UI 변수들

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference reference = database.getReference();
    private FirebaseAuth mAuth;
    private FirebaseUser user; //firebase DB 변수

    private static final String TAG = "Connect"; //로그용 태그

    int classificationUserFlag = 0; //장애인 보호자 구별 (0: 기본값, 1: 장애인, 2: 보호자)
    String findMyCode = null; //내 코드값
    String counterpartyUID = null; //상대방 uid

    int cnt = 0; //재연결 끊기 안되는 오류 방지 플래그

    Timer timer; //상대방과 매칭 검사를 위한 타이머
    TimerTask timerTask;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //뒤로가기
        getSupportActionBar().setTitle("상대방과 연결");

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        tvMyCode = (TextView) findViewById(R.id.tvMyCode);
        editOtherCode = (EditText) findViewById(R.id.editOtherCode);
        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnCopy = (Button) findViewById(R.id.btnCopy);

        btnConnect.setOnClickListener(this);
        btnCopy.setOnClickListener(this);

        classificationUser(user.getUid()); //사용자 구별
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

    /////////////////////////////////////////사용자 구별, 내 코드값 가져오기////////////////////////////////////////
    private void classificationUser(String uid){ //firebase select 조회 함수, 내 코드값 저장
        Query disabledQuery = reference.child("connect").child("disabled").orderByKey().equalTo(uid); //내가 장애인일 경우
        disabledQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Connect connectUser = new Connect();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    connectUser = ds.getValue(Connect.class); //내 연결 코드 가져오기
                }

                if(connectUser.getMyCode() != null){
                    classificationUserFlag = 1; //사용자 구별 flag 값 1로(장애인) 변경
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

        Query guardianQuery = reference.child("connect").child("guardian").orderByKey().equalTo(uid); //내가 보호자일 경우
        guardianQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Connect connectUser = new Connect();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    connectUser = ds.getValue(Connect.class); //내 연결 코드 가져오기
                }

                if(connectUser.getMyCode() != null){
                    classificationUserFlag = 2; //사용자 구별 flag 값 2로(보호자) 변경
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
        
        skipScreen(); //화면 넘어가기
    }

    private void matchingskipScreen(){
        //매칭 성공시 메인 화면으로 이동
        Toast.makeText(ConnectActivity.this, "연결 성공", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(ConnectActivity.this, RealTimeLocationActivity.class);
        startActivity(intent);
        finish();
        finishAffinity(); //스택 비우기
    }

    /////////////////////////////////////////1:1 매칭////////////////////////////////////////
    private void matchingUser(String counterpartyCode){
        if(classificationUserFlag == 1) { //내가 장애인이고, 상대방이 보호자일 경우
            Query query = reference.child("connect").child("guardian").orderByChild("myCode").equalTo(counterpartyCode);
            query.addListenerForSingleValueEvent(new ValueEventListener() { //보호자 테이블에서 counterpartyCode 존재 검사
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        counterpartyUID = ds.getKey(); //상대 보호자 코드 가져오기
                    }

                    if(counterpartyUID != null){ //상대 보호자 코드가 올바를 경우
                        //이미 상대방과 연결되어 있는지 확인
                        reference.child("connect").child("guardian").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String value = null;
                                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                                    value = ds.child("counterpartyCode").getValue(String.class);
                                }

                                if(value == null && cnt == 0){ //이미 연결되지 않은 경우
                                    Connect myConnect = new Connect(findMyCode, counterpartyCode); //내 코드에 상대 코드 연결
                                    reference.child("connect").child("disabled").child(user.getUid()).setValue(myConnect);

                                    Connect counterpartyConnect = new Connect(counterpartyCode, findMyCode); //상대 코드에 내 코드 연결
                                    reference.child("connect").child("guardian").child(counterpartyUID).setValue(counterpartyConnect);

                                    //매칭 성공시 메인 화면으로 이동
                                    matchingskipScreen();
                                    
                                    cnt++;
                                }
                                else if(value != null){ //이미 연결된 경우
                                    Toast.makeText(ConnectActivity.this, "다른 피보호자와 연결된 사용자입니다.", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                    else { //상대 보호자 코드가 올바르지 않을 경우
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
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        counterpartyUID = ds.getKey(); //상대 장애인 코드 가져오기
                    }

                    if(counterpartyUID != null){ //상대 장애인 코드가 올바를 경우
                        //이미 상대방과 연결되어 있는지 확인
                        reference.child("connect").child("disabled").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String value = null;
                                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                                    value = ds.child("counterpartyCode").getValue(String.class);
                                }

                                if(value == null && cnt == 0){ //이미 연결되지 않은 경우
                                    Connect myConnect = new Connect(findMyCode, counterpartyCode); //내 코드에 상대 코드 연결
                                    reference.child("connect").child("guardian").child(user.getUid()).setValue(myConnect);

                                    Connect counterpartyConnect = new Connect(counterpartyCode, findMyCode); //상대 코드에 내 코드 연결
                                    reference.child("connect").child("disabled").child(counterpartyUID).setValue(counterpartyConnect);

                                    //매칭 성공시 메인 화면으로 이동
                                    matchingskipScreen();

                                    cnt++;
                                }
                                else if(value != null){
                                    Toast.makeText(ConnectActivity.this, "다른 보호자와 연결된 사용자입니다.", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                    else { //상대 장애인 코드가 올바르지 않을 경우
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

    /////////////////////////////////////////연결 여부 확인 후 화면 넘어가기////////////////////////////////////////
    private void skipScreen(){
        timer = new Timer();

        timerTask = new TimerTask() {
            @Override
            public void run() {
                //1초마다 실행
                connectCheck(); //상대방과 매칭 여부 확인
            }
        };
        timer.schedule(timerTask,0,1000);
    }

    private void connectCheck(){ //상대방과 매칭 여부 확인
        if(classificationUserFlag == 1) { //내가 장애인인 경우
            Query query = reference.child("connect").child("disabled").orderByKey().equalTo(user.getUid());
            query.addListenerForSingleValueEvent(new ValueEventListener() { //내 테이블에서 counterpartyCode 존재 검사
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Connect connect = new Connect();
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        connect = ds.getValue(Connect.class);
                    }

                    if(connect.getCounterpartyCode() != null && !connect.getCounterpartyCode().isEmpty()){ //상대방과 매칭 되어있을 경우
                        timer.cancel();
                        timerTask.cancel(); //타이머 종료
                        
                        //메인 화면으로 이동
                        Toast.makeText(getApplicationContext(),"상대방과 연결되었습니다.",Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ConnectActivity.this, RealTimeLocationActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    else {

                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        else if(classificationUserFlag == 2) { //내가 보호자인 경우
            Query query = reference.child("connect").child("guardian").orderByKey().equalTo(user.getUid());
            query.addListenerForSingleValueEvent(new ValueEventListener() { //내 테이블에서 counterpartyCode 존재 검사
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Connect connect = new Connect();
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        connect = ds.getValue(Connect.class);
                    }

                    if(connect.getCounterpartyCode() != null && !connect.getCounterpartyCode().isEmpty()){ //상대방과 매칭 되어있을 경우
                        timer.cancel();
                        timerTask.cancel(); //타이머 종료

                        //메인 화면으로 이동
                        Toast.makeText(getApplicationContext(),"상대방과 연결되었습니다.",Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ConnectActivity.this, RealTimeLocationActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    else {

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
                cnt = 0;
                matchingUser(editOtherCode.getText().toString());
                editOtherCode.setText(null);
        }
    }
}
