package com.example.polarstarproject.Domain;

public class SafeZone {

    String name;
    String address;
    int dis;
    //int image_path;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getDis() {
        return dis;
    }

    public void setDis(int name) {
        this.dis = name;
    }
/*
    public int getImage_path() {
        return image_path;
    }

    public void setImage_path(int image_path) {
        this.image_path = image_path;
    }

 */
    public SafeZone(String name, String address, int dis) {
        this.name = name;
        this.address = address;
        this.dis = dis;
        //this.image_path = image_path;
    }

    public SafeZone(){

    }
}
