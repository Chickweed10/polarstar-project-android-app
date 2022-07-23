package com.example.polarstarproject.Domain;

public class DepartureArrivalStatus { //출도착 상태(플래그)
    public boolean departureStatus; //출발
    public boolean arrivalStatus; //도착

    public DepartureArrivalStatus(){

    }

    public DepartureArrivalStatus(boolean departureStatus, boolean arrivalStatus){
        this.departureStatus = departureStatus;
        this.arrivalStatus = arrivalStatus;
    }

    public boolean getDepartureStatus() {
        return departureStatus;
    }

    public void setDepartureStatus(boolean departureStatus) {
        this.departureStatus = departureStatus;
    }

    public boolean getArrivalStatus() {
        return arrivalStatus;
    }

    public void setArrivalStatus(boolean arrivalStatus) {
        this.arrivalStatus = arrivalStatus;
    }
}
