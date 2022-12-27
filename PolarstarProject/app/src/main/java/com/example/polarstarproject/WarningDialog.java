package com.example.polarstarproject;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class WarningDialog extends Dialog{
    private Context mContext;

    private TextView dialogTextView;

    private String text;

    public WarningDialog(@NonNull Context context, String text) {
        super(context);
        mContext = context;
        this.text = text;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_warning);

        dialogTextView = (TextView) findViewById(R.id.dialogTextView);
        dialogTextView.setText(text);
    }
}
