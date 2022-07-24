package com.example.polarstarproject.Domain;

public class InOutStatus { //이탈 복귀 상태(플래그)
    public boolean outStatus; //이탈
    public boolean inStatus; //복귀

    public InOutStatus(){

    }

    public InOutStatus(boolean outStatus, boolean inStatus){
        this.outStatus = outStatus;
        this.inStatus = inStatus;
    }

    public boolean getOutStatus() {
        return outStatus;
    }

    public void setOutStatus(boolean outStatus) {
        this.outStatus = outStatus;
    }

    public boolean getInStatus() {
        return inStatus;
    }

    public void setInStatus(boolean inStatus) {
        this.inStatus = inStatus;
    }
}
