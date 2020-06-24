package com.golden_apps.offlinemessenger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.golden_apps.offlinemessenger.adapters.MessageListAdapter;
import com.golden_apps.offlinemessenger.io.ConnectionListener;
import com.golden_apps.offlinemessenger.io.ErrorListener;
import com.golden_apps.offlinemessenger.io.MessageHandler;
import com.golden_apps.offlinemessenger.io.MessageReceivingHandler;
import com.golden_apps.offlinemessenger.io.MessageUploadListener;
import com.golden_apps.offlinemessenger.io.WifiDirectManager;

import com.golden_apps.offlinemessenger.utils.PathUtil;
import com.golden_apps.offlinemessenger.utils.Utils;
import com.google.gson.JsonParser;

import java.net.InetAddress;
import java.net.URISyntaxException;

public class ChatActivity extends AppCompatActivity {

    public static final String TAG = "ChatActivity";

    private static final int PICK_IMAGE = 2;
    private static final int PICK_FILE = 3;

    private ListView mMessages;
    private MessageListAdapter mAdapter;
    private EditText mMessageText;

    private Chat mChat;

    private Handler mHandler;
    private MessageHandler mMessageHandler;
    private ConnectionListener mConnectionListener;

    private ImageView mMessageIcon;
    private Uri mImageUri;

    private Message.Binary mAttachedFileBinary;

    private ImageButton mAddImageButton;
    private ImageButton mSendMessageButton;
    private ImageButton mAttachFileButton;
    private Uri mFileUri;
    private FrameLayout mAttachedFileContainer;

    private TextView mAttachedFile;

    private NotificationManager mNotificationManager;

    private NotificationCompat.Builder mNotificationBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        mHandler = new Handler(getMainLooper());
        mMessageHandler = new MessageHandler() {
            @Override
            public void handleMessage(InetAddress source,Message message) {
                if(mAdapter != null && mMessages != null){
                    mAdapter.getChat().addMessage(message);
                    mAdapter.notifyDataSetChanged();
                }
            }
        };

        mConnectionListener = new ConnectionListener() {
            @Override
            public void onConnected() {
                Toast.makeText(ChatActivity.this, R.string.connected,Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDisconnected() {

                if(WifiDirectManager.getInstance().getStatus() != WifiDirectManager.STATUS_NONE){
                    Toast.makeText(ChatActivity.this, R.string.disconnected,Toast.LENGTH_SHORT).show();

                    finish();
                }
            }
        };

        WifiDirectManager.getInstance().setMessageHandler(mMessageHandler);
        WifiDirectManager.getInstance().setConnectionListener(mConnectionListener);

        mChat = null;

        String chatInfoJson = getIntent().getExtras().getString("chat");

        mChat = Chat.createFromJson(this
                ,JsonParser.parseString(chatInfoJson).getAsJsonObject()
                ,true);

        mMessages = findViewById(R.id.messages);
        mSendMessageButton = findViewById(R.id.send_message);
        mAddImageButton = findViewById(R.id.add_image);
        mMessageText = findViewById(R.id.new_message);
        mAttachFileButton = findViewById(R.id.attach_file);
        mAttachedFileContainer = findViewById(R.id.attached_file_container);

        mAttachedFile = findViewById(R.id.attached_file_title);

        mMessageIcon = findViewById(R.id.message_icon);

        mMessageIcon.setClipToOutline(true);

        WifiDirectManager.getInstance().setReceivingHandler(new MessageReceivingHandler() {
            @Override
            public void onReceiveStarted() {
                if(Build.VERSION.SDK_INT >= 26){
                    NotificationChannel channel = new NotificationChannel("receive_message_channel","Message channel",NotificationManager.IMPORTANCE_DEFAULT);

                    mNotificationManager.createNotificationChannel(channel);

                    mNotificationBuilder =
                            new NotificationCompat.Builder(ChatActivity.this,"receive_message_channel");

                }
                else {
                    mNotificationBuilder =
                            new NotificationCompat.Builder(ChatActivity.this);
                }

                mNotificationBuilder.setSmallIcon(android.R.drawable.stat_sys_download);
                mNotificationBuilder.setContentTitle("Receiving message...");
                mNotificationBuilder.setWhen(System.currentTimeMillis());
                mNotificationBuilder.setOngoing(true);
                mNotificationBuilder.setProgress(0,0,true);
                mNotificationManager.notify(2,mNotificationBuilder.build());
            }

            @Override
            public void onReceiveFinished() {
                mNotificationManager.cancel(2);
            }
        });

        WifiDirectManager.getInstance().setErrorListener(new ErrorListener() {
            @Override
            public void onError() {
                Toast.makeText(ChatActivity.this,"Error!",Toast.LENGTH_LONG).show();

                WifiDirectManager.getInstance().disconnect(null);

                finish();
            }
        });

        mSendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mMessageText.getText().toString().isEmpty() || mFileUri != null || mImageUri != null){

                    Message message = new Message(ChatActivity.this,User.SELF,mMessageText.getText().toString());

                    if(mImageUri != null){
                        message.loadImage(ChatActivity.this,mImageUri);

                        mMessageIcon.setImageURI(null);
                        mImageUri = null;
                    }

                    mAdapter.addMessage(message);
                    mAdapter.notifyDataSetChanged();

                    RelativeLayout.LayoutParams params =
                            (RelativeLayout.LayoutParams) mMessageIcon.getLayoutParams();

                    params.width = RelativeLayout.LayoutParams.WRAP_CONTENT;
                    params.height = RelativeLayout.LayoutParams.WRAP_CONTENT;

                    mMessageIcon.setLayoutParams(params);

                    if(mAttachedFile.getVisibility() == View.VISIBLE && mFileUri != null){

                        message.setAttachedFile(mAttachedFileBinary);

                        mAttachedFile.setVisibility(View.INVISIBLE);
                        mAttachedFileContainer.setVisibility(View.INVISIBLE);
                        mAttachFileButton.setEnabled(true);
                        mFileUri = null;
                    }

                    mMessageText.setText("");

                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                        NotificationChannel channel = new NotificationChannel("upload_message_channel","Message channel",NotificationManager.IMPORTANCE_DEFAULT);

                        mNotificationManager.createNotificationChannel(channel);

                        mNotificationBuilder =
                                new NotificationCompat.Builder(ChatActivity.this,"upload_message_channel");

                    }
                    else {
                       mNotificationBuilder =
                                new NotificationCompat.Builder(ChatActivity.this);
                    }

                    mNotificationBuilder.setSmallIcon(android.R.drawable.stat_sys_upload);
                    mNotificationBuilder.setContentTitle("Sending message...");
                    mNotificationBuilder.setWhen(System.currentTimeMillis());
                    mNotificationBuilder.setOngoing(true);
                    mNotificationBuilder.setProgress(0,0,true);
                    mNotificationManager.notify(1,mNotificationBuilder.build());

                    WifiDirectManager.getInstance().sendMessage(message, new MessageUploadListener() {
                        @Override
                        public void onFinish() {
                            mNotificationManager.cancel(1);

                            mAddImageButton.setEnabled(true);
                            mAttachFileButton.setEnabled(true);
                        }
                    });
                }
            }
        });

        mAddImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);

                intent.setType("image/*");

                startActivityForResult(intent,PICK_IMAGE);
            }
        });

        mAttachFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();

                String[] mimeTypes = new String[]{
                        "application/*","text/*","video/*","file/*","audio/*"
                };

                intent.putExtra(Intent.EXTRA_MIME_TYPES,mimeTypes);

                intent.setType("*/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(intent,PICK_FILE);
            }
        });

        mAdapter = new MessageListAdapter(mChat,this);

        mMessages.setAdapter(mAdapter);

        WifiDirectManager.getInstance().registerReceiver();

        if(mChat.getOwner().equals(User.SELF)){
            WifiDirectManager.getInstance().createGroup(mMessageHandler);
        }
        else{
            WifiDirectManager
                    .getInstance()
                    .connect(mChat.getOwner().getAddress(),mMessageHandler,mConnectionListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        WifiDirectManager.getInstance().disconnect(null);

        WifiDirectManager.getInstance().unregisterReceiver();

        mNotificationManager.cancel(1);
        mNotificationManager.cancel(2);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("chat",mChat.toJson().toString());
        outState.putString("messages",mChat.convertMessagesToJson().toString());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        String chat = savedInstanceState.getString("chat");

        if(chat != null){
            mChat = Chat.createFromJson(this
                    ,JsonParser.parseString(chat).getAsJsonObject()
                    ,true);
        }

        String messagesJson = savedInstanceState.getString("messages");

        if(messagesJson != null){
            mChat.setMessages(Chat.loadMessagesFromJson(messagesJson));
        }

        mAdapter = new MessageListAdapter(mChat,this);

        mMessages.setAdapter(mAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PICK_IMAGE || resultCode == PICK_IMAGE){
            if(data != null){
                mImageUri = data.getData();
                mMessageIcon.setImageURI(mImageUri);
                mAddImageButton.setEnabled(false);
            }
        }
        else if(requestCode == PICK_FILE || resultCode == PICK_FILE){
            if(data != null){
                try {

                    mFileUri = data.getData();
                    mAttachedFileBinary = Utils.getBinaryFromUri(this,mFileUri);

                    if(mAttachedFileBinary != null){
                        mAttachedFile.setVisibility(View.VISIBLE);
                        mAttachFileButton.setEnabled(false);
                        mAttachedFileContainer.setVisibility(View.VISIBLE);

                        mAttachedFile.setText(mAttachedFileBinary.getFilename());
                    }


                } catch (Exception ex) {
                    Log.e(TAG,"",ex);
                }
            }

        }
    }

    /*@Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        ClipData data = getIntent().getClipData();

        if(data != null){
            for(int i = 0;i<data.getItemCount();i++){
                ClipData.Item item = data.getItemAt(i);

                if(item.getUri() != null){
                    mMessageIcon.setImageURI(item.getUri());
                    mImageUri = item.getUri();
                }
            }
        }
    }*/
}
