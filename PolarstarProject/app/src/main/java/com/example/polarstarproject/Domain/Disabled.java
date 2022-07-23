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

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getBirth() {
        return birth;
    }

    public void setBirth(String birth) {
        this.birth = birth;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDetailAddress() {
        return detailAddress;
    }

    public void setDetailAddress(String detailAddress) {
        this.detailAddress = detailAddress;
    }

    public String getDisabilityLevel() {
        return disabilityLevel;
    }

    public void setDisabilityLevel(String disabilityLevel) {
        this.disabilityLevel = disabilityLevel;
    }
}
