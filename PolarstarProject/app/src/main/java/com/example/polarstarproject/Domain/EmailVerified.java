package com.example.polarstarproject.Domain;

public class EmailVerified { //이메일 유효성
    public boolean verified;

    public EmailVerified(){

    }

    public EmailVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
