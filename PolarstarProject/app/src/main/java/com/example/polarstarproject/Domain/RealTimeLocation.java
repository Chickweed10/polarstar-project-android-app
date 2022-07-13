package com.example.polarstarproject.Domain;

public class RealTimeLocation { //실시간 위치 확인
    public double latitude; //위도
    public double longitude; //경도

    public RealTimeLocation(){

    }

    public RealTimeLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
