package com.example.polarstarproject;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.widget.TextView;
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
import com.google.firebase.storage.UploadTask;


public class Myinfo_DuserActivity extends AppCompatActivity implements View.OnClickListener{
    Toolbar toolbar;

    private DatabaseReference mDatabase;

    ImageView Profl;
    EditText Name, Email, Birth, mProflDetailAddress;
    TextView PhoneNum, Address;
    Button Bt, mProflBtChn, mProflBtPWChage, mProflFdAdd;
    String sex,  cSex;
    //Spinner DrDisG;
    RadioGroup rdgGroup;
    RadioButton rdoButton, mProflBtGenderF, mProflBtGenderM;

    private FirebaseAuth mAuth;
    private FirebaseUser user; //firebase 변수
    String myUid;

    private static final String TAG = "MyinfoDuser";

    private FirebaseStorage storage = FirebaseStorage.getInstance();;
    private StorageReference storageRef, riversRef; //firebase DB, Storage 변수
    private Uri imageUri;
    private String pathUri = "profile/default.png"; //프로필 이미지 처리 변수

    private static final int SEARCH_ADDRESS_ACTIVITY = 10000; //우편번호 검색


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
        PhoneNum = (TextView) findViewById(R.id.mProflPhoneNum); //전화번호
        Birth = (EditText) findViewById(R.id.mProflBirth); //생년월일

        Address = (TextView) findViewById(R.id.mProflAddress); //주소 텍스트
        mProflFdAdd = (Button) findViewById(R.id.mProflFdAdd); // 주소 버튼
        mProflDetailAddress = (EditText) findViewById(R.id.mProflDetailAddress); //상세 주소
        
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

        Bt = (Button) findViewById(R.id.mProflBtEdit); //프로필 수정
        Bt.setOnClickListener(this);

        mProflBtChn = (Button) findViewById(R.id.mProflBtChn); //프로필 사진
        mProflBtChn.setOnClickListener(this);

        user = FirebaseAuth.getInstance().getCurrentUser(); //현재 로그인한 유저
        myUid = user.getUid(); // 이 유저 uid 가져오기

        storageRef = storage.getReference();
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

        mProflBtPWChage = (Button) findViewById(R.id.mProflBtPWChage); //비밀번호 재설정
        //재설정 버튼이 눌리면
        mProflBtPWChage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = Email.getText().toString().trim();
                if(email.isEmpty()) {
                    Toast.makeText(Myinfo_DuserActivity.this,
                            "이메일을 입력해주세요.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    mAuth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(Myinfo_DuserActivity.this,
                                                "비밀번호 재설정 이메일을 발송했습니다.",
                                                Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(Myinfo_DuserActivity.this,
                                                "가입한 이메일을 입력하세요.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });
        mProflFdAdd = (Button) findViewById(R.id.mProflFdAdd); //우편번호 찾기
        mProflFdAdd.setOnClickListener(this);
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

                Email.setFocusable(false);
                Birth.setFocusable(false);

                //스피너 라디오 버튼 세팅 가져오기
                cSex = user.sex;
                Log.w(TAG, "성별: "+ cSex);

                if(cSex.equals("여")) {
                    mProflBtGenderF.setChecked(true);
                    mProflBtGenderM.setEnabled(false);
                }
                else {
                    mProflBtGenderM.setChecked(true);
                    mProflBtGenderF.setEnabled(false);
                }Log.w(TAG, "선택: "+ mProflBtGenderF.isChecked());


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { //참조에 액세스 할 수 없을 때 호출
                Toast.makeText(getApplicationContext(),"데이터를 가져오는데 실패했습니다" , Toast.LENGTH_LONG).show();
            }
        });
    }

    /////////////////////////////////////////프로필 사진 등록////////////////////////////////////////
    private void gotoAlbum() { //갤러리 이동
        final CharSequence[] oItems = {"앨범에서 사진 선택", "기본 이미지로 변경"};

        AlertDialog.Builder oDialog = new AlertDialog.Builder(this,
                android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);

        oDialog.setTitle("프로필 사진").setItems(oItems, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: //앨범
                                Intent albumIntent = new Intent();
                                albumIntent.setType("image/*");
                                albumIntent.setAction(Intent.ACTION_GET_CONTENT);
                                startActivityForResult(albumIntent, 0);
                                break;
                            case 1: //기본 프로필
                                //firebase Storage 삭제
                                FirebaseStorage storage = FirebaseStorage.getInstance();
                                StorageReference storageRef = storage.getReference();
                                StorageReference myPro = storageRef.child("profile").child(user.getUid());
                                if (myPro != null) { //프로필 삭제
                                    myPro.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(Myinfo_DuserActivity.this, "프로필 삭제가 완료되었습니다.", Toast.LENGTH_SHORT).show();
                                                Log.d(TAG, "firebase Storage delete");
                                            }
                                        }
                                    });
                                    //기본 프로필 띄우기
                                    StorageReference defaultPro = storageRef.child("profile").child("default.png");
                                    defaultPro.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            //이미지 로드 성공시
                                            Glide.with(Myinfo_DuserActivity.this).load(uri).into(Profl);
                                        }
                                    });
                                }
                        }
                    }
                })
                //.setCancelable(false)
                .show();
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

                if(imageUri != null){ //프로필 수정 했을 시
                    Glide.with(getApplicationContext())
                            .load(intent.getData())
                            .into(Profl); //버튼에 이미지 삽입

                    pathUri = "profile/"+myUid;
                    firebaseImageUpload(pathUri); //이미지 등록

                    Toast.makeText(Myinfo_DuserActivity.this, "프로필 변경이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        }else if(requestCode == SEARCH_ADDRESS_ACTIVITY) { //우편번호 등록
            if (resultCode == RESULT_OK) {
                String data = intent.getExtras().getString("data");
                if(data != null) {
                    Address.setText(data);
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.mProflBtChn: //프로필 이미지 버튼 클릭 시
                gotoAlbum(); //
                break;
            case R.id.mProflFdAdd: //우편번호 검색
                Intent i = new Intent(Myinfo_DuserActivity.this, WebViewActivity.class); //우편번호 검색 화면으로 전환
                startActivityForResult(i, SEARCH_ADDRESS_ACTIVITY);
                break;
            case R.id.mProflBtEdit: //수정 버튼 클릭 시 저장
                try{
                    mDatabase.child("disabled").child(myUid).child("name").setValue(Name.getText().toString());
                    mDatabase.child("disabled").child(myUid).child("email").setValue(Email.getText().toString());
                    mDatabase.child("disabled").child(myUid).child("phoneNumber").setValue(PhoneNum.getText().toString());
                    mDatabase.child("disabled").child(myUid).child("address").setValue(Address.getText().toString());
                    mDatabase.child("disabled").child(myUid).child("detailAddress").setValue(mProflDetailAddress.getText().toString());

                    mDatabase.child("disabled").child(myUid).child("birth").setValue(Birth.getText().toString());
                    mDatabase.child("disabled").child(myUid).child("sex").setValue(sex);
                }catch (@NonNull Exception e){
                    return;
                }

                Toast.makeText(Myinfo_DuserActivity.this, "프로필 수정이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                //mDatabase.child("disabled").child(myUid).child("disabilityLevel").setValue(DrDisG.getSelectedItem().toString());
                //디테일 어드레스

                break;
        }
    }
}