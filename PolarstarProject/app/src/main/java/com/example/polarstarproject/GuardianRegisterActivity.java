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
import com.example.polarstarproject.Domain.Guardian;
import com.google.android.gms.tasks.OnCompleteListener;
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

public class GuardianRegisterActivity extends AppCompatActivity implements View.OnClickListener{
    EditText joinEmailN, joinPWN, joinPWCkN, joinNameN, joinPhoneNumN, joinPNCkN, joinBirthN, joinRoadAddressN, joinDetailAddressN;
    RadioGroup joinBtGenderN;
    Button joinBtEmailCkN, joinPNReqN, joinPNReqCkN, joinBtN;
    ImageButton joinBtProflN;

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

        joinBtProflN = (ImageButton) findViewById(R.id.joinBtProfl); //프로필 사진
        joinEmailN = (EditText) findViewById(R.id.joinEmail); //이메일
        joinBtEmailCkN = (Button) findViewById(R.id.joinBtEmailCk); //이메일 중복 확인
        joinPWN = (EditText) findViewById(R.id.joinPW); //비밀번호
        joinPWCkN = (EditText) findViewById(R.id.joinPWCk); //비밀번호 확인
        joinNameN = (EditText) findViewById(R.id.joinName); //이름
        joinPhoneNumN = (EditText) findViewById(R.id.joinPhoneNum); //전화번호
        joinPNReqN = (Button) findViewById(R.id.joinPNReq); //전화번호 인증
        joinPNCkN = (EditText) findViewById(R.id.joinPNCk); //인증번호 요청
        joinPNReqCkN = (Button) findViewById(R.id.joinPNReqCk); //인증번호 확인
        joinBirthN = (EditText) findViewById(R.id.joinBirth); //생년월일
        joinBtGenderN = findViewById(R.id.joinBtGender); //성별
        joinRoadAddressN = (EditText) findViewById(R.id.joinRoadAddress); //도로명 주소
        joinDetailAddressN = (EditText) findViewById(R.id.joinDetailAddress); //상세 주소
        joinBtN = (Button) findViewById(R.id.joinBt); //회원가입

        joinBtProflN.setOnClickListener(this);
        joinBtEmailCkN.setOnClickListener(this);
        joinPNReqN.setOnClickListener(this);
        joinPNReqCkN.setOnClickListener(this);
        joinBtN.setOnClickListener(this);

        joinBtGenderN.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
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
                        String address, String detailAddress) {

        Log.d(TAG, "signUp:" + email);

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
                            Guardian guardian = new Guardian(profileImage, email, password, name,
                                    phoneNumber, birth, sex, address, detailAddress);

                            reference.child("users").child("guardian").child(phoneNumber).setValue(guardian);

                            //가입이 이루어져을시 가입 화면을 빠져나감.
                    /*Intent intent = new Intent(DisabledRegisterActivity.this, LoginActivity.class);
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

    //폼 빈칸 체크
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

        String password = joinPWN.getText().toString();
        if (TextUtils.isEmpty(password)) { //비밀번호 editText가 공란이면
            joinPWN.setError("비밀번호를 입력해주세요.");
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
    private void emailDuplicateCheck(String email){ //이메일 중복 검사
        reference.child("users").child("disabled").orderByChild("email").equalTo(email). //장애인 user 검사
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

        reference.child("users").child("Guardian").orderByChild("email").equalTo(email). //보호자 user 검사
                addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
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


    private void sendVerificationCode(){ //인증번호 전송
        PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                verificationCodeFlag = 1;
                Toast.makeText(GuardianRegisterActivity.this, "인증번호가 전송되었습니다. 60초 이내에 입력해주세요.",
                        Toast.LENGTH_SHORT).show();
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

        String pn = joinPhoneNumN.getText().toString();
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
                        .into(joinBtProflN); //버튼에 이미지 업로드
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
                emailDuplicateCheck(joinEmailN.getText().toString());
                break;

            case R.id.joinPNReq: //인증번호 전송
                sendVerificationCode();
                break;

            case R.id.joinPNReqCk: //인증번호 확인
                if(joinPNCkN.getText().toString().isEmpty()){
                    Toast.makeText(GuardianRegisterActivity.this, "인증번호를 입력해주세요.",
                            Toast.LENGTH_SHORT).show();
                }
                else if(verificationCodeFlag == 0){
                    Toast.makeText(GuardianRegisterActivity.this, "인증요청을 해주세요.",
                            Toast.LENGTH_SHORT).show();
                }
                else if(verificationCodeFlag != 0 && !joinPNCkN.getText().toString().isEmpty()){
                    signInWithPhoneAuthCredential(PhoneAuthProvider.getCredential(VID, joinPNCkN.getText().toString()));
                }

                break;

            case R.id.joinBt: //회원가입
                signUp(pathUri, joinEmailN.getText().toString(), joinPWN.getText().toString(), joinNameN.getText().toString(),
                        joinPhoneNumN.getText().toString(), joinBirthN.getText().toString(), sex,
                        joinRoadAddressN.getText().toString(), joinDetailAddressN.getText().toString());
        }
    }
}
