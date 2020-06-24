package com.golden_apps.offlinemessenger.io;


import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import com.golden_apps.offlinemessenger.Chat;
import com.golden_apps.offlinemessenger.Message;
import com.golden_apps.offlinemessenger.User;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server implements Runnable{

    private Thread mThread;
    private int mPort;
    private Handler mHandler;
    private MessageHandler mMessageHandler;
    private ServerSocket mServerSocket;
    private Context mContext;
    private MessageUploadListener mMessageUploadListener;
    private MessageReceivingHandler mReceivingHandler;
    private ArrayList<ClientHandler> mClientHandlers;
    private ErrorListener mErrorListener;
    private MessageHandler mSecondaryMessageHandler;

    Server(@NonNull Context context, @NonNull MessageHandler messageHandler, int port, ErrorListener errorListener){
        mPort = port;
        mHandler = new Handler(context.getMainLooper());
        mSecondaryMessageHandler = messageHandler;
        mContext = context;

        mMessageHandler = new MessageHandler() {
            @Override
            public void handleMessage(InetAddress source,Message message) {

                for(ClientHandler client:mClientHandlers){
                    if(!source.equals(client.getAddress())){
                        client.sendMessage(message);
                    }
                }

                mSecondaryMessageHandler.handleMessage(source,message);
            }
        };

        mMessageUploadListener = null;
        mReceivingHandler = null;

        mClientHandlers = new ArrayList<>();

        mErrorListener = errorListener;

        mThread = new Thread(this);
    }

    void start() {
        mThread.start();
    }

    @Override
    public void run() {

        try{
            Log.d(WifiDirectManager.TAG,"Starting Server on port " + mPort + "...");

            mServerSocket = new ServerSocket(mPort);
        }
        catch (IOException ex){
            Log.e(WifiDirectManager.TAG,"",ex);

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mErrorListener.onError();
                }
            });

            return;
        }

        while(!Thread.currentThread().isInterrupted()){
            try{
                Socket client = mServerSocket.accept();

                ClientHandler clientHandler =
                        new ClientHandler(mContext,mHandler,client,mMessageHandler,mReceivingHandler,mMessageUploadListener
                                ,mClientHandlers.size());

                clientHandler.handleClient();

                mClientHandlers.add(clientHandler);
            }
            catch (IOException ex){
                ex.printStackTrace();

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mErrorListener.onError();
                    }
                });

                Thread.currentThread().interrupt();
            }
        }
    }

    void sendMessage(Message message){
        for(ClientHandler client:mClientHandlers){
            client.sendMessage(message);
        }
    }

    void stop(){
        try{
            if(mServerSocket != null){
                for(ClientHandler client:mClientHandlers){
                    client.stop();
                }

                mServerSocket.close();
            }
        }
        catch (Exception ex){
            mErrorListener.onError();

            Log.e(WifiDirectManager.TAG,"",ex);
        }

        mThread.interrupt();

        Log.d(WifiDirectManager.TAG,"The server has stopped.");
    }

    void setMessageUploadListener(MessageUploadListener messageUploadListener) {
        mMessageUploadListener = messageUploadListener;

        for(ClientHandler client:mClientHandlers){
            client.setUploadListener(messageUploadListener);
        }
    }

    void setReceivingHandler(MessageReceivingHandler receivingHandler) {
        mReceivingHandler = receivingHandler;

        for(ClientHandler client:mClientHandlers){
            client.setReceivingHandler(receivingHandler);
        }
    }
}
