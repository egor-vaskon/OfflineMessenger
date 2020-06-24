package com.golden_apps.offlinemessenger.io;

import android.app.VoiceInteractor;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.golden_apps.offlinemessenger.Message;
import com.golden_apps.offlinemessenger.User;
import com.golden_apps.offlinemessenger.utils.Utils;
import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

class ClientHandler implements Runnable{

    private Handler mHandler;
    private Socket mClient;
    private MessageHandler mMessageHandler;
    private Context mContext;
    private BufferedOutputStream mOutputStream;
    private BufferedInputStream mInputStream;
    private MessageReceivingHandler mReceivingHandler;
    private Thread mThread;
    private MessageUploadListener mUploadListener;
    private int mId;

    private static final String TAG = "ClientHandler";

    public ClientHandler(
            Context context
            ,Handler handler
            ,Socket client
            ,MessageHandler messageHandler
            ,MessageReceivingHandler receivingHandler
            ,MessageUploadListener uploadListener
            ,int id) {
        mContext = context;
        mHandler = handler;
        mClient = client;
        mMessageHandler = messageHandler;
        mReceivingHandler = receivingHandler;
        mUploadListener = uploadListener;
        mId = id;
    }

    public void handleClient(){
        mThread = new Thread(this);

        mThread.start();
    }

    public void stop(){
        mThread.interrupt();

        try {
            mClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        try{
            mInputStream = new BufferedInputStream(mClient.getInputStream(),512);
            mOutputStream = new BufferedOutputStream(mClient.getOutputStream(),512);
        }
        catch (IOException ex){
            Log.e(TAG,"",ex);
        }

        if(mInputStream == null || mOutputStream == null){
            Log.e(TAG,"Cannot handle client.");

            return;
        }

        while(!Thread.currentThread().isInterrupted()){
            handleMessage();
        }
    }

    private void sendBinary(OutputStream os,Message.Binary binary) throws IOException{

        BufferedInputStream is =
                new BufferedInputStream(mContext.getContentResolver().openInputStream(binary.getUri()));

        int writeCount = 0;

        while(true){
            if(Thread.currentThread().isInterrupted())  break;

            byte[] segment = new byte[512];

            int readCount = is.read(segment);

            mOutputStream.write(segment,0,readCount);

            mOutputStream.flush();

            writeCount += readCount;

            Log.d(WifiDirectManager.TAG,"Sending message: " + (writeCount/(double)binary.getFileSize()) * 100 + "%");

            if(readCount < 512) break;
        }

        mOutputStream.flush();
        is.close();
    }

    public void sendMessage(@NonNull final Message message){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(WifiDirectManager.TAG,"Sending a message...");

                Gson gson = new Gson();

                if(mOutputStream != null){
                    try{
                        String messageJson = gson.toJson(message);

                        byte[] messageBytes = messageJson.getBytes(StandardCharsets.UTF_8);
                        byte[] jsonLengthBytes = ByteBuffer.allocate(4).putInt(messageBytes.length).array();

                        mOutputStream.write(jsonLengthBytes);
                        mOutputStream.write(messageBytes);
                        mOutputStream.flush();

                        if(message.hasImage()) sendBinary(mOutputStream,message.getAttachedImage());
                        if(message.hasFile()) sendBinary(mOutputStream,message.getAttachedFile());

                        if(mUploadListener != null){
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mUploadListener.onFinish();
                                }
                            });
                        }
                    }
                    catch (Exception ex){
                        ex.printStackTrace();
                    }
                }
            }
        });

        thread.start();
    }

    private void handleMessage(){
        Gson gson = new Gson();

        try{
            int jsonLength = Utils.readInt(mInputStream);

            if(jsonLength > 1024*1024*5 || jsonLength < 0) return;

            byte[] messageBytes = new byte[jsonLength];
            int readCount = mInputStream.read(messageBytes);

            if(readCount < jsonLength){
                Log.e(WifiDirectManager.TAG,"Read " + readCount + " bytes,but length is " + jsonLength + " bytes.");
            }

            String data = new String(messageBytes,StandardCharsets.UTF_8);
            final Message message = gson.fromJson(data,Message.class);

            if(mReceivingHandler != null){
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mReceivingHandler.onReceiveStarted();
                    }
                });
            }

            if(message.hasImage()){
                message.getAttachedImage().setUri(Uri.fromFile(new File(message.getAttachedImage().getSavePath())));

                OutputStream osr = new FileOutputStream(message.getAttachedImage().getSavePath());
                BufferedOutputStream os = new BufferedOutputStream(osr,512);

                readDataToFile(mInputStream,os,message.getAttachedImage().getFileSize());
            }

            if(message.hasFile()){
                message.getAttachedFile().setUri(Uri.fromFile(new File(message.getAttachedFile().getSavePath())));

                OutputStream osr = new FileOutputStream(message.getAttachedFile().getSavePath());
                BufferedOutputStream os = new BufferedOutputStream(osr,512);

                readDataToFile(mInputStream,os,message.getAttachedFile().getFileSize());
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(mReceivingHandler != null) mReceivingHandler.onReceiveFinished();

                    mMessageHandler.handleMessage(getAddress(),message);
                }
            });
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private static void readDataToFile(InputStream is,OutputStream os,int size) throws IOException{
        byte[] buff = new byte[512];

        int totalReadCount = 0;
        int readCount;

        while(totalReadCount < size){

            int blockSize = Math.min((size - totalReadCount), 512);

            readCount = is.read(buff,0,blockSize);
            totalReadCount += readCount;

            os.write(buff,0,readCount);
        }

        Log.d(TAG,"Total read count: " + totalReadCount);

        os.flush();
        os.close();
    }

    public void setReceivingHandler(MessageReceivingHandler receivingHandler) {
        mReceivingHandler = receivingHandler;
    }

    public void setUploadListener(MessageUploadListener uploadListener) {
        mUploadListener = uploadListener;
    }

    public int getId() {
        return mId;
    }

    public InetAddress getAddress(){
        return mClient.getInetAddress();
    }
}
