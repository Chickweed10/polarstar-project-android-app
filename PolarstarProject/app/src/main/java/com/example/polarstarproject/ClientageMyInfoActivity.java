package com.example.polarstarproject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
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
import com.example.polarstarproject.Domain.Clientage;
import com.example.polarstarproject.Domain.Connect;
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

import java.util.Timer;
import java.util.TimerTask;


public class ClientageMyInfoActivity extends AppCompatActivity implements View.OnClickListener{
    Toolbar toolbar;

    private DatabaseReference mDatabase;

    ImageView Profl;
    EditText Name, mProflDetailAddress;
    TextView Email, Birth, PhoneNum, Address;
    Button Bt, mProflBtChn, mProflBtPWChage, mProflFdAdd, mProflPNReq;
    String sex,  cSex;
    //Spinner DrDisG;
    RadioGroup rdgGroup;
    RadioButton rdoButton, mProflBtGenderF, mProflBtGenderM;

    private DisconnectDialog disconnectDialog; //연결끊기 다이얼로그 팝업

    private FirebaseAuth mAuth;
    private FirebaseUser user; //firebase 변수
    String myUid;

    private static final String TAG = "MyinfoDuser";

    private FirebaseStorage storage = FirebaseStorage.getInstance();;
    private StorageReference storageRef, riversRef; //firebase DB, Storage 변수
    private Uri imageUri;
    private String pathUri = "profile/default.png"; //프로필 이미지 처리 변수

    private static final int SEARCH_ADDRESS_ACTIVITY = 10000; //우편번호 검색

    Timer timer; //상대방과 매칭 검사를 위한 타이머
    TimerTask timerTask;

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

        disconnectDialog = new DisconnectDialog(this);
        disconnectDialog.requestWindowFeature(Window.FEATURE_NO_TITLE); //타이틀 제거

        Profl = (ImageView) findViewById(R.id.Profl); //프로필 사진

        Name = (EditText) findViewById(R.id.mProflName); //이름
        Email = (TextView) findViewById(R.id.mProflEmail); //이메일
        PhoneNum = (TextView) findViewById(R.id.mProflPhoneNum); //전화번호
        mProflPNReq = (Button) findViewById(R.id.mProflPNReq); // 전화번호 인증 버튼
        Birth = (TextView) findViewById(R.id.mProflBirth); //생년월일

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
                    Glide.with(ClientageMyInfoActivity.this).load(uri).into(Profl);

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
                    Toast.makeText(ClientageMyInfoActivity.this,
                            "이메일을 입력해주세요.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    mAuth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(ClientageMyInfoActivity.this,
                                                "비밀번호 재설정 이메일을 발송했습니다.",
                                                Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(ClientageMyInfoActivity.this,
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

        mProflPNReq.setOnClickListener(new View.OnClickListener() { //인증버튼 눌리면
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MyInfoPhoneNumberVerificationActivity.class);
                startActivity(intent);
                finish(); //화면 이동
            }
        });

        skipScreen();
    }


    /////////////////////////////////////////액티비티 뒤로가기 설정////////////////////////////////////////
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home: { //toolbar의 back키를 눌렀을 때 동작
                Intent intent = new Intent(getApplicationContext(), RealTimeLocationActivity.class);
                startActivity(intent);
                finish(); //화면 이동
                timer.cancel();
                timerTask.cancel(); //타이머 종료

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
        timer.cancel();
        timerTask.cancel(); //타이머 종료
    }

    /////////////////////////////////////////연결 체크////////////////////////////////////////
    private void startDisconnectDialog(){
        RefactoringForegroundService.stopLocationService(this); //포그라운드 서비스 종료
        timer.cancel();
        timerTask.cancel(); //타이머 종료
        disconnectDialog = new DisconnectDialog(this);
        disconnectDialog.setCancelable(false);
        disconnectDialog.show();
        disconnectDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); //모서리 둥글게
    }

    /////////////////////////////////////////연결 여부 확인 후 화면 넘어가기////////////////////////////////////////
    private void skipScreen(){
        timer = new Timer();

        timerTask = new TimerTask() {
            @Override
            public void run() {
                //3초마다 실행
                connectionCheck(); //상대방과 매칭 여부 확인
                Log.w(TAG, "돌아감");
            }
        };
        timer.schedule(timerTask,0,3000);
    }

    /////////////////////////////////////////연결 여부 확인////////////////////////////////////////
    private void connectionCheck(){ //firebase select 조회 함수, 내 connect 테이블 조회
        Query clientageQuery = mDatabase.child("connect").child("clientage").orderByKey().equalTo(user.getUid()); //장애인 테이블 조회
        clientageQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Connect myConnect = new Connect();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    myConnect = ds.getValue(Connect.class);
                }

                if(myConnect.getMyCode() != null && !myConnect.getMyCode().isEmpty()){
                    if(myConnect.getCounterpartyCode() == null){ //상대방이 연결 끊었을 경우
                        if(! ClientageMyInfoActivity.this.isFinishing()){ //finish 오류 방지
                            startDisconnectDialog();
                            timer.cancel();
                            timerTask.cancel(); //타이머 종료
                        }
                        Log.w(TAG, "상대 보호자 없음");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void readUser(String uid) {
        //데이터 읽기
        mDatabase.child("clientage").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Clientage user = snapshot.getValue(Clientage.class);
                Name.setText(user.name);
                Email.setText(user.email);
                PhoneNum.setText(user.phoneNumber);
                Birth.setText(user.birth);
                Address.setText(user.address);
                mProflDetailAddress.setText(user.detailAddress);

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
                                                Toast.makeText(ClientageMyInfoActivity.this, "프로필 삭제가 완료되었습니다.", Toast.LENGTH_SHORT).show();
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
                                            Glide.with(ClientageMyInfoActivity.this).load(uri).into(Profl);
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

                    Toast.makeText(ClientageMyInfoActivity.this, "프로필 변경이 완료되었습니다.", Toast.LENGTH_SHORT).show();
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
                Intent i = new Intent(ClientageMyInfoActivity.this, WebViewActivity.class); //우편번호 검색 화면으로 전환
                startActivityForResult(i, SEARCH_ADDRESS_ACTIVITY);
                break;
            case R.id.mProflBtEdit: //수정 버튼 클릭 시 저장
                try{
                    mDatabase.child("clientage").child(myUid).child("name").setValue(Name.getText().toString());
                    mDatabase.child("clientage").child(myUid).child("address").setValue(Address.getText().toString());
                    mDatabase.child("clientage").child(myUid).child("detailAddress").setValue(mProflDetailAddress.getText().toString());
                }catch (@NonNull Exception e){
                    return;
                }

                Toast.makeText(ClientageMyInfoActivity.this, "프로필 수정이 완료되었습니다.", Toast.LENGTH_SHORT).show();

                break;
        }
    }
}