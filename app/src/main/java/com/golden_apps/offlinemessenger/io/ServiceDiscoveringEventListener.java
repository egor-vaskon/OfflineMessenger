package com.golden_apps.offlinemessenger.io;

public interface ServiceDiscoveringEventListener {
    void onServiceFound(Service service);

    void onDiscoveringStarted();

    void onDiscoveringFinished();
}
