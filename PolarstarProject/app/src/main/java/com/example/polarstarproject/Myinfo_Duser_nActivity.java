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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.polarstarproject.Domain.Connect;
import com.example.polarstarproject.Domain.EmailVerified;
import com.example.polarstarproject.Domain.Guardian;
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
import com.google.firebase.storage.UploadTask;

public class Myinfo_Duser_nActivity extends AppCompatActivity implements View.OnClickListener{
    Toolbar toolbar;

    private DatabaseReference mDatabase;

    ImageView Profl;
    EditText Name, Email, PhoneNum, Birth, Address, dAddress;
    Button Bt, mProflBtEmailCkN, mProflBtChnN;
    String sex,  cSex;
    RadioGroup rdgGroup;
    RadioButton rdoButton, mProflBtGenderF, mProflBtGenderM;

    private FirebaseAuth mAuth;
    private FirebaseUser user; //firebase 변수
    String mynUid;

    private static final String TAG = "MyinfonDuser";

    private FirebaseStorage storage = FirebaseStorage.getInstance();;
    private StorageReference storageRef, riversRef; //firebase DB, Storage 변수
    private Uri imageUri;
    private String pathUri = "profile/default.png"; //프로필 이미지 처리 변수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_myinfo_duser_n);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //뒤로가기
        getSupportActionBar().setTitle("내 정보");

        mDatabase = FirebaseDatabase.getInstance().getReference(); //DatabaseReference의 인스턴스
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        Profl = (ImageView) findViewById(R.id.ProflN); //프로필 사진
        Name = (EditText) findViewById(R.id.mProflNameN); //이름
        Email = (EditText) findViewById(R.id.mProflEmailN); //이메일
        PhoneNum = (EditText) findViewById(R.id.mProflPhoneNumN); //전화번호
        Birth = (EditText) findViewById(R.id.mProflBirthN); //생년월일
        Address = (EditText) findViewById(R.id.mProflAddressN); //주소
        dAddress = (EditText) findViewById(R.id.mProflDetailAddressN); //주소
        mProflBtGenderF = findViewById( R.id.mProflBtGenderFN);
        mProflBtGenderM = findViewById( R.id.mProflBtGenderMN);
        rdgGroup = findViewById( R.id.mProflBtGenderN );

        rdgGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                rdoButton = findViewById( rdgGroup.getCheckedRadioButtonId() );
                sex = rdoButton.getText().toString();
            }
        });

        Bt = (Button) findViewById(R.id.mProflBtEditN); //프로필 수정
        Bt.setOnClickListener(this);

        mProflBtChnN = (Button) findViewById(R.id.mProflBtChnN); //프로필 사진
        mProflBtChnN.setOnClickListener(this);

        mProflBtEmailCkN = (Button) findViewById(R.id.mProflBtEmailCkN); //이메일 인증
        mProflBtEmailCkN.setOnClickListener(this);

        emailVerifiedButton();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser(); //현재 로그인한 유저
        mynUid = user.getUid(); // 이 유저 uid 가져오기

        StorageReference storageRef = storage.getReference();
        StorageReference myPro = storageRef.child("profile").child(mynUid);
        if (myPro != null) {
            myPro.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    //이미지 로드 성공시
                    Glide.with(Myinfo_Duser_nActivity.this).load(uri).into(Profl);

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    //이미지 로드 실패시
                    Log.w(TAG, "이미지 로드 실패");
                }
            });
        }
        readUser(mynUid);
    }


    /////////////////////////////////////////액티비티 뒤로가기 설정////////////////////////////////////////
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home: { //toolbar의 back키를 눌렀을 때 동작
                Intent intent = new Intent(getApplicationContext(), RealTimeLocationActivity.class);
                startActivity(intent);
                finish(); //화면 이동

                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed() { //뒤로가기 했을 때
        Intent intent = new Intent(getApplicationContext(), RealTimeLocationActivity.class);
        startActivity(intent);
        finish(); //화면 이동
    }

    private void readUser(String uid) {
        //데이터 읽기
        mDatabase.child("guardian").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Guardian user = snapshot.getValue(Guardian.class);
                //Uri uri = Uri.parse("gs://polarstarproject-7034b.appspot.com/"+user.profileImage+".jpeg");
                //Toast.makeText(getApplicationContext(),"데이터"+uri, Toast.LENGTH_LONG).show();
                //Profl.setImageURI(uri);
                Name.setText(user.name);
                Email.setText(user.email);
                PhoneNum.setText(user.phoneNumber);
                Birth.setText(user.birth);
                Address.setText(user.address);
                dAddress.setText(user.detailAddress);

                //스피너 라디오 버튼 세팅 가져오기
                cSex = user.sex;
                Log.w(TAG, "성별: "+ cSex);
                if(cSex.equals("여")) {
                    mProflBtGenderF.setChecked(true);
                }
                else {
                    mProflBtGenderM.setChecked(true);
                }Log.w(TAG, "선택: "+ mProflBtGenderF.isChecked());
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

            mProflBtEmailCkN.setEnabled(false); //이메일 인증 버튼 비활성화

            Log.d(TAG, "메일 인증 성공");
        }
        else{
            EmailVerified emailVerified = new EmailVerified(false);
            mDatabase.child("emailverified").child(user.getUid()).setValue(emailVerified); //이메일 유효성 false

            mProflBtEmailCkN.setEnabled(true); //이메일 인증 버튼 활성화

            Log.d(TAG, "메일 인증 실패");
        }
    }

    @Override
    protected void onResume(){ //Activity가 사용자와 상호작용하면
        super.onResume();

        if(user.isEmailVerified()) {
            EmailVerified emailVerified = new EmailVerified(true);
            mDatabase.child("emailverified").child(user.getUid()).setValue(emailVerified); //이메일 유효성 true

            mProflBtEmailCkN.setEnabled(false); //이메일 인증 버튼 비활성화

            Log.d(TAG, "메일 인증 성공");
        }
        else{
            EmailVerified emailVerified = new EmailVerified(false);
            mDatabase.child("emailverified").child(user.getUid()).setValue(emailVerified); //이메일 유효성 false

            mProflBtEmailCkN.setEnabled(true); //이메일 인증 버튼 활성화

            Log.d(TAG, "메일 인증 실패");
        }
    }


    private void emailVerifiedButton(){
        if(user.isEmailVerified()){
            mProflBtEmailCkN.setEnabled(false);
        }
        else{
            mProflBtEmailCkN.setEnabled(true);
        }
    }

    private void emailAuthentication(){
        user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    Toast.makeText(Myinfo_Duser_nActivity.this, "인증 메일을 전송하였습니다.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "인증 메일 전송 성공");

                    mProflBtEmailCkN.setEnabled(false); //이메일 인증 버튼 비활성화
                }
                else {
                    EmailVerified emailVerified = new EmailVerified(false);
                    mDatabase.child("emailverified").child(user.getUid()).setValue(emailVerified); //이메일 유효성 false

                    mProflBtEmailCkN.setEnabled(true); //이메일 인증 버튼 활성화

                    Toast.makeText(Myinfo_Duser_nActivity.this, "인증 메일을 전송하지 못했습니다.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "인증 메일 전송 실패");
                }
            }
        });
    }

    /////////////////////////////////////////프로필 사진 등록////////////////////////////////////////
    private void gotoAlbum() { //갤러리 이동
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, 0);
    }

    private void firebaseImageUpload(String pathUri) { //파이어베이스 이미지 등록
        storageRef = storage.getReference();
        riversRef = storageRef.child(pathUri);
        UploadTask uploadTask = riversRef.putFile(imageUri); //이미지 업로드

        uploadTask.addOnFailureListener(new OnFailureListener() { //업로드 실패시
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "사진 업로드 실패", e);
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() { //업로드 성공시
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.w(TAG, "사진 업로드 성공");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == 0) { //프로필 사진
            if (resultCode == RESULT_OK) {
                imageUri = intent.getData();
                Glide.with(getApplicationContext())
                        .load(intent.getData())
                        .into(Profl); //버튼에 이미지 삽입
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.mProflBtEmailCkN: //이메일 인증 버튼 클릭
                emailAuthentication(); //이메일 인증 함수 호출
                break;
            case R.id.mProflBtChnN: //프로필 이미지 버튼 클릭 시
                gotoAlbum(); //
                break;
            case R.id.mProflBtEditN: //수정 버튼 클릭 시 저장
                mDatabase.child("guardian").child(mynUid).child("name").setValue(Name.getText().toString());
                mDatabase.child("guardian").child(mynUid).child("email").setValue(Email.getText().toString());
                mDatabase.child("guardian").child(mynUid).child("phoneNumber").setValue(PhoneNum.getText().toString());
                mDatabase.child("guardian").child(mynUid).child("address").setValue(Address.getText().toString());
                mDatabase.child("guardian").child(mynUid).child("detailAddress").setValue(dAddress.getText().toString());
                mDatabase.child("guardian").child(mynUid).child("birth").setValue(Birth.getText().toString());
                mDatabase.child("guardian").child(mynUid).child("sex").setValue(sex);

                if(imageUri != null){ //프로필 수정 했을 시
                    pathUri = "profile/"+mynUid;
                    firebaseImageUpload(pathUri); //이미지 등록
                }
                break;
        }
    }
}