package com.example.polarstarproject.Domain;

public class TrackingStatus {
    public boolean status;

    public TrackingStatus(){

    }

    public TrackingStatus(boolean status) {
        this.status = status;
    }

    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
