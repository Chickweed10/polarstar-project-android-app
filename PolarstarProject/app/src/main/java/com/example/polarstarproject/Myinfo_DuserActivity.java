package com.example.polarstarproject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
import com.example.polarstarproject.Domain.EmailVerified;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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


public class Myinfo_DuserActivity extends AppCompatActivity implements View.OnClickListener{
    Toolbar toolbar;

    private DatabaseReference mDatabase;

    ImageView Profl;
    EditText Name, Email, PhoneNum, Birth, Address, mProflDetailAddress;
    Button Bt, mProflBtEmailCk;
    String sex,  cSex, cDrDisG;
    Spinner DrDisG;
    RadioGroup rdgGroup;
    RadioButton rdoButton, mProflBtGenderF, mProflBtGenderM;

    private FirebaseAuth mAuth;
    private FirebaseUser user; //firebase 변수
    String myUid;

    private static final String TAG = "MyinfoDuser";

    int classificationUserFlag = 0; //장애인 보호자 구별 (0: 기본값, 1: 장애인, 2: 보호자)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_myinfo_duser);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //뒤로가기
        getSupportActionBar().setTitle("내 정보");

        mDatabase = FirebaseDatabase.getInstance().getReference(); //DatabaseReference의 인스턴스
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        Profl = (ImageView) findViewById(R.id.Profl); //프로필 사진
        Name = (EditText) findViewById(R.id.mProflName); //이름
        Email = (EditText) findViewById(R.id.mProflEmail); //이메일
        PhoneNum = (EditText) findViewById(R.id.mProflPhoneNum); //전화번호
        Birth = (EditText) findViewById(R.id.mProflBirth); //생년월일
        Address = (EditText) findViewById(R.id.mProflAddress); //주소
        mProflDetailAddress = (EditText) findViewById(R.id.mProflDetailAddress); //주소
        mProflBtGenderF = findViewById( R.id.mProflBtGenderF);
        mProflBtGenderM = findViewById( R.id.mProflBtGenderM);
        rdgGroup = findViewById( R.id.mProflBtGender );

        rdgGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                rdoButton = findViewById( rdgGroup.getCheckedRadioButtonId() );
                sex = rdoButton.getText().toString();
            }
        });

        //RadioButton rdoButton = findViewById( rdgGroup.getCheckedRadioButtonId() );
        //String sex = rdoButton.getText().toString();

        DrDisG = (Spinner)findViewById(R.id.mProflDrDisG); //장애정도 (pre.장애등급)
        Bt = (Button) findViewById(R.id.mProflBtEdit); //프로필 수정
        Bt.setOnClickListener(this);

        mProflBtEmailCk = (Button) findViewById(R.id.mProflBtEmailCk); //이메일 인증
        mProflBtEmailCk.setOnClickListener(this);

        emailVerifiedButton(); //이메일 인증 버튼 활성화 유무

        user = FirebaseAuth.getInstance().getCurrentUser(); //현재 로그인한 유저
        myUid = user.getUid(); // 이 유저 uid 가져오기

        FirebaseStorage storage = FirebaseStorage.getInstance(); //프로필 사진 가져오기
        StorageReference storageRef = storage.getReference();
        StorageReference myPro = storageRef.child("profile").child(myUid);
        if (myPro != null) {
            myPro.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    //이미지 로드 성공시
                    Glide.with(Myinfo_DuserActivity.this).load(uri).into(Profl);

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    //이미지 로드 실패시
                    Log.w(TAG, "이미지 로드 실패");
                }
            });
        }
        readUser(myUid);
    }

    /////////////////////////////////////////사용자 구별////////////////////////////////////////
    private void classificationUser(String uid){ //firebase select 조회 함수, 내 connect 테이블 조회
        Query disabledQuery = mDatabase.child("connect").child("disabled").orderByKey().equalTo(uid); //장애인 테이블 조회
        disabledQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Connect myConnect = new Connect();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    myConnect = ds.getValue(Connect.class);
                }

                if(myConnect.getMyCode() != null && !myConnect.getMyCode().isEmpty()){
                    classificationUserFlag = 1;
                    skipScreen(); //화면 넘어가기
                }
                else {

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        Query guardianQuery = mDatabase.child("connect").child("guardian").orderByKey().equalTo(uid); //보호자 테이블 조회
        guardianQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Connect myConnect = new Connect();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    myConnect = ds.getValue(Connect.class);
                }

                if(myConnect.getMyCode() != null && !myConnect.getMyCode().isEmpty()){
                    classificationUserFlag = 2;
                    skipScreen(); //화면 넘어가기
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
                classificationUser(user.getUid()); //사용자 구별 후 실시간 위치 화면으로 돌아감

                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed() { //뒤로가기 했을 때
        classificationUser(user.getUid()); //사용자 구별 후 실시간 위치 화면으로 돌아감
    }

    /////////////////////////////////////////화면 넘어가기////////////////////////////////////////
    public void skipScreen(){
        if(classificationUserFlag == 1){ //장애인
            Intent intent = new Intent(getApplicationContext(), RealTimeLocationActivity.class);
            startActivity(intent);
            finish();
        }
        else if(classificationUserFlag == 2){ //보호자
            Intent intent = new Intent(getApplicationContext(), RealTimeLocationActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void readUser(String uid) {
        //데이터 읽기
        mDatabase.child("disabled").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Disabled user = snapshot.getValue(Disabled.class);
                Name.setText(user.name);
                Email.setText(user.email);
                PhoneNum.setText(user.phoneNumber);
                Birth.setText(user.birth);
                Address.setText(user.address);
                mProflDetailAddress.setText(user.detailAddress);
                //스피너 라디오 버튼 세팅 가져오기
                cSex = user.sex;
                Log.w(TAG, "성별: "+ cSex);
                cDrDisG = user.disabilityLevel;
                Log.w(TAG, "정도: "+ cDrDisG);
                if(cSex.equals("여")) {
                    mProflBtGenderF.setChecked(true);
                }
                else {
                    mProflBtGenderM.setChecked(true);
                }Log.w(TAG, "선택: "+ mProflBtGenderF.isChecked());

                if(cDrDisG.equals("경증")) {
                    DrDisG.setSelection(1);
                }
                else {
                    DrDisG.setSelection(0);
                }Log.w(TAG, "선택: "+ DrDisG);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { //참조에 액세스 할 수 없을 때 호출
                Toast.makeText(getApplicationContext(),"데이터를 가져오는데 실패했습니다" , Toast.LENGTH_LONG).show();
            }
        });
    }

    /////////////////////////////////////////이메일 인증////////////////////////////////////////
    @Override
    protected void onStart(){
        super.onStart();

        if(user.isEmailVerified()) {
            EmailVerified emailVerified = new EmailVerified(true);
            mDatabase.child("emailverified").child(user.getUid()).setValue(emailVerified); //이메일 유효성 true

            mProflBtEmailCk.setEnabled(false); //이메일 인증 버튼 비활성화

            Log.d(TAG, "메일 인증 성공");
        }
        else{
            EmailVerified emailVerified = new EmailVerified(false);
            mDatabase.child("emailverified").child(user.getUid()).setValue(emailVerified); //이메일 유효성 false

            mProflBtEmailCk.setEnabled(true); //이메일 인증 버튼 활성화

            Log.d(TAG, "메일 인증 실패");
        }
    }

    @Override
    protected void onResume(){
        super.onResume();

        if(user.isEmailVerified()) {
            EmailVerified emailVerified = new EmailVerified(true);
            mDatabase.child("emailverified").child(user.getUid()).setValue(emailVerified); //이메일 유효성 true

            mProflBtEmailCk.setEnabled(false); //이메일 인증 버튼 비활성화

            Log.d(TAG, "메일 인증 성공");
        }
        else{
            EmailVerified emailVerified = new EmailVerified(false);
            mDatabase.child("emailverified").child(user.getUid()).setValue(emailVerified); //이메일 유효성 false

            mProflBtEmailCk.setEnabled(true); //이메일 인증 버튼 활성화

            Log.d(TAG, "메일 인증 실패");
        }
    }


    private void emailVerifiedButton(){
        if(user.isEmailVerified()){
            mProflBtEmailCk.setEnabled(false);
        }
        else{
            mProflBtEmailCk.setEnabled(true);
        }
    }

    private void emailAuthentication(){
        user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    Toast.makeText(Myinfo_DuserActivity.this, "인증 메일을 전송하였습니다.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "인증 메일 전송 성공");

                    mProflBtEmailCk.setEnabled(false); //이메일 인증 버튼 비활성화
                }
                else {
                    EmailVerified emailVerified = new EmailVerified(false);
                    mDatabase.child("emailverified").child(user.getUid()).setValue(emailVerified); //이메일 유효성 false

                    mProflBtEmailCk.setEnabled(true); //이메일 인증 버튼 활성화

                    Toast.makeText(Myinfo_DuserActivity.this, "인증 메일을 전송하지 못했습니다.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "인증 메일 전송 실패");
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.mProflBtEmailCk: //이메일 인증 버튼 클릭 시
                emailAuthentication(); //이메일 인증 함수 호출
                break;
            case R.id.mProflBtEdit: //수정 버튼 클릭 시 저장
                mDatabase.child("disabled").child(myUid).child("name").setValue(Name.getText().toString());
                mDatabase.child("disabled").child(myUid).child("email").setValue(Email.getText().toString());
                mDatabase.child("disabled").child(myUid).child("phoneNumber").setValue(PhoneNum.getText().toString());
                mDatabase.child("disabled").child(myUid).child("address").setValue(Address.getText().toString());
                mDatabase.child("disabled").child(myUid).child("birth").setValue(Birth.getText().toString());
                mDatabase.child("disabled").child(myUid).child("sex").setValue(sex);
                mDatabase.child("disabled").child(myUid).child("disabilityLevel").setValue(DrDisG.getSelectedItem().toString());
                break;
        }
    }
}