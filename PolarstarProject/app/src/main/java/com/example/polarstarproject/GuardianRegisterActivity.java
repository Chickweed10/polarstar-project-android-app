package com.example.polarstarproject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.polarstarproject.Domain.Connect;
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

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;

//보호자 회원가입
public class GuardianRegisterActivity extends AppCompatActivity implements View.OnClickListener{
    Toolbar toolbar;
    BirthDialog birthDialog; //생년월일

    EditText joinEmailN, joinPWN, joinPWCkN, joinNameN, joinPhoneNumN, joinPNCkN, joinRoadAddressN, joinDetailAddressN;
    RadioGroup joinBtGenderN;
    Button joinBtEmailCkN, joinPNReqN, joinPNReqCkN, joinFdAddN, joinBtN, joinBtProflN, joinBirthN;
    CircleImageView ivRproflN; //UI 변수

    String birth = null; //생년월일

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference reference = database.getReference();
    private FirebaseAuth mAuth;
    private FirebaseStorage storage = FirebaseStorage.getInstance();;
    private StorageReference storageRef, riversRef; //firebase DB, Storage 변수

    private Uri imageUri;
    private String pathUri = "profile/default.png"; //프로필 이미지 처리 변수

    private static final String TAG = "GuardianRegister";//로그용 태그

    private static final int SEARCH_ADDRESS_ACTIVITY = 10000; //우편번호 검색

    String VID = "", sex = "남";
    int certificationFlag = 0, emailDuplicateCheckFlag = 0, phoneNumberDuplicateCheckFlag = 0, verificationCodeFlag = 0;
    //인증 여부 판단, 이메일 중복 여부 판단 (0: 기본값, 1: 중복, 2: 통과), 전화번호 중복 여부 판단 (0: 기본값, 1: 중복), 인증번호 요청 예외 처리


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_duser_n);//회원가입 xml 파일 이름

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //뒤로가기
        getSupportActionBar().setTitle("회원가입");

        mAuth = FirebaseAuth.getInstance();

        joinBtProflN = (Button) findViewById(R.id.joinBtProflN);
        ivRproflN = (CircleImageView) findViewById(R.id.iv_RproflN); //프로필 사진
        joinEmailN = (EditText) findViewById(R.id.joinEmailN); //이메일
        joinBtEmailCkN = (Button) findViewById(R.id.joinBtEmailCkN); //이메일 중복 확인
        joinPWN = (EditText) findViewById(R.id.joinPWN); //비밀번호
        joinPWCkN = (EditText) findViewById(R.id.joinPWCkN); //비밀번호 확인
        joinNameN = (EditText) findViewById(R.id.joinNameN); //이름
        joinPhoneNumN = (EditText) findViewById(R.id.joinPhoneNumN); //전화번호
        joinPNReqN = (Button) findViewById(R.id.joinPNReqN); //전화번호 인증
        joinPNCkN = (EditText) findViewById(R.id.joinPNCkN); //인증번호 요청
        joinPNReqCkN = (Button) findViewById(R.id.joinPNReqCkN); //인증번호 확인
        joinBirthN = (Button) findViewById(R.id.joinBirthN); //생년월일
        joinBirthN.setText("1900-01-01");
        joinBtGenderN = findViewById(R.id.joinBtGenderN); //성별
        joinRoadAddressN = (EditText) findViewById(R.id.joinRoadAddressN); //도로명 주소
        joinDetailAddressN = (EditText) findViewById(R.id.joinDetailAddressN); //상세 주소
        joinFdAddN = (Button) findViewById(R.id.joinFdAddN); //우편번호 찾기
        joinBtN = (Button) findViewById(R.id.joinBtN); //회원가입

        joinPNCkN.setEnabled(false); //초기 인증번호란 비활성화

        joinBtProflN.setOnClickListener(this);
        joinBtEmailCkN.setOnClickListener(this);
        joinPNReqN.setOnClickListener(this);
        joinPNReqCkN.setOnClickListener(this);
        joinFdAddN.setOnClickListener(this);
        joinBtN.setOnClickListener(this);
        joinBirthN.setOnClickListener(this);

        birthDialog = new BirthDialog(this);
        birthDialog.requestWindowFeature(Window.FEATURE_NO_TITLE); //타이틀 제거

        joinBtGenderN.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() { //성별 버튼 클릭 시
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.joinBtGenderMN: //남자 클릭시
                        sex = "남";
                        break;
                    case R.id.joinBtGenderFN: //여자 클릭시
                        sex = "여";
                        break;
                }
            }
        });
    }

    /////////////////////////////////////////액티비티 뒤로가기 설정////////////////////////////////////////
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home: { //toolbar의 back키를 눌렀을 때 동작
                Intent intent = new Intent(getApplicationContext(), UserSelectActivity.class);
                startActivity(intent);
                finish(); //사용자 선택 화면으로 이동

                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed() { //뒤로가기 했을 때
        Intent intent = new Intent(getApplicationContext(), UserSelectActivity.class);
        startActivity(intent);
        finish(); //사용자 선택 화면으로 이동
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

    /////////////////////////////////////////이메일 중복 검사////////////////////////////////////////
    private void emailDuplicateCheck(String email){
        emailDuplicateCheckFlag = 0; //이메일 중복 flag 값 초기화
        reference.child("clientage").orderByChild("email").equalTo(email). //장애인 user 검사
                addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {

                } else { //이메일 중복시
                    emailDuplicateCheckFlag = 1; //이메일 중복 flag 값 1로 변경
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
                if (emailDuplicateCheckFlag != 1 && !snapshot.exists()) { //장애인 user 이메일 중복 검사 통과 & 보호자 user 이메일 중복 검사 통과시
                    emailDuplicateCheckFlag = 2; //이메일 중복 flag 값 2로 변경
                    Toast.makeText(GuardianRegisterActivity.this, "이메일 인증 성공",
                            Toast.LENGTH_SHORT).show();
                } else {
                    emailDuplicateCheckFlag = 1; //이메일 중복 flag 값 1로 변경
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

        if(matcher.matches()){
            emailDuplicateCheck(email);
        }
        else {
            joinEmailN.setError("잘못된 이메일 형식입니다.");
        }

        return matcher.matches();
    }

    /////////////////////////////////////////전화번호////////////////////////////////////////
    private void phoneNumberDuplicateCheck(String phoneNumber){ //전화번호 공백 & 중복 검사
        phoneNumberDuplicateCheckFlag = 0; //전화번호 중복 flag 값 초기화

        if(phoneNumber == null || phoneNumber.isEmpty()){
            Toast.makeText(GuardianRegisterActivity.this, "전화번호를 입력해주세요.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        reference.child("clientage").orderByChild("phoneNumber").equalTo(phoneNumber). //장애인 user 검사
                addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {

                } else { //전화번호 중복시
                    phoneNumberDuplicateCheckFlag = 1; //전화번호 중복 flag 값 1로 변경
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
                if (phoneNumberDuplicateCheckFlag != 1 && !snapshot.exists()) { //장애인 user 전화번호 중복 검사 통과 & 보호자 user 전화번호 중복 검사 통과
                    sendVerificationCode(); //인증번호 보내기
                } else { //전화번호 중복시
                    phoneNumberDuplicateCheckFlag = 1; //전화번호 중복 flag 값 1로 변경
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
                joinPhoneNumN.setEnabled(false); //전화번호란 비활성화
                joinPNCkN.setEnabled(true); //인증번호란 활성화
                Toast.makeText(GuardianRegisterActivity.this, "인증번호가 전송되었습니다." + '\n' + "60초 이내에 입력해주세요.",
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
                joinPhoneNumN.setEnabled(false); //전화번호란 비활성화
                joinPNCkN.setEnabled(true); //인증번호란 활성화
                Toast.makeText(GuardianRegisterActivity.this, "인증번호가 전송되었습니다." + '\n' + "60초 이내에 입력해주세요.",
                        Toast.LENGTH_LONG).show();
                Log.d(TAG, "onCodeSent:" + verificationId);
                VID = verificationId;
            }
        };

        String pn = joinPhoneNumN.getText().toString(); //국가번호 변환
        if(pn.charAt(0) == '0'){ //앞자리 0으로 시작할 시
            pn = pn.substring(1); //앞자라 0 제외
        }

        mAuth.setLanguageCode("kr"); //인증문자 메시지 언어 한국어로
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
                        if (task.isSuccessful()) { //인증번호 일치
                            Log.d(TAG, "인증 성공");
                            Toast.makeText(GuardianRegisterActivity.this, "인증 성공",
                                    Toast.LENGTH_SHORT).show();
                            joinPNCkN.setEnabled(false); //초기 인증번호란 비활성화
                            certificationFlag = 1; //인증번호 flag 값 1로 변경
                        } else { //인증번호 불일치
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
                        .into(ivRproflN); //버튼에 이미지 삽입
            }
        }
        else if(requestCode == SEARCH_ADDRESS_ACTIVITY) { //우편번호 등록
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

        mAuth.createUserWithEmailAndPassword(email, password) //이메일, 비밀번호 회원가입
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
                                    phoneNumber, birth, sex, address, detailAddress); //보호자 객체 생성
                            reference.child("guardian").child(uid).setValue(guardian); //DB에 보호자 정보 삽입

                            //연결 코드 생성
                            createConnectionCode(uid);

                            //가입이 이루어졌을시 로그인 화면으로 이동
                            Intent intent = new Intent(GuardianRegisterActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                            Toast.makeText(GuardianRegisterActivity.this, "회원가입 성공", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            task.getException().printStackTrace();
                            Toast.makeText(GuardianRegisterActivity.this, "회원가입 실패", Toast.LENGTH_SHORT).show();
                            return;  //해당 메소드 진행을 멈추고 빠져나감
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

        reference.child("connect").child("guardian").orderByChild("myCode").equalTo(myCode). //보호자 user 연결 코드 중복 검사
                addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Connect connect = new Connect(myCode, null); //connect에 내 코드 생성
                    reference.child("connect").child("guardian").child(uid).setValue(connect);
                } else { //연결 코드 중복시
                    createConnectionCode(uid); //연결 코드 재생성
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

        if (birth == null) { //생년월일 선택안했을 경운
            Toast.makeText(GuardianRegisterActivity.this, "생년월일을 선택해주세요.", Toast.LENGTH_SHORT).show();
            valid = false;
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

            case R.id.joinBtEmailCkN: //이메일 형식 & 중복 확인
                emailFormCheck(joinEmailN.getText().toString());
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

            case R.id.joinBirthN: //생년월일
                birthDialog = new BirthDialog(this);
                birthDialog.setDialogListener(new BirthDialog.BirthDialogListener() {
                    @Override
                    public void onOkClicked(int year, int monthOfYear, int dayOfMonth) { //선택한 생년월일 가져오기
                        birth = Integer.toString(year)+String.format("%02d", monthOfYear+1)+String.format("%02d", dayOfMonth);
                        String setBirth = Integer.toString(year) + "-" + String.format("%02d", monthOfYear+1) + "-" + String.format("%02d", dayOfMonth);
                        joinBirthN.setText(setBirth); //선택한 생년월일로 버튼 text 변경
                    }

                    @Override
                    public void onCancleClicked() {

                    }
                });
                birthDialog.show();
                break;

            case R.id.joinBtN: //회원가입
                signUp(joinEmailN.getText().toString(), joinPWN.getText().toString(), joinNameN.getText().toString(),
                        joinPhoneNumN.getText().toString(), birth, sex,
                        joinRoadAddressN.getText().toString(), joinDetailAddressN.getText().toString());
        }
    }
}
