package com.example.polarstarproject.Domain;

public class TrackingStatus {
    public int status; //(0: 추적가능, 1: 추적불가)

    public TrackingStatus(){

    }

    public TrackingStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
