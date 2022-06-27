package com.example.polarstarproject.Domain;

public class Connect {
    public String myCode;
    public String counterpartyCode;

    public Connect() {

    }
    public Connect(String myCode, String counterpartyCode) {
        this.myCode = myCode;
        this.counterpartyCode = counterpartyCode;
    }

    public void setMyCode(String myCode) {
        this.myCode = myCode;
    }

    public void setCounterpartyCode(String counterpartyCode) {
        this.counterpartyCode = counterpartyCode;
    }

    public String getMyCode() {
        return myCode;
    }

    public String getCounterpartyCode() {
        return counterpartyCode;
    }
}
