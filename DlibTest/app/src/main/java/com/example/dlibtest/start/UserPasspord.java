package com.example.dlibtest.start;
import org.litepal.crud.DataSupport;

public class UserPasspord extends DataSupport{
    private String user;

    private String password;

    private byte[] headShot;

    public void setUser(String user) {
        this.user = user;
    }

    public String getUser() {
        return user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }


    public byte[] getHeadShot() {return headShot;}

    public void setHeadShot(byte[] headShot) {this.headShot = headShot;}
}
