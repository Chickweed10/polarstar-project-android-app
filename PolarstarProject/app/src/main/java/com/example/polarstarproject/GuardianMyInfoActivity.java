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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
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
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class GuardianMyInfoActivity extends AppCompatActivity implements View.OnClickListener{
    Toolbar toolbar;

    private DatabaseReference mDatabase;

    ImageView Profl;
    EditText Name, Email, Birth, dAddress;
    TextView PhoneNum, Address;
    Button Bt, mProflBtChnN, mProflBtPWChageN, mProflFdAddN;
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

    private static final int SEARCH_ADDRESS_ACTIVITY = 10000; //우편번호 검색d

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
        PhoneNum = (TextView) findViewById(R.id.mProflPhoneNumN); //전화번호
        Birth = (EditText) findViewById(R.id.mProflBirthN); //생년월일
        
        Address = (TextView) findViewById(R.id.mProflAddressN); //주소
        mProflFdAddN = (Button) findViewById(R.id.mProflFdAddN); // 주소 버튼
        dAddress = (EditText) findViewById(R.id.mProflDetailAddressN); // 상세 주소
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

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser(); //현재 로그인한 유저
        mynUid = user.getUid(); // 이 유저 uid 가져오기

        StorageReference storageRef = storage.getReference();
        StorageReference myPro = storageRef.child("profile").child(mynUid);
        if (myPro != null) {
            myPro.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    //이미지 로드 성공시
                    Glide.with(GuardianMyInfoActivity.this).load(uri).into(Profl);

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

        mProflBtPWChageN = (Button) findViewById(R.id.mProflBtPWChageN); //비밀번호 재설정
        //재설정 버튼이 눌리면
        mProflBtPWChageN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = Email.getText().toString().trim();
                if(email.isEmpty()) {
                    Toast.makeText(GuardianMyInfoActivity.this,
                            "이메일을 입력해주세요.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    mAuth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(GuardianMyInfoActivity.this,
                                                "비밀번호 재설정 이메일을 발송했습니다.",
                                                Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(GuardianMyInfoActivity.this,
                                                "가입한 이메일을 입력하세요.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });
        mProflFdAddN = (Button) findViewById(R.id.mProflFdAddN); //우편번호 찾기
        mProflFdAddN.setOnClickListener(this);
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

                PhoneNum.setFocusable(false);
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
                                                Toast.makeText(GuardianMyInfoActivity.this, "프로필 삭제가 완료되었습니다.", Toast.LENGTH_SHORT).show();
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
                                            Glide.with(GuardianMyInfoActivity.this).load(uri).into(Profl);
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

                    pathUri = "profile/"+mynUid;
                    firebaseImageUpload(pathUri); //이미지 등록

                    Toast.makeText(GuardianMyInfoActivity.this, "프로필 변경이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        } else if(requestCode == SEARCH_ADDRESS_ACTIVITY) { //우편번호 등록
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
            case R.id.mProflBtChnN: //프로필 이미지 버튼 클릭 시
                gotoAlbum(); //
                break;
            case R.id.mProflFdAddN: //우편번호 검색
                Intent i = new Intent(GuardianMyInfoActivity.this, WebViewActivity.class); //우편번호 검색 화면으로 전환
                startActivityForResult(i, SEARCH_ADDRESS_ACTIVITY);
                break;
            case R.id.mProflBtEditN: //수정 버튼 클릭 시 저장
                try{
                    mDatabase.child("guardian").child(mynUid).child("name").setValue(Name.getText().toString());
                    mDatabase.child("guardian").child(mynUid).child("email").setValue(Email.getText().toString());
                    mDatabase.child("guardian").child(mynUid).child("address").setValue(Address.getText().toString());
                    mDatabase.child("guardian").child(mynUid).child("detailAddress").setValue(dAddress.getText().toString());
                    mDatabase.child("guardian").child(mynUid).child("birth").setValue(Birth.getText().toString());
                    mDatabase.child("guardian").child(mynUid).child("sex").setValue(sex);
                }catch (@NonNull Exception e){
                    return;
                }

                Toast.makeText(GuardianMyInfoActivity.this, "프로필 수정이 완료되었습니다.", Toast.LENGTH_SHORT).show();

                break;
        }
    }
}