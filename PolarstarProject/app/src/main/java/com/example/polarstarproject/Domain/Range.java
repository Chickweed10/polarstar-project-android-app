package com.example.polarstarproject.Domain;

public class Range { //범위 설정
    public String rName;
    public double latitude; //위도
    public double longitude; //경도
    public int distance; //반지름

    public Range(){

    }

    public Range(double latitude, double longitude, int distance) {
        this.rName = rName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distance = distance;
    }

    public double getLat() {return latitude;}
    public double getLug() {return longitude;}
    public int getDis() {return distance;}
}
