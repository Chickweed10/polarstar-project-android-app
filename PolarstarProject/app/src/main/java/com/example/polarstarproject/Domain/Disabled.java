package com.example.polarstarproject.Domain;

public class Disabled { //장애인
    public String profileImage;
    public String email;
    public String password;
    public String name;
    public String phoneNumber;
    public String birth;
    public String sex;
    public String address;
    public String detailAddress;
    public String disabilityLevel;

    public Disabled(){

    }

    public Disabled(String profileImage, String email, String password, String name,
                    String phoneNumber, String birth, String sex,
                    String address, String detailAddress, String disabilityLevel) {
        this.profileImage = profileImage;
        this.email = email;
        this.password = password;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.birth = birth;
        this.sex = sex;
        this.address = address;
        this.detailAddress = detailAddress;
        this.disabilityLevel = disabilityLevel;
    }
}
