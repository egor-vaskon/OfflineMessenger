package com.golden_apps.offlinemessenger.io;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;

import com.golden_apps.offlinemessenger.Message;
import com.golden_apps.offlinemessenger.User;
import com.golden_apps.offlinemessenger.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.$Gson$Types;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Client implements Runnable{

    private BufferedInputStream mInputStream;
    private volatile BufferedOutputStream mOutputStream;
    private Thread mThread;
    private InetAddress mAddress;
    private int mPort;
    private Handler mHandler;
    private MessageHandler mMessageHandler;
    @NonNull
    private Context mContext;
    private MessageUploadListener mMessageUploadListener;
    private MessageReceivingHandler mReceivingHandler;
    private ErrorListener mErrorListener;
    private Socket mSocket;

    private static final String TAG = "Client";

    Client(@NonNull Context context,@NonNull MessageHandler messageHandler,@NonNull InetAddress address
            , int port,ErrorListener errorListener){

        mHandler = new Handler(context.getMainLooper());
        mMessageHandler = messageHandler;
        mAddress = address;
        mPort = port;
        mContext = context;
        mErrorListener = errorListener;

        mThread = new Thread(this);
    }

    public void start(){
        mThread.start();
    }

    private void sendBinary(OutputStream os,Message.Binary binary) throws IOException{

        BufferedInputStream is =
                new BufferedInputStream(mContext.getContentResolver().openInputStream(binary.getUri()));

        int writeCount = 0;

        while(true){
            if(Thread.currentThread().isInterrupted())  break;

            byte[] segment = new byte[512];

            int readCount = is.read(segment);

            os.write(segment,0,readCount);
            os.flush();

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

                        if(mMessageUploadListener != null){
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mMessageUploadListener.onFinish();
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

    @Override
    public void run() {
        Gson gson = new Gson();

        try{
            mSocket = new Socket(mAddress,mPort);

            Log.d(WifiDirectManager.TAG,"Connected to " + mAddress + " on port " + mPort);

            mInputStream = new BufferedInputStream(mSocket.getInputStream());
            mOutputStream = new BufferedOutputStream(mSocket.getOutputStream());
        }
        catch (IOException ex){
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mErrorListener.onError();
                }
            });

            Log.e(WifiDirectManager.TAG,"",ex);

            return;
        }

        while (!Thread.currentThread().isInterrupted()) {

            try {
                int jsonLength = Utils.readInt(mInputStream);

                if (jsonLength > 1024 * 1024 * 5 || jsonLength < 0) return;

                byte[] messageBytes = new byte[jsonLength];
                int readCount = mInputStream.read(messageBytes);

                if (readCount < jsonLength) {
                    Log.e(WifiDirectManager.TAG, "Read " + readCount + " bytes,but length is " + jsonLength + " bytes.");
                }

                String data = new String(messageBytes, StandardCharsets.UTF_8);
                final Message message = gson.fromJson(data, Message.class);

                if (mReceivingHandler != null) {
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
                        if (mReceivingHandler != null) mReceivingHandler.onReceiveFinished();

                        mMessageHandler.handleMessage(mSocket.getInetAddress(), message);
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    void stop(){
        mThread.interrupt();

        try {
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(WifiDirectManager.TAG,"The client has stopped.");
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

    public void setMessageUploadListener(MessageUploadListener messageUploadListener) {
        mMessageUploadListener = messageUploadListener;
    }

    public void setReceivingHandler(MessageReceivingHandler receivingHandler) {
        mReceivingHandler = receivingHandler;
    }
}
