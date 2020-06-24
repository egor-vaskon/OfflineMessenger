package com.golden_apps.offlinemessenger.io;

import android.net.wifi.p2p.WifiP2pDevice;

import java.util.Map;

public class Service {

    private WifiP2pDevice mOwner;
    private String mType;
    private String mInstanceName;
    private Map<String,String> mRecord;
    private int mPort;

    public Service(){
        mOwner = null;
        mType = null;
        mInstanceName = null;
        mRecord = null;
        mPort = 0;
    }

    public WifiP2pDevice getOwner() {
        return mOwner;
    }

    public void setOwner(WifiP2pDevice owner) {
        mOwner = owner;
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        mType = type;
    }

    public String getInstanceName() {
        return mInstanceName;
    }

    public void setInstanceName(String instanceName) {
        mInstanceName = instanceName;
    }

    public Map<String, String> getRecord() {
        return mRecord;
    }

    public void setRecord(Map<String, String> record) {
        mRecord = record;
    }

    public int getPort() {
        return mPort;
    }

    public void setPort(int port) {
        mPort = port;
    }
}
