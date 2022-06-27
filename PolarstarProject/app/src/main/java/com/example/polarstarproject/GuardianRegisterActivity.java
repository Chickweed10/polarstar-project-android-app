package com.example.polarstarproject;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
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
import com.example.polarstarproject.Domain.Disabled;
import com.example.polarstarproject.Domain.Guardian;
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
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuardianRegisterActivity extends AppCompatActivity implements View.OnClickListener{
    EditText joinEmailN, joinPWN, joinPWCkN, joinNameN, joinPhoneNumN, joinPNCkN, joinBirthN, joinRoadAddressN, joinDetailAddressN;
    RadioGroup joinBtGenderN;
    Button joinBtEmailCkN, joinPNReqN, joinPNReqCkN, joinFdAddN, joinBtN;
    ImageButton joinBtProflN;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference reference = database.getReference();
    private FirebaseAuth mAuth;
    private FirebaseStorage storage = FirebaseStorage.getInstance();;
    private StorageReference storageRef, riversRef;

    private Uri imageUri;
    private String pathUri = "profile/default.png";

    private static final String TAG = "GuardianRegister";
    private static final int SEARCH_ADDRESS_ACTIVITY = 10000;

    String VID = "", sex = "남";
    int certificationFlag = 0, emailDuplicateCheckFlag = 0, phoneNumberDuplicateCheckFlag = 0, verificationCodeFlag = 0;
    //인증 여부 판단, 이메일 중복 여부 판단 (0: 기본값, 1: 중복, 2: 통과), 전화번호 중복 여부 판단 (0: 기본값, 1: 중복), 인증번호 요청 예외 처리


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_duser_n);//회원가입 xml 파일 이름

        mAuth = FirebaseAuth.getInstance();

        joinBtProflN = (ImageButton) findViewById(R.id.joinBtProflN); //프로필 사진
        joinEmailN = (EditText) findViewById(R.id.joinEmailN); //이메일
        joinBtEmailCkN = (Button) findViewById(R.id.joinBtEmailCkN); //이메일 중복 확인
        joinPWN = (EditText) findViewById(R.id.joinPWN); //비밀번호
        joinPWCkN = (EditText) findViewById(R.id.joinPWCkN); //비밀번호 확인
        joinNameN = (EditText) findViewById(R.id.joinNameN); //이름
        joinPhoneNumN = (EditText) findViewById(R.id.joinPhoneNumN); //전화번호
        joinPNReqN = (Button) findViewById(R.id.joinPNReqN); //전화번호 인증
        joinPNCkN = (EditText) findViewById(R.id.joinPNCkN); //인증번호 요청
        joinPNReqCkN = (Button) findViewById(R.id.joinPNReqCkN); //인증번호 확인
        joinBirthN = (EditText) findViewById(R.id.joinBirthN); //생년월일
        joinBtGenderN = findViewById(R.id.joinBtGenderN); //성별
        joinRoadAddressN = (EditText) findViewById(R.id.joinRoadAddressN); //도로명 주소
        joinDetailAddressN = (EditText) findViewById(R.id.joinDetailAddressN); //상세 주소
        joinFdAddN = (Button) findViewById(R.id.joinFdAddN); //우편번호 찾기
        joinBtN = (Button) findViewById(R.id.joinBtN); //회원가입

        joinBtProflN.setOnClickListener(this);
        joinBtEmailCkN.setOnClickListener(this);
        joinPNReqN.setOnClickListener(this);
        joinPNReqCkN.setOnClickListener(this);
        joinFdAddN.setOnClickListener(this);
        joinBtN.setOnClickListener(this);

        joinBtGenderN.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.joinBtGenderMN:
                        sex = "남";
                        break;
                    case R.id.joinBtGenderFN:
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

    /////////////////////////////////////////이메일 중복 검사////////////////////////////////////////
    private void emailDuplicateCheck(String email){
        emailDuplicateCheckFlag = 0; //이메일 중복 flag 값 초기화
        reference.child("disabled").orderByChild("email").equalTo(email). //장애인 user 검사
                addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {

                } else {
                    emailDuplicateCheckFlag = 1;
                    Toast.makeText(GuardianRegisterActivity.this, "중복된 이메일입니다.",
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
                    Toast.makeText(GuardianRegisterActivity.this, "이메일 인증 성공",
                            Toast.LENGTH_SHORT).show();
                } else {
                    emailDuplicateCheckFlag = 1;
                    Toast.makeText(GuardianRegisterActivity.this, "중복된 이메일입니다.",
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
                    Toast.makeText(GuardianRegisterActivity.this, "중복된 전화번호입니다.",
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
                    Toast.makeText(GuardianRegisterActivity.this, "중복된 전화번호입니다.",
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
                Toast.makeText(GuardianRegisterActivity.this, "인증번호가 전송되었습니다. 60초 이내에 입력해주세요.",
                        Toast.LENGTH_LONG).show();
                Log.d(TAG, "인증번호 전송 성공");
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Toast.makeText(GuardianRegisterActivity.this, "인증번호 전송 실패",
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

        String pn = joinPhoneNumN.getText().toString(); //국가번호 변환
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
                            Toast.makeText(GuardianRegisterActivity.this, "인증 성공",
                                    Toast.LENGTH_SHORT).show();
                            certificationFlag = 1;
                        } else {
                            Toast.makeText(GuardianRegisterActivity.this, "인증 실패",
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
                        .into(joinBtProflN); //버튼에 이미지 업로드
            }
        }
        else if(requestCode == SEARCH_ADDRESS_ACTIVITY) { //우편번호
            if (resultCode == RESULT_OK) {
                String data = intent.getExtras().getString("data");
                if(data != null) {
                    joinRoadAddressN.setText(data);
                }
            }
        }

    }

    /////////////////////////////////////////회원 가입////////////////////////////////////////
    private void signUp(String email, String password, String name,
                        String phoneNumber, String birth, String sex,
                        String address, String detailAddress) {

        //공란 검사 및 예외 처리
        if (!validateForm()) {
            return; //공란, 예외 있으면 return
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(GuardianRegisterActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        //가입 성공시
                        if (task.isSuccessful()) {
                            final String uid = task.getResult().getUser().getUid();

                            if(imageUri != null){ //프로필 설정 했을 시
                                pathUri = "profile/"+uid;
                                firebaseImageUpload(pathUri); //이미지 등록
                            }
                            Guardian guardian = new Guardian(pathUri, email, password, name,
                                    phoneNumber, birth, sex, address, detailAddress);

                            reference.child("guardian").child(uid).setValue(guardian);

                            //연결 코드 생성
                            createConnectionCode(uid);

                            //가입이 이루어졌을시 가입 화면을 빠져나감.
                            /*Intent intent = new Intent(GuardianRegisterActivity.this, GuardianRegisterActivity.class);
                            startActivity(intent);
                            finish();*/
                            Toast.makeText(GuardianRegisterActivity.this, "회원가입 성공", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            task.getException().printStackTrace();
                            Toast.makeText(GuardianRegisterActivity.this, "회원가입 실패", Toast.LENGTH_SHORT).show();
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

        reference.child("connect").child("guardian").orderByChild("myCode").equalTo(myCode). //장애인 user 검사
                addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Connect connect = new Connect(myCode, ""); //connect에 내 코드 생성
                    reference.child("connect").child("guardian").child(uid).setValue(connect);
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

        String email = joinEmailN.getText().toString();
        if (TextUtils.isEmpty(email)) { //이메일 editText가 공란이면
            joinEmailN.setError("이메일을 입력해주세요.");
            valid = false;
        } else {
            joinEmailN.setError(null);
        }

        if (emailDuplicateCheckFlag == 0) { //이메일이 중복확인 안했을 경우
            joinEmailN.setError("이메일 중복확인을 해주세요.");
            valid = false;
        }

        if (emailDuplicateCheckFlag == 1) { //이메일이 중복일 경우
            joinEmailN.setError("중복된 이메일입니다.");
            valid = false;
        }

        if(emailFormCheck(email) == false){ //이메일 형식 오류인 경우
            joinEmailN.setError("잘못된 이메일입니다.");
            valid = false;
        }

        String password = joinPWN.getText().toString();
        if (TextUtils.isEmpty(password)) { //비밀번호 editText가 공란이면
            joinPWN.setError("비밀번호를 입력해주세요.");
            valid = false;
        } else {
            joinPWN.setError(null);
        }

        if (password.length() < 6) { //비밀번호가 6자리 미만일 경우
            joinPWN.setError("비밀번호를 6자리 이상 입력해주세요.");
            valid = false;
        } else {
            joinPWN.setError(null);
        }

        String passwordCheck = joinPWCkN.getText().toString();
        if (TextUtils.isEmpty(passwordCheck)) { //비밀번호 editText가 공란이면
            joinPWCkN.setError("비밀번호를 확인해주세요.");
            valid = false;
        } else {
            joinPWCkN.setError(null);
        }

        if (password.equals(passwordCheck) == false) { //비밀번호와 비밀번호 확인이 일치하지 않으면
            joinPWCkN.setError("비밀번호가 일치하지 않습니다.");
            valid = false;
        } else {
            joinPWCkN.setError(null);
        }

        String name = joinNameN.getText().toString();
        if (TextUtils.isEmpty(name)) { //이름 editText가 공란이면
            joinNameN.setError("이름을 입력해주세요.");
            valid = false;
        } else {
            joinNameN.setError(null);
        }

        String phoneNumber = joinPhoneNumN.getText().toString();
        if (TextUtils.isEmpty(phoneNumber)) { //전화번호 editText가 공란이면
            joinPhoneNumN.setError("전화번호를 입력해주세요.");
            valid = false;
        } else {
            joinPhoneNumN.setError(null);
        }

        if(phoneNumberDuplicateCheckFlag == 1) { //중복된 전화번호면
            joinPhoneNumN.setError("중복된 전화번호입니다.");
            valid = false;
        }

        if (certificationFlag == 0) { //인증번호가 일치하지 않으면
            joinPNCkN.setError("인증번호가 일치하지 않습니다.");
            valid = false;
        } else {
            joinPNCkN.setError(null);
        }

        String birth = joinBirthN.getText().toString();
        if (TextUtils.isEmpty(birth)) { //생년월일 editText가 공란이면
            joinBirthN.setError("생년월일을 입력해주세요.");
            valid = false;
        } else {
            joinBirthN.setError(null);
        }

        String roadAddress = joinRoadAddressN.getText().toString();
        if (TextUtils.isEmpty(roadAddress)) { //도로명 주소 editText가 공란이면
            joinRoadAddressN.setError("도로명 주소를 입력해주세요.");
            valid = false;
        } else {
            joinRoadAddressN.setError(null);
        }

        String detailAddress = joinDetailAddressN.getText().toString();
        if (TextUtils.isEmpty(detailAddress)) { //상세 주소 editText가 공란이면
            joinDetailAddressN.setError("상세 주소를 입력해주세요.");
            valid = false;
        } else {
            joinDetailAddressN.setError(null);
        }

        return valid;
    }

    /////////////////////////////////////////버튼 클릭 이벤트////////////////////////////////////////
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.joinBtProflN: //프로필 이미지 등록
                gotoAlbum();
                break;

            case R.id.joinBtEmailCkN: //이메일 중복 확인
                emailDuplicateCheck(joinEmailN.getText().toString());
                break;

            case R.id.joinPNReqN: //인증번호 전송
                phoneNumberDuplicateCheck(joinPhoneNumN.getText().toString());
                verificationCodeFlag = 1;
                break;

            case R.id.joinPNReqCkN: //인증번호 확인
                if(joinPNCkN.getText().toString().isEmpty()){ //공란인 경우
                    Toast.makeText(GuardianRegisterActivity.this, "인증번호를 입력해주세요.",
                            Toast.LENGTH_SHORT).show();
                }
                else if(verificationCodeFlag == 0){ //인증요청을 안한 경우
                    Toast.makeText(GuardianRegisterActivity.this, "인증요청을 해주세요.",
                            Toast.LENGTH_SHORT).show();
                }
                else if(verificationCodeFlag != 0 && !joinPNCkN.getText().toString().isEmpty()){
                    signInWithPhoneAuthCredential(PhoneAuthProvider.getCredential(VID, joinPNCkN.getText().toString()));
                }

                break;

            case R.id.joinFdAddN: //우편번호 검색
                Intent i = new Intent(GuardianRegisterActivity.this, WebViewActivity.class);
                startActivityForResult(i, SEARCH_ADDRESS_ACTIVITY);
                break;

            case R.id.joinBtN: //회원가입
                signUp(joinEmailN.getText().toString(), joinPWN.getText().toString(), joinNameN.getText().toString(),
                        joinPhoneNumN.getText().toString(), joinBirthN.getText().toString(), sex,
                        joinRoadAddressN.getText().toString(), joinDetailAddressN.getText().toString());
        }
    }
}
