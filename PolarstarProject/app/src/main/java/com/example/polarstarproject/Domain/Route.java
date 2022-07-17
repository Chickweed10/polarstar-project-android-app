package com.example.polarstarproject.Domain;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class Route { //장애인 이동경로
    public String nowTime; //현재 시간
    public double latitude; //위도
    public double longitude; //경도

    public Route(){

    }

    public Route(String nowTime, double latitude, double longitude) {
        this.nowTime = nowTime;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("nowTime", nowTime);
        result.put("latitude", latitude);
        result.put("longitude", longitude);

        return result;
    }
}
