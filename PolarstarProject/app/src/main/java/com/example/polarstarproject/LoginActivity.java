package com.example.polarstarproject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    public static Context context_main; // 다른 엑티비티에서의 접근을 위해 사용
    public String setId, setPassword;

    Button mLoginBtn, mRePasswordBtn, mReFindEmailBtn;
    EditText mEmailText, mPasswordText;
    CheckBox autoCheck;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        context_main = this;

        firebaseAuth = FirebaseAuth.getInstance();

        //등록하기
        mLoginBtn = findViewById(R.id.btn_login);
        mReFindEmailBtn = findViewById(R.id.btn_findEmail);
        mRePasswordBtn = findViewById(R.id.btn_rsPW);
        mEmailText = findViewById(R.id.lgnEmail);
        mPasswordText = findViewById(R.id.lgnPW);
        autoCheck = findViewById(R.id.cb_save);

        // SharedPreferences 사용해서 앱에 데이터 저장&불러오기기
        SharedPreferences auto = getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        SharedPreferences.Editor autoLoginEdit = auto.edit();

        // 로그인 데이터 불러오기
        setId = auto.getString("Id", null);
        setPassword = auto.getString("Password", null);
        mEmailText.setText(setId);
        mPasswordText.setText(setPassword);

        //로그인 버튼이 눌리면
        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mEmailText.getText().toString().trim();
                String pwd = mPasswordText.getText().toString().trim();
                firebaseAuth.signInWithEmailAndPassword(email, pwd)
                        .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(LoginActivity.this,
                                            "로그인 성공",
                                            Toast.LENGTH_SHORT).show();
                                    if(autoCheck.isChecked()) {
                                        if(setId == null && setPassword == null) {
                                            // 로그인 데이터 저장
                                            autoLoginEdit.putString("Id", mEmailText.getText().toString().trim());
                                            autoLoginEdit.putString("Password", mPasswordText.getText().toString().trim());
                                            autoLoginEdit.commit(); // commit 해야지만 저장됨

                                            autoCheck.setChecked(true); //체크박스는 여전히 체크 표시 하도록 셋팅
                                        }
                                    }
                                    Intent intent = new Intent(LoginActivity.this, ConnectActivity.class);
                                    startActivity(intent);

                                } else {
                                    Toast.makeText(LoginActivity.this,
                                            "로그인 오류",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

            }
        });

        //이메일 찾기 버튼이 눌리면
        mReFindEmailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mEmailText.setText(setId); // 자동로그인 해놨을 때만 가능
                Intent intent = new Intent(getApplicationContext(), FindActivity.class);
                startActivity(intent);

            }
        });

        //재설정 버튼이 눌리면
        mRePasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mEmailText.getText().toString().trim();
                if(email.isEmpty()) {
                    Toast.makeText(LoginActivity.this,
                            "이메일을 입력해주세요.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    firebaseAuth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(LoginActivity.this,
                                                "비밀번호 재설정 이메일을 발송했습니다.",
                                                Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(LoginActivity.this,
                                                "가입한 이메일을 입력하세요.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });

    }
}
