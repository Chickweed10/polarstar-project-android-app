package com.example.polarstarproject;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.polarstarproject.Domain.Disabled;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kakao.sdk.user.model.User;

public class Myinfo_DuserActivity extends AppCompatActivity {
    private DatabaseReference mDatabase;
    ImageView Profl;
    TextView Name, Email, PhoneNum, Birth, Address, DrDisG;
    Button Bt;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_myinfo_duser);

        mDatabase = FirebaseDatabase.getInstance().getReference(); //DatabaseReference의 인스턴스

        Profl = (ImageView) findViewById(R.id.Profl); //프로필 사진
        Name = (TextView) findViewById(R.id.Name); //이름
        Email = (TextView) findViewById(R.id.Email); //이메일
        PhoneNum = (TextView) findViewById(R.id.PhoneNum); //전화번호
        Birth = (TextView) findViewById(R.id.Birth); //생년월일
        Address = (TextView) findViewById(R.id.Address); //주소
        DrDisG = (TextView)findViewById(R.id.DrDisG); //장애등급
        Bt = (Button) findViewById(R.id.Bt); //프로필 수정


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
}