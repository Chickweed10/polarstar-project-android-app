package com.example.polarstarproject;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

public class BirthDialog extends Dialog implements View.OnClickListener{
    private Context mContext;
    private Button birthBtnOk, birthBtnCancle;
    private DatePicker datePicker;
    private BirthDialogListener birthDialogListener;

    private static final String TAG = "BirthDialog"; //로그용 태그

    public BirthDialog(@NonNull Context context) {
        super(context);
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_birth_date_picker);

        birthBtnOk = (Button) findViewById(R.id.birthBtnOk);
        birthBtnCancle = (Button) findViewById(R.id.birthBtnCancle);

        datePicker = (DatePicker)findViewById(R.id.vDatePicker);
        datePicker.setMaxDate(System.currentTimeMillis());

        birthBtnOk.setOnClickListener(this);
        birthBtnCancle.setOnClickListener(this);
    }

    //인터페이스 설정
    interface BirthDialogListener{
        void onOkClicked(int year, int monthOfYear, int dayOfMonth);
        void onCancleClicked();
    }

    //호출할 리스너 초기화
    public void setDialogListener(BirthDialogListener birthDialogListener){
        this.birthDialogListener = birthDialogListener;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.birthBtnOk:
                dismiss(); //다이얼로그 닫기
                int year = datePicker.getYear();
                int month = datePicker.getMonth();
                int day = datePicker.getDayOfMonth();

                Log.w(TAG, "생년월일: " + year + month + day);
                birthDialogListener.onOkClicked(year, month, day);

                break;

            case R.id.birthBtnCancle:
                dismiss(); //다이얼로그 닫기
                break;
        }
    }
}
