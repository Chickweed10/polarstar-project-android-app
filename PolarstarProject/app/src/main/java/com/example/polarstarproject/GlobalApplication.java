package com.example.polarstarproject;

import android.app.Application;
import com.kakao.sdk.common.KakaoSdk;

public class GlobalApplication extends Application{

    @Override
    public void onCreate() {
        super.onCreate();

        //카카오 sdk 초기화
        KakaoSdk.init(this, "71c576cef191a5f3a9bc15471c9eaea7");
    }
}
