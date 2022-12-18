package com.example.polarstarproject.Domain;

public class AddressGeocoding {
    public double addressLatitude; //위도
    public double addressLongitude; //경도

    public AddressGeocoding(){

    }

    public AddressGeocoding(double addressLatitude, double addressLongitude){
        this.addressLatitude = addressLatitude;
        this.addressLongitude = addressLongitude;
    }

    public double getAddressLatitude() {
        return addressLatitude;
    }

    public void setAddressLatitude(double addressLatitude) {
        this.addressLatitude = addressLatitude;
    }

    public double getAddressLongitude() {
        return addressLongitude;
    }

    public void setAddressLongitude(double addressLongitude) {
        this.addressLongitude = addressLongitude;
    }
}
