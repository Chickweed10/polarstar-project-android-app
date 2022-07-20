package com.example.polarstarproject;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.polarstarproject.Domain.Disabled;
import com.example.polarstarproject.Domain.EmailVerified;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class Myinfo_DuserActivity extends AppCompatActivity implements View.OnClickListener{
    private DatabaseReference mDatabase;
    ImageView Profl;
    TextView DrDisG;
    EditText Name, Email, PhoneNum, Birth, Address;
    Button Bt;

    private FirebaseAuth mAuth;
    private FirebaseUser user; //firebase 변수

    private static final String TAG = "MyinfoDuser";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_myinfo_duser);

        mDatabase = FirebaseDatabase.getInstance().getReference(); //DatabaseReference의 인스턴스
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        Profl = (ImageView) findViewById(R.id.Profl); //프로필 사진
        Name = (EditText) findViewById(R.id.mProflName); //이름
        Email = (EditText) findViewById(R.id.mProflEmail); //이메일
        PhoneNum = (EditText) findViewById(R.id.mProflPhoneNum); //전화번호
        Birth = (EditText) findViewById(R.id.mProflBirth); //생년월일
        Address = (EditText) findViewById(R.id.mProflAddress); //주소
        DrDisG = (TextView)findViewById(R.id.mProflDisG); //장애정도 (pre.장애등급)
        //장애정도 변경필요?_cogy //DrDisG -> 드롭메뉴 생김
        Bt = (Button) findViewById(R.id.mProflBtEdit); //프로필 수정

        mProflBtEmailCk = (Button) findViewById(R.id.mProflBtEmailCk); //이메일 인증

        mProflBtEmailCk.setOnClickListener(this);

        emailVerifiedButton();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser(); //현재 로그인한 유저
        String uid = user.getUid(); // 이 유저 uid 가져오기
        readUser(uid);

        /*if (user != null) {
            //String name = user.getDisplayName();
            //Name.setText(name);
            Uri photoUrl = user.getPhotoUrl();
            Profl.setImageURI(photoUrl);
        }*/

    }

    private void readUser(String uid) {
        //데이터 읽기
        mDatabase.child("disabled").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Disabled user = snapshot.getValue(Disabled.class);
                Uri uri = Uri.parse("gs://polarstarproject-7034b.appspot.com/"+user.profileImage+".jpeg");
                Toast.makeText(getApplicationContext(),"데이터"+uri, Toast.LENGTH_LONG).show();
                Profl.setImageURI(uri);
                Name.setText("이름: " + user.name);
                Email.setText("이메일: " + user.email);
                PhoneNum.setText("전화번호: " + user.phoneNumber);
                Birth.setText("생년월일: " + user.birth);
                Address.setText("주소: " + user.address + user.detailAddress);
                DrDisG.setText("장애등급: " + user.disabilityLevel);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { //참조에 액세스 할 수 없을 때 호출
                Toast.makeText(getApplicationContext(),"데이터를 가져오는데 실패했습니다" , Toast.LENGTH_LONG).show();
            }
        });
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
        Log.d(TAG, "메일 유효성" + user.isEmailVerified());
        user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    if(user.isEmailVerified()){ //이메일 인증 성공
                        EmailVerified emailVerified = new EmailVerified(true);
                        mDatabase.child("emailverified").child(user.getUid()).setValue(emailVerified); //이메일 유효성 true

                        mProflBtEmailCk.setEnabled(false); //이메일 인증 버튼 비활성화

                        Toast.makeText(Myinfo_DuserActivity.this, "인증 메일 전송 완료", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "메일 인증 성공");
                    }
                    else {
                        EmailVerified emailVerified = new EmailVerified(false);
                        mDatabase.child("emailverified").child(user.getUid()).setValue(emailVerified); //이메일 유효성 true

                        mProflBtEmailCk.setEnabled(true); //이메일 인증 버튼 활성화

                        Toast.makeText(Myinfo_DuserActivity.this, "유효하지 않은 메일입니다.", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "메일 인증 실패");
                    }
                }
                else {
                    EmailVerified emailVerified = new EmailVerified(false);
                    mDatabase.child("emailverified").child(user.getUid()).setValue(emailVerified); //이메일 유효성 true

                    mProflBtEmailCk.setEnabled(true); //이메일 인증 버튼 활성화

                    Toast.makeText(Myinfo_DuserActivity.this, "인증 메일 전송 실패", Toast.LENGTH_SHORT).show();
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
        }
    }
}