package com.example.polarstarproject;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.CursorLoader;

import com.bumptech.glide.Glide;
import com.example.polarstarproject.Domain.Connect;
import com.example.polarstarproject.Domain.DepartureArrivalStatus;
import com.example.polarstarproject.Domain.Disabled;
import com.example.polarstarproject.Domain.EmailVerified;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DisabledRegisterActivity extends AppCompatActivity implements View.OnClickListener {
    EditText joinEmail, joinPW, joinPWCk, joinName, joinPhoneNum, joinPNCk, joinBirth, joinRoadAddress, joinDetailAddress;
    Spinner joinDrDisG;
    RadioGroup joinBtGender;
    Button joinBtEmailCk, joinPNReq, joinPNReqCk, joinFdAdd, joinBt;
    ImageButton joinBtProfl; //view 변수

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference reference = database.getReference();
    private FirebaseAuth mAuth;
    private FirebaseStorage storage = FirebaseStorage.getInstance();;
    private StorageReference storageRef, riversRef; //firebase DB, Storage 변수

    private Uri imageUri;
    private String pathUri = "profile/default.png"; //프로필 이미지 처리 변수

    private static final String TAG = "DisabledRegister";
    private static final int SEARCH_ADDRESS_ACTIVITY = 10000;

    String VID = "", sex = "남";
    int certificationFlag = 0, emailDuplicateCheckFlag = 0, phoneNumberDuplicateCheckFlag = 0, verificationCodeFlag = 0;
    //인증 여부 판단, 이메일 중복 여부 판단 (0: 기본값, 1: 중복, 2: 통과), 전화번호 중복 여부 판단 (0: 기본값, 1: 중복), 인증번호 요청 예외 처리


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_duser);//회원가입 xml 파일 이름

        mAuth = FirebaseAuth.getInstance();

        joinBtProfl = (ImageButton) findViewById(R.id.joinBtProfl); //프로필 사진
        joinEmail = (EditText) findViewById(R.id.joinEmail); //이메일
        joinBtEmailCk = (Button) findViewById(R.id.joinBtEmailCk); //이메일 중복 확인
        joinPW = (EditText) findViewById(R.id.joinPW); //비밀번호
        joinPWCk = (EditText) findViewById(R.id.joinPWCk); //비밀번호 확인
        joinName = (EditText) findViewById(R.id.joinName); //이름
        joinPhoneNum = (EditText) findViewById(R.id.joinPhoneNum); //전화번호
        joinPNReq = (Button) findViewById(R.id.joinPNReq); //전화번호 인증
        joinPNCk = (EditText) findViewById(R.id.joinPNCk); //인증번호 요청
        joinPNReqCk = (Button) findViewById(R.id.joinPNReqCk); //인증번호 확인
        joinBirth = (EditText) findViewById(R.id.joinBirth); //생년월일
        joinBtGender = findViewById(R.id.joinBtGender); //성별
        joinRoadAddress = (EditText) findViewById(R.id.joinRoadAddress); //도로명 주소
        joinDetailAddress = (EditText) findViewById(R.id.joinDetailAddress); //상세 주소
        joinDrDisG = (Spinner)findViewById(R.id.joinDrDisG); //장애등급
        joinFdAdd = (Button) findViewById(R.id.joinFdAdd); //우편번호 찾기
        joinBt = (Button) findViewById(R.id.joinBt); //회원가입

        joinBtProfl.setOnClickListener(this);
        joinBtEmailCk.setOnClickListener(this);
        joinPNReq.setOnClickListener(this);
        joinPNReqCk.setOnClickListener(this);
        joinFdAdd.setOnClickListener(this);
        joinBt.setOnClickListener(this);

        joinBtGender.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.joinBtGenderM:
                        sex = "남";
                        break;
                    case R.id.joinBtGenderF:
                        sex = "여";
                        break;
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
        UploadTask uploadTask = riversRef.putFile(imageUri);

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "사진 업로드 실패", e);
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.w(TAG, "사진 업로드 성공");
            }
        });
    }

    /////////////////////////////////////////이메일 검사////////////////////////////////////////
    private void emailDuplicateCheck(String email){ //이메일 중복 검사
        emailDuplicateCheckFlag = 0; //이메일 중복 flag 값 초기화
        reference.child("disabled").orderByChild("email").equalTo(email). //장애인 user 검사
                addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {

                } else {
                    emailDuplicateCheckFlag = 1;
                    Toast.makeText(DisabledRegisterActivity.this, "중복된 이메일입니다.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        reference.child("guardian").orderByChild("email").equalTo(email). //보호자 user 검사
                addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (emailDuplicateCheckFlag != 1 && !snapshot.exists()) {
                    emailDuplicateCheckFlag = 2;
                    Toast.makeText(DisabledRegisterActivity.this, "이메일 인증 성공",
                            Toast.LENGTH_SHORT).show();
                } else {
                    emailDuplicateCheckFlag = 1;
                    Toast.makeText(DisabledRegisterActivity.this, "중복된 이메일입니다.",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private boolean emailFormCheck(String email) { //이메일 형식 검사
        String regx = "^[_a-z0-9-]+(.[_a-z0-9-]+)*@(?:\\w+\\.)+\\w+$";
        Pattern pattern = Pattern.compile(regx);
        Matcher matcher = pattern.matcher(email);

        return matcher.matches();
    }

    /*private void emailAvailabilityCheck(){ //이메일 유효성 검사
        mAuth.setLanguageCode("fr");
        mAuth.getCurrentUser().sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "유효성 검사 이메일 전송 완료");
                        }
                        else {
                            Log.d(TAG, "유효성 검사 이메일 전송 실패");
                        }
                    }
                });
    }*/

    /////////////////////////////////////////전화번호////////////////////////////////////////
    private void phoneNumberDuplicateCheck(String phoneNumber){ //전화번호 중복 검사
        phoneNumberDuplicateCheckFlag = 0; //전화번호 중복 flag 값 초기화
        reference.child("disabled").orderByChild("phoneNumber").equalTo(phoneNumber). //장애인 user 검사
                addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {

                } else {
                    phoneNumberDuplicateCheckFlag = 1;
                    Toast.makeText(DisabledRegisterActivity.this, "중복된 전화번호입니다.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        reference.child("guardian").orderByChild("phoneNumber").equalTo(phoneNumber). //보호자 user 검사
                addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (phoneNumberDuplicateCheckFlag != 1 && !snapshot.exists()) {
                    sendVerificationCode();
                } else {
                    phoneNumberDuplicateCheckFlag = 1;
                    Toast.makeText(DisabledRegisterActivity.this, "중복된 전화번호입니다.",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendVerificationCode(){ //인증번호 전송
        PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                Toast.makeText(DisabledRegisterActivity.this, "인증번호가 전송되었습니다. 60초 이내에 입력해주세요.",
                        Toast.LENGTH_LONG).show();
                Log.d(TAG, "인증번호 전송 성공");
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Toast.makeText(DisabledRegisterActivity.this, "인증번호 전송 실패",
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

        String pn = joinPhoneNum.getText().toString(); //국가번호 변환
        if(pn.charAt(0) == '0'){ //앞자리 0으로 시작할 시
            pn = pn.substring(1); //앞자라 0 제외
        }

        mAuth.setLanguageCode("kr");
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber("+82"+ pn)       //핸드폰 번호
                        .setTimeout(60L, TimeUnit.SECONDS) //시간 제한
                        .setActivity(this)
                        .setCallbacks(mCallbacks)
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) { //인증번호 확인
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "인증 성공");
                            Toast.makeText(DisabledRegisterActivity.this, "인증 성공",
                                    Toast.LENGTH_SHORT).show();
                            certificationFlag = 1;
                        } else {
                            Toast.makeText(DisabledRegisterActivity.this, "인증 실패",
                                    Toast.LENGTH_SHORT).show();
                            Log.w(TAG, "인증 실패", task.getException());
                        }
                    }
                });
    }

    /////////////////////////////////////////우편번호 검색////////////////////////////////////////
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(requestCode == 0) { //프로필 사진
            if (resultCode == RESULT_OK) {
                imageUri = intent.getData();
                Glide.with(getApplicationContext())
                        .load(intent.getData())
                        .into(joinBtProfl); //버튼에 이미지 업로드
            }
        }
        else if(requestCode == SEARCH_ADDRESS_ACTIVITY) { //우편번호
            if (resultCode == RESULT_OK) {
                String data = intent.getExtras().getString("data");
                if(data != null) {
                    joinRoadAddress.setText(data);
                }
            }
        }
    }

    /////////////////////////////////////////회원 가입////////////////////////////////////////
    private void signUp(String email, String password, String name,
                        String phoneNumber, String birth, String sex,
                        String address, String detailAddress, String disabilityLevel) {

        //공란 검사 및 예외 처리
        if (!validateForm()) {
            return; //공란, 예외 있으면 return
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(DisabledRegisterActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        //가입 성공시
                        if (task.isSuccessful()) {
                            final String uid = task.getResult().getUser().getUid();

                            if(imageUri != null){ //프로필 설정 했을 시
                                pathUri = "profile/"+uid;
                                firebaseImageUpload(pathUri); //이미지 등록
                            }

                            Disabled disabled = new Disabled(pathUri, email, password, name,
                                    phoneNumber, birth, sex, address, detailAddress, disabilityLevel);
                            reference.child("disabled").child(uid).setValue(disabled);

                            EmailVerified emailVerified = new EmailVerified(false);
                            reference.child("emailverified").child(uid).setValue(emailVerified); //이메일 유효성 초기화

                            DepartureArrivalStatus departureArrivalStatus = new DepartureArrivalStatus(false, false);
                            reference.child("departurearrivalstatus").child(uid).setValue(departureArrivalStatus); //출도착 플래그 초기화

                            //연결 코드 생성
                            createConnectionCode(uid);

                            //가입이 이루어졌을시 가입 화면을 빠져나감.
                            Intent intent = new Intent(DisabledRegisterActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                            Toast.makeText(DisabledRegisterActivity.this, "회원가입 성공", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            task.getException().printStackTrace();
                            Toast.makeText(DisabledRegisterActivity.this, "회원가입 실패", Toast.LENGTH_SHORT).show();
                            return;  //해당 메소드 진행을 멈추고 빠져나감.
                        }

                    }
                });
    }

    /////////////////////////////////////////연결 코드 생성////////////////////////////////////////
    private void createConnectionCode(String uid){
        String myCode;

        Random random = new Random();
        int length = 10; //코드 길이

        StringBuffer newWord = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int choice = random.nextInt(3);
            switch(choice) {
                case 0:
                    newWord.append((char)((int)random.nextInt(25)+97)); //소문자 랜덤 생성
                    break;
                case 1:
                    newWord.append((char)((int)random.nextInt(25)+65)); //대문자 랜덤 생성
                    break;
                case 2:
                    newWord.append((char)((int)random.nextInt(10)+48)); //숫자 랜덤 생성
                    break;
                default:
                    break;
            }
        }
        myCode = newWord.toString();

        reference.child("connect").child("disabled").orderByChild("myCode").equalTo(myCode). //장애인 user 검사
                addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Connect connect = new Connect(myCode, ""); //connect에 내 코드 생성
                    reference.child("connect").child("disabled").child(uid).setValue(connect);
                } else {
                    createConnectionCode(uid);
                    return;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    /////////////////////////////////////////공란 검사 및 예외 처리////////////////////////////////////////
    private boolean validateForm() {
        boolean valid = true;

        String email = joinEmail.getText().toString();
        if (TextUtils.isEmpty(email)) { //이메일 editText가 공란이면
            joinEmail.setError("이메일을 입력해주세요.");
            valid = false;
        } else {
            joinEmail.setError(null);
        }

        if (emailDuplicateCheckFlag == 0) { //이메일이 중복확인 안했을 경우
            joinEmail.setError("이메일 중복확인을 해주세요.");
            valid = false;
        }

        if (emailDuplicateCheckFlag == 1) { //이메일이 중복일 경우
            joinEmail.setError("중복된 이메일입니다.");
            valid = false;
        }
        
        if(emailFormCheck(email) == false){ //이메일 형식 오류인 경우
            joinEmail.setError("잘못된 이메일입니다.");
            valid = false;
        }

        String password = joinPW.getText().toString();
        if (TextUtils.isEmpty(password)) { //비밀번호 editText가 공란이면
            joinPW.setError("비밀번호를 입력해주세요.");
            valid = false;
        } else {
            joinPW.setError(null);
        }

        if (password.length() < 6) { //비밀번호가 6자리 미만일 경우
            joinPW.setError("비밀번호를 6자리 이상 입력해주세요.");
            valid = false;
        } else {
            joinPW.setError(null);
        }

        String passwordCheck = joinPWCk.getText().toString();
        if (TextUtils.isEmpty(passwordCheck)) { //비밀번호 editText가 공란이면
            joinPWCk.setError("비밀번호를 확인해주세요.");
            valid = false;
        } else {
            joinPWCk.setError(null);
        }

        if (password.equals(passwordCheck) == false) { //비밀번호와 비밀번호 확인이 일치하지 않으면
            joinPWCk.setError("비밀번호가 일치하지 않습니다.");
            valid = false;
        } else {
            joinPWCk.setError(null);
        }

        String name = joinName.getText().toString();
        if (TextUtils.isEmpty(name)) { //이름 editText가 공란이면
            joinName.setError("이름을 입력해주세요.");
            valid = false;
        } else {
            joinName.setError(null);
        }

        String phoneNumber = joinPhoneNum.getText().toString();
        if (TextUtils.isEmpty(phoneNumber)) { //전화번호 editText가 공란이면
            joinPhoneNum.setError("전화번호를 입력해주세요.");
            valid = false;
        } else {
            joinPhoneNum.setError(null);
        }

        if(phoneNumberDuplicateCheckFlag == 1) { //중복된 전화번호면
            joinPhoneNum.setError("중복된 전화번호입니다.");
            valid = false;
        }

        if (certificationFlag == 0) { //인증번호가 일치하지 않으면
            joinPNCk.setError("인증번호가 일치하지 않습니다.");
            valid = false;
        } else {
            joinPNCk.setError(null);
        }

        String birth = joinBirth.getText().toString();
        if (TextUtils.isEmpty(birth)) { //생년월일 editText가 공란이면
            joinBirth.setError("생년월일을 입력해주세요.");
            valid = false;
        } else {
            joinBirth.setError(null);
        }

        String roadAddress = joinRoadAddress.getText().toString();
        if (TextUtils.isEmpty(roadAddress)) { //도로명 주소 editText가 공란이면
            joinRoadAddress.setError("도로명 주소를 입력해주세요.");
            valid = false;
        } else {
            joinRoadAddress.setError(null);
        }

        String detailAddress = joinDetailAddress.getText().toString();
        if (TextUtils.isEmpty(detailAddress)) { //상세 주소 editText가 공란이면
            joinDetailAddress.setError("상세 주소를 입력해주세요.");
            valid = false;
        } else {
            joinDetailAddress.setError(null);
        }

        return valid;
    }

    /////////////////////////////////////////버튼 클릭 이벤트////////////////////////////////////////
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.joinBtProfl: //프로필 이미지 등록
                gotoAlbum();
                break;

            case R.id.joinBtEmailCk: //이메일 중복 확인
                emailDuplicateCheck(joinEmail.getText().toString());
                break;

            case R.id.joinPNReq: //인증번호 전송
                phoneNumberDuplicateCheck(joinPhoneNum.getText().toString());
                verificationCodeFlag = 1;
                break;

            case R.id.joinPNReqCk: //인증번호 확인
                if(joinPNCk.getText().toString().isEmpty()){ //공란인 경우
                    Toast.makeText(DisabledRegisterActivity.this, "인증번호를 입력해주세요.",
                            Toast.LENGTH_SHORT).show();
                }
                else if(verificationCodeFlag == 0){ //인증요청을 안한 경우
                    Toast.makeText(DisabledRegisterActivity.this, "인증요청을 해주세요.",
                            Toast.LENGTH_SHORT).show();
                }
                else if(verificationCodeFlag != 0 && !joinPNCk.getText().toString().isEmpty()){
                    signInWithPhoneAuthCredential(PhoneAuthProvider.getCredential(VID, joinPNCk.getText().toString()));
                }

                break;

            case R.id.joinFdAdd: //우편번호 검색
                Intent i = new Intent(DisabledRegisterActivity.this, WebViewActivity.class);
                startActivityForResult(i, SEARCH_ADDRESS_ACTIVITY);
                break;

            case R.id.joinBt: //회원가입
                signUp(joinEmail.getText().toString(), joinPW.getText().toString(), joinName.getText().toString(),
                        joinPhoneNum.getText().toString(), joinBirth.getText().toString(), sex,
                        joinRoadAddress.getText().toString(), joinDetailAddress.getText().toString(), joinDrDisG.getSelectedItem().toString());
        }
    }
}