package com.golden_apps.offlinemessenger.io;


import android.app.VoiceInteractor;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.Nullable;

import com.golden_apps.offlinemessenger.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class ServiceScanner{

    private ServiceDiscoveringEventListener mServiceDiscoveringEventListener;
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private volatile WifiP2pDnsSdServiceRequest mServiceRequest;
    private HashMap<String,Service> mFoundServices;
    private Handler mHandler;
    private Timer mTimer;

    private static final String TAG = "ServiceScanner";

    ServiceScanner(Context context, WifiP2pManager manager, WifiP2pManager.Channel channel) {
        mServiceDiscoveringEventListener = null;
        mManager = manager;
        mChannel = channel;

        mTimer = new Timer();
        mHandler = new Handler(context.getMainLooper());
    }

    public void scan(ServiceDiscoveringEventListener callback){
        if(callback != null) mServiceDiscoveringEventListener = callback;

        TimerTask scannerTask = new TimerTask() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(mServiceRequest != null) stop();

                        start(mServiceDiscoveringEventListener);
                    }
                });
            }
        };

        mTimer = new Timer();

        mTimer.schedule(scannerTask,0,2 * 1000);
    }

    public void stopScanning(){
        mTimer.cancel();

        if(mServiceRequest != null) stop();
    }

    public void rescan(ServiceDiscoveringEventListener callback){
        if(callback != null) mServiceDiscoveringEventListener = callback;

        stopScanning();
        scan(mServiceDiscoveringEventListener);
    }

    private void start(ServiceDiscoveringEventListener callback){
        removeServiceRequest(null);

        if(callback != null) mServiceDiscoveringEventListener = callback;

        //addService();

        if(mFoundServices == null){
            mFoundServices = new HashMap<>();
        }

        WifiP2pManager.DnsSdTxtRecordListener dnsSdTxtRecordListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String instanceName
                    , Map<String, String> record
                    , WifiP2pDevice device) {
                Log.d(TAG,"onDnsTxtRecordAvailable Instance: " + instanceName + "\nrecord: " + record.toString());

                Service service = new Service();

                service.setInstanceName(instanceName);
                service.setOwner(device);
                service.setRecord(record);

                if(!record.containsKey("port")) return;

                service.setPort(Integer.parseInt(record.get("port")));

                mFoundServices.put(device.deviceAddress,service);
            }
        };

        WifiP2pManager.DnsSdServiceResponseListener dnsSdServiceResponseListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName
                    ,String type
                    ,WifiP2pDevice device) {

                Log.d(TAG,"Found service: {instanceName: " + instanceName + ",type: " + type + ",device: " + device.toString() + ")");

                Service service = mFoundServices.get(device.deviceAddress);

                service.setType(type);

                if(mServiceDiscoveringEventListener != null){
                    if(service.getType().contains(WifiDirectManager.SERVICE_REGISTRATION_TYPE)){
                        mServiceDiscoveringEventListener.onServiceFound(service);
                    }
                }
            }
        };

        mManager.setDnsSdResponseListeners(mChannel,dnsSdServiceResponseListener,dnsSdTxtRecordListener);

        mServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();

        mManager.addServiceRequest(mChannel, mServiceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG,"Added service request.");

                mManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG,"Service discovering has initiated");

                        Log.d(TAG,Boolean.toString(mServiceDiscoveringEventListener == null));

                        if(mServiceDiscoveringEventListener != null) mServiceDiscoveringEventListener.onDiscoveringStarted();
                    }

                    @Override
                    public void onFailure(int i) {
                        error(i,"cannot initiate service discovering.");
                    }
                });
            }

            @Override
            public void onFailure(int i) {
                error(i,"cannot add service request.");
            }
        });

    }

     private void stop(){
        removeServiceRequest(new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG,"The service discovering has stopped.");
            }

            @Override
            public void onFailure(int i) {
                error(i,"cannot stop service discovering.");
            }
        });

        if(mServiceDiscoveringEventListener != null) mServiceDiscoveringEventListener.onDiscoveringFinished();
     }

     private void addService(){
        Map<String,String> record = new HashMap<>();

        record.put("username", User.SELF.getUserName());
        record.put("visibility","invisible");

         WifiP2pDnsSdServiceInfo serviceInfo =
                 WifiP2pDnsSdServiceInfo
                         .newInstance(WifiDirectManager.SERVICE_INSTANCE
                         ,WifiDirectManager.SERVICE_REGISTRATION_TYPE,record);

         mManager.addLocalService(mChannel, serviceInfo, new WifiP2pManager.ActionListener() {
             @Override
             public void onSuccess() {
                 Log.d(TAG,"Invisible local service has added.");
             }

             @Override
             public void onFailure(int i) {
                error(i,"cannot add invisible local service.");
             }
         });
     }

     private void stopService(){
        mManager.clearLocalServices(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG,"Invisible local service has stopped.");
            }

            @Override
            public void onFailure(int i) {
                error(i,"cannot remove invisible local service.");
            }
        });
     }

     public void removeServiceRequest(WifiP2pManager.ActionListener listener){
        if(mServiceRequest != null){
            mManager.removeServiceRequest(mChannel, mServiceRequest, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG,"Service request has removed.");

                    mServiceRequest = null;
                }

                @Override
                public void onFailure(int i) {
                    error(i,"cannot remove service request");
                }
            });
        }
        else{
            if(listener != null) listener.onSuccess();
        }
     }

    private void error(int code,@Nullable String message){
        if(code == WifiP2pManager.P2P_UNSUPPORTED){
            Log.e(TAG,"Wifi Direct is not supported");
        }
        else{
            if(message != null){
                Log.e(TAG,"Fatal error," + message);
            }
            else{
                Log.e(TAG,"Fatal error,code: " + code + ".");
            }
        }
    }

    public HashMap<String, Service> getFoundServices() {
        return mFoundServices;
    }

    public void clearFoundServices(){
        mFoundServices = new HashMap<>();
    }

    public void setCallback(ServiceDiscoveringEventListener callback){
        mServiceDiscoveringEventListener = callback;
    }
}

