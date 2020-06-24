package com.golden_apps.offlinemessenger.io;

import com.golden_apps.offlinemessenger.Message;

import java.net.InetAddress;

public interface MessageHandler {
    void handleMessage(InetAddress source,Message message);
}

