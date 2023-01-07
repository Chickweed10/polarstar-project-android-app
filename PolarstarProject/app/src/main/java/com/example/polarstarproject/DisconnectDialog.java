package com.example.polarstarproject;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class DisconnectDialog extends Dialog implements View.OnClickListener{
    DisconnectDialog disconnectDialog;
    RealTimeLocationActivity realTimeLocationActivity;
    
    private Context mContext;

    private TextView dialogTextView;
    private Button btn_ok;

    public DisconnectDialog(@NonNull Context context) {
        super(context);
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_authority);

        dialogTextView = (TextView) findViewById(R.id.dialogTextView);
        dialogTextView.setText("상대방과의 연결이 해제되었습니다.");

        btn_ok = (Button) findViewById(R.id.btn_ok);
        btn_ok.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_ok:
                dismiss(); //다이얼로그 닫기
                Intent intent = new Intent(mContext, MainActivity.class); //연결 화면 넘어가기
                mContext.startActivity(intent);
                break;
        }
    }
}
