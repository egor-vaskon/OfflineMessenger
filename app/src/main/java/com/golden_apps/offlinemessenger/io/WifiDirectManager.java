package com.golden_apps.offlinemessenger.io;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.drm.DrmStore;
import android.net.IpPrefix;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.golden_apps.offlinemessenger.Message;
import com.golden_apps.offlinemessenger.R;
import com.golden_apps.offlinemessenger.User;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import me.alexpanov.net.FreePortFinder;

public class WifiDirectManager {

    private static WifiDirectManager sInstance;

    private Context mContext;
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private IntentFilter mIntentFilter;
    private BroadcastReceiver mReceiver;
    private int mStatus;
    private Server mServer;
    private Client mClient;
    private MessageHandler mMessageHandler;
    private InetAddress mGroupOwnerAddress;
    private ServiceScanner mServiceScanner;
    private ConnectionListener mConnectionListener;
    private MessageReceivingHandler mReceivingHandler;
    private ErrorListener mErrorListener;
    private WifiManager mWifiManager;
    private int mPort;

    public static final String SERVICE_REGISTRATION_TYPE = "_wifidirectchat._tcp";
    public static final String SERVICE_INSTANCE = "_wifidirectchatapp";

    public static final int STATUS_NONE = 0;
    public static final int STATUS_CLIENT = 1;
    public static final int STATUS_GROUP_OWNER = 2;

    public static final String TAG = "WifiDirectManager";

    private class WifiDirectBroadcastReceiver extends BroadcastReceiver{

        public WifiDirectBroadcastReceiver(){

        }

        @Override
        public void onReceive(Context context, final Intent intent) {
            String action = intent.getAction();

            if(action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)){
                mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                    @Override
                    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
                        NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                        if(networkInfo.isConnected()){
                            Log.d(TAG,"Connected.");

                            if(wifiP2pInfo.isGroupOwner && wifiP2pInfo.groupFormed){
                                mStatus = STATUS_GROUP_OWNER;

                                startServer(mMessageHandler,mReceivingHandler);
                            }
                            else if(wifiP2pInfo.groupFormed){
                                mStatus = STATUS_CLIENT;
                                mGroupOwnerAddress = wifiP2pInfo.groupOwnerAddress;

                                addClient(mMessageHandler,mReceivingHandler);
                            }

                            if(mConnectionListener != null) mConnectionListener.onConnected();
                        }
                        else{
                            if(mConnectionListener != null){
                                if(mServer != null || mClient != null){

                                    Log.d(TAG,"Disconnected");

                                    mServer = null;
                                    mClient = null;


                                    mConnectionListener.onDisconnected();
                                }
                            }
                        }
                    }
                });
            }
            else if (action.equals(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)){
                if(!isWifiEnabled()){
                    mConnectionListener.onDisconnected();
                }
            }
        }
    }

    private WifiDirectManager(Context context){
        mContext = context;

        mManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(mContext, mContext.getMainLooper(), null);

        mReceiver = new WifiDirectBroadcastReceiver();
        mServiceScanner = new ServiceScanner(mContext,mManager,mChannel);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mWifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mStatus = STATUS_NONE;
    }

    public static void initInstance(Context context){
        sInstance = new WifiDirectManager(context);
    }

    public static WifiDirectManager getInstance(){
        return sInstance;
    }

    public void registerReceiver(){
        mContext.registerReceiver(mReceiver,mIntentFilter);
    }

    public void unregisterReceiver(){
        try{
            mContext.unregisterReceiver(mReceiver);
        }
        catch (Exception ex){
            //Ignore
        }
    }

    public void connect(String address,MessageHandler messageHandler
            ,ConnectionListener connectionListener){

       if(mMessageHandler != null) mMessageHandler = messageHandler;
       if(connectionListener != null) mConnectionListener = connectionListener;

       Service service = mServiceScanner.getFoundServices().get(address);
       connect(service,0);
    }

    private void connect(final Service service, int status){
        mServiceScanner.removeServiceRequest(null);

        if(service == null){
            Log.e(TAG,"Cannot connect to group, because service is null.");

            mErrorListener.onError();

            return;
        }

        final WifiP2pConfig config = new WifiP2pConfig();

        mPort = service.getPort();

        config.deviceAddress = service.getOwner().deviceAddress;
        config.groupOwnerIntent = status;
        config.wps.setup = WpsInfo.PBC;

        mServiceScanner.removeServiceRequest(new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG,"Connecting to the group (" + service.getOwner().deviceAddress + ") ...");
                    }

                    @Override
                    public void onFailure(int i) {
                        error(i,"cannot connect to the group with address " + service.getOwner().deviceAddress);

                        mErrorListener.onError();
                    }
                });
            }
            @Override
            public void onFailure(int i) {
                error(i,"cannot remove service request");

                mErrorListener.onError();
            }
        });
    }

    public void disconnect(final WifiP2pManager.ActionListener listener){

        if(mStatus == STATUS_CLIENT) removeClient();
        if(mStatus == STATUS_GROUP_OWNER) stopServer();

        if(mStatus == STATUS_GROUP_OWNER){
            stopService(new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    //Ignore
                }

                @Override
                public void onFailure(int i) {
                    //Ignore
                }
            });

            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG,"Disconnecting...");

                    mStatus = STATUS_NONE;

                    if(listener != null) listener.onSuccess();
                }

                @Override
                public void onFailure(int i) {
                    error(i,"Disconnecting failed.");

                    if(listener != null) listener.onFailure(i);
                }
            });
        }
        else if(mStatus == STATUS_CLIENT){
            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG,"Disconnecting...");

                    mStatus = STATUS_NONE;

                    if(listener != null) listener.onSuccess();
                }

                @Override
                public void onFailure(int i) {
                    error(i,"Disconnecting failed, code: " + i);

                    if(listener != null) listener.onFailure(i);
                }
            });
        }

        mStatus = STATUS_NONE;
    }

    private void startService(){
        mServiceScanner.removeServiceRequest(null);

        Map<String,String> record = new HashMap<>();

        mPort = getAvailablePort();

        record.put("username",User.SELF.getUserName());
        record.put("visibility","visible");
        record.put("port", Integer.toString(mPort));

        WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance(SERVICE_INSTANCE,SERVICE_REGISTRATION_TYPE,record);

        mManager.addLocalService(mChannel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG,"Registering local service...");
            }

            @Override
            public void onFailure(int i) {
                error(i,"cannot register local service.");

                mErrorListener.onError();
            }
        });
    }

    private void stopService(final WifiP2pManager.ActionListener listener){
        mManager.clearLocalServices(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG,"The local service has stopped.");

                if(listener != null) listener.onSuccess();
            }

            @Override
            public void onFailure(int i) {
                error(i,"Cannot remove local service.");

                if(listener != null) listener.onFailure(i);
            }
        });
    }

    public void discoverServices(final ServiceDiscoveringEventListener callback){
        if(mServiceScanner != null){
            mServiceScanner.stopScanning();

            mServiceScanner.scan(callback);
        }
    }

    public void stopServiceDiscovering(){
        mServiceScanner.stopScanning();
    }

    public void sendMessage(Message message,MessageUploadListener messageUploadListener){
        Log.d(TAG,"Trying to send message. Status: " + mStatus);

        if(mServer == null && mClient == null){
            Toast.makeText(mContext, R.string.cannot_send_message,Toast.LENGTH_SHORT).show();

            Log.d(TAG,"Cannot send the message, because connection is not already exist.");

            messageUploadListener.onFinish();

            return;
        }

        if(mStatus == STATUS_GROUP_OWNER && mServer != null){
            mServer.setMessageUploadListener(messageUploadListener);

            mServer.sendMessage(message);
        }
        else if(mStatus == STATUS_CLIENT && mClient != null){
            mClient.setMessageUploadListener(messageUploadListener);
            mClient.sendMessage(message);
        }
    }

    public void createGroup(final MessageHandler messageHandler){
        mServiceScanner.removeServiceRequest(null);

        startService();

        if(messageHandler != null) mMessageHandler = messageHandler;

        mStatus = STATUS_GROUP_OWNER;
    }

    private void error(int code,@Nullable String message){
        if(code == WifiP2pManager.P2P_UNSUPPORTED){
            Log.e(TAG,"Wifi Direct is not supported");
        }
        else{
            if(message != null){
                Log.e(TAG,"Fatal error," + message + ",code: " + code);
            }
            else{
                Log.e(TAG,"Fatal error,code: " + code + ".");
            }
        }
    }

    private void startServer(MessageHandler messageHandler,MessageReceivingHandler receivingHandler){

        mMessageHandler = messageHandler;

        mServer = new Server(mContext,mMessageHandler,mPort,mErrorListener);

        mServer.setReceivingHandler(receivingHandler);

        mServer.start();
    }

    private void stopServer(){
        if(mServer != null){
            mServer.stop();
        }
    }

    private void addClient(MessageHandler messageHandler,MessageReceivingHandler receivingHandler){
        mMessageHandler = messageHandler;

        mClient = new Client(mContext,mMessageHandler,mGroupOwnerAddress,mPort,mErrorListener);

        mClient.setReceivingHandler(receivingHandler);

        mClient.start();
    }

    private void removeClient(){
        if(mClient != null){
            mClient.stop();
        }
    }

    public void rescanServices(){
        mServiceScanner.rescan(null);
    }

    public void setMessageHandler(MessageHandler messageHandler) {
        mMessageHandler = messageHandler;
    }

    public void setConnectionListener(ConnectionListener connectionListener) {
        mConnectionListener = connectionListener;
    }

    public void setReceivingHandler(MessageReceivingHandler receivingHandler) {
        mReceivingHandler = receivingHandler;

        if (mServer != null) mServer.setReceivingHandler(receivingHandler);
        if (mClient != null) mClient.setReceivingHandler(receivingHandler);
    }

    public void setErrorListener(ErrorListener errorListener) {
        mErrorListener = errorListener;
    }

    public int getStatus() {
        return mStatus;
    }

    public boolean isWifiEnabled(){
        return mWifiManager.isWifiEnabled();
    }

    public void enableWifi(){
        if(!isWifiEnabled()){
            mWifiManager.setWifiEnabled(true);
        }
    }

    public void disableWifi(){
        if(isWifiEnabled()){
            mWifiManager.setWifiEnabled(false);
        }
    }

    private int getAvailablePort(){
        return FreePortFinder.findFreeLocalPort();
    }
}
