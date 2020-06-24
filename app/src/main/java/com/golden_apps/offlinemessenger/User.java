package com.golden_apps.offlinemessenger;

import android.net.wifi.p2p.WifiP2pDevice;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class User{

    public static User SELF = new User("",0,"");

    private String mUserName;
    private int mIconId;
    private String mAddress;

    public User(WifiP2pDevice device){
        mUserName = device.deviceName;
        mAddress = device.deviceAddress;
        mIconId = 0;
    }

    public User(String userName, int iconId,String address) {
        mUserName = userName;
        mIconId = iconId;
        mAddress = address;
    }

    public String getUserName() {
        return mUserName;
    }

    public void setUserName(String userName) {
        mUserName = userName;
    }

    public int getIconId() {
        return mIconId;
    }

    public void setIconId(int iconId) {
        mIconId = iconId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return getIconId() == user.getIconId() &&
                Objects.equals(getUserName(), user.getUserName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserName(), getIconId());
    }

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String address) {
        mAddress = address;
    }

    @NotNull
    @Override
    public String toString() {
        return "User{" +
                "mUserName='" + mUserName + '\'' +
                ", mIconId=" + mIconId +
                ", mAddress='" + mAddress + '\'' +
                '}';
    }
}
