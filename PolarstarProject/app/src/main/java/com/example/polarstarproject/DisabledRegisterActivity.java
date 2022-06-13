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
import com.example.polarstarproject.Domain.Disabled;
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

import java.io.File;
import java.util.concurrent.TimeUnit;

public class DisabledRegisterActivity extends AppCompatActivity implements View.OnClickListener {
    EditText joinEmail, joinPW, joinPWCk, joinName, joinPhoneNum, joinPNCk, joinBirth, joinRoadAddress, joinDetailAddress;
    Spinner joinDrDisG;
    RadioGroup joinBtGender;
    Button joinBtEmailCk, joinPNReq, joinPNReqCk, joinBt;
    ImageButton joinBtProfl;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference reference = database.getReference();
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    public static final int PICK_FROM_ALBUM = 1;
    private Uri imageUri;
    private String pathUri;
    private File tempFile;

    private static final String TAG = "Register";

    String VID = "", sex = "남";
    int certificationFlag = 0, emailDuplicateCheckFlag = 0, verificationCodeFlag = 0; //인증 여부 판단, 이메일 중복 여부 판단 (0: 기본값, 1: 중복, 2: 통과), 인증번호 요청 예외 처리


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disabled_register);//회원가입 xml 파일 이름

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
        joinBt = (Button) findViewById(R.id.joinBt); //회원가입

        joinBtProfl.setOnClickListener(this);
        joinBtEmailCk.setOnClickListener(this);
        joinPNReq.setOnClickListener(this);
        joinPNReqCk.setOnClickListener(this);
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

    //회원가입
    private void signUp(String profileImage, String email, String password, String name,
                        String phoneNumber, String birth, String sex,
                        String address, String detailAddress, String disabilityLevel) {

        Log.d(TAG, "signUp:" + email);

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
                    Disabled disabled = new Disabled(profileImage, email, password, name,
                            phoneNumber, birth, sex, address, detailAddress, disabilityLevel);

                    reference.child("users").child("disabled").child(phoneNumber).setValue(disabled);

                    //가입이 이루어져을시 가입 화면을 빠져나감.
                    /*Intent intent = new Intent(DisabledRegisterActivity.this, UserSelectActivity.class);
                    startActivity(intent);
                    finish();*/
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

    //폼 빈칸 체크
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

        String password = joinPW.getText().toString();
        if (TextUtils.isEmpty(password)) { //비밀번호 editText가 공란이면
            joinPW.setError("비밀번호를 입력해주세요.");
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
    private void emailDuplicateCheck(String email){ //이메일 중복 검사
        reference.child("users").child("disabled").orderByChild("email").equalTo(email). //장애인 user 검사
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

        reference.child("users").child("Guardian").orderByChild("email").equalTo(email). //보호자 user 검사
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


    private void sendVerificationCode(){ //인증번호 전송
        PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                Toast.makeText(DisabledRegisterActivity.this, "인증번호가 전송되었습니다. 60초 이내에 입력해주세요.",
                        Toast.LENGTH_SHORT).show();
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

        String pn = joinPhoneNum.getText().toString();
        if(pn.charAt(0) == '0'){
            pn = pn.substring(1);
        }

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber("+82"+ pn)       //핸드폰 번호
                        .setTimeout(60L, TimeUnit.SECONDS) //시간 제한
                        .setActivity(this)
                        .setCallbacks(mCallbacks)
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
        mAuth.setLanguageCode("kr");
    }
    private void gotoAlbum() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent, PICK_FROM_ALBUM);
    }

    public String getPath(Uri uri) {

        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader cursorLoader = new CursorLoader(this, uri, proj, null, null, null);

        Cursor cursor = cursorLoader.loadInBackground();
        int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

        cursor.moveToFirst();
        return cursor.getString(index);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 0) {
            if (resultCode == RESULT_OK) {
                Glide.with(getApplicationContext())
                        .load(data.getData())
                        .into(joinBtProfl); //버튼에 이미지 업로드
            }
        }
    }

    @Override
    public void onClick(View v) { //버튼 클릭 이벤트
        switch (v.getId()) {
            case R.id.joinBtProfl: //프로필 이미지 등록
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, 0);

                /*storage = FirebaseStorage.getInstance("gs://polarstarproject-7034b.appspot.com");
                storageRef = storage.getReference();
                storageRef.child("test.png").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        //이미지 로드 성공시

                        Glide.with(getApplicationContext())
                                .load(uri)
                                .into(joinBtProfl);

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        //이미지 로드 실패시
                        Toast.makeText(getApplicationContext(), "실패", Toast.LENGTH_SHORT).show();
                    }
                });*/
                break;
                
            case R.id.joinBtEmailCk: //이메일 중복 확인
                emailDuplicateCheck(joinEmail.getText().toString());
                break;

            case R.id.joinPNReq: //인증번호 전송
                verificationCodeFlag = 1;
                sendVerificationCode();
                break;

            case R.id.joinPNReqCk: //인증번호 확인
                if(joinPNCk.getText().toString().isEmpty()){
                    Toast.makeText(DisabledRegisterActivity.this, "인증번호를 입력해주세요.",
                            Toast.LENGTH_SHORT).show();
                }
                else if(verificationCodeFlag == 0){
                    Toast.makeText(DisabledRegisterActivity.this, "인증요청을 해주세요.",
                            Toast.LENGTH_SHORT).show();
                }
                else if(verificationCodeFlag != 0 && !joinPNCk.getText().toString().isEmpty()){
                    signInWithPhoneAuthCredential(PhoneAuthProvider.getCredential(VID, joinPNCk.getText().toString()));
                }

                break;

            case R.id.joinBt: //회원가입
                signUp(pathUri, joinEmail.getText().toString(), joinPW.getText().toString(), joinName.getText().toString(),
                        joinPhoneNum.getText().toString(), joinBirth.getText().toString(), sex,
                        joinRoadAddress.getText().toString(), joinDetailAddress.getText().toString(), joinDrDisG.getSelectedItem().toString());
        }
    }
}