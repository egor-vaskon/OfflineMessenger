package com.golden_apps.offlinemessenger.adapters;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.golden_apps.offlinemessenger.Chat;
import com.golden_apps.offlinemessenger.ChatActivity;
import com.golden_apps.offlinemessenger.Message;
import com.golden_apps.offlinemessenger.R;
import com.golden_apps.offlinemessenger.User;

import org.w3c.dom.Text;

import java.lang.ref.WeakReference;

public class MessageListAdapter extends BaseAdapter {

    private Chat mChat;
    private Context mContext;
    private LayoutInflater mInflater;

    public MessageListAdapter(Chat chat, Context context) {
        mChat = chat;
        mContext = context;

        mInflater = LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        return mChat.getNumberOfMessages();
    }

    @Override
    public Object getItem(int i) {
        return mChat.getMessage(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {

        if(view == null){
            view = mInflater.inflate(R.layout.message,viewGroup,false);
        }

        final Message message = mChat.getMessage(i);

        RelativeLayout messageContainer = view.findViewById(R.id.message_container);

        TextView messageText = view.findViewById(R.id.message_text);
        messageText.setText(message.getText());

        final TextView messageAuthor = view.findViewById(R.id.message_author_text);
        messageAuthor.setText(message.getAuthor().getUserName());

        ImageView image = view.findViewById(R.id.message_image);

        TextView attachedFile = view.findViewById(R.id.attached_to_message_file);
        FrameLayout attachedFileContainer = view.findViewById(R.id.attached_to_message_file_container);

        attachedFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(message.hasFile()){
                    Intent intent = new Intent(Intent.ACTION_VIEW);

                    intent.setDataAndType(message.getAttachedFile().getUri(),message.getAttachedFile().getMimeType());
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    try{
                        mContext.startActivity(intent);
                    }
                    catch (ActivityNotFoundException ex){
                        Toast.makeText(mContext, R.string.cant_open_file,Toast.LENGTH_SHORT).show();
                    }

                }
            }
        });

        if(message.hasFile()){
            attachedFile.setVisibility(View.VISIBLE);
            attachedFileContainer.setVisibility(View.VISIBLE);

            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) attachedFile.getLayoutParams();
            layoutParams.height = FrameLayout.LayoutParams.WRAP_CONTENT;

            attachedFile.setLayoutParams(layoutParams);

            attachedFile.setText(message.getAttachedFile().getFilename());
        }
        else{
            attachedFile.setVisibility(View.INVISIBLE);
            attachedFileContainer.setVisibility(View.INVISIBLE);

            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) attachedFile.getLayoutParams();
            layoutParams.height = 0;
            attachedFile.setLayoutParams(layoutParams);

            attachedFile.setText(null);
        }

        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(message.hasImage()){
                    Intent intent = new Intent(Intent.ACTION_VIEW);

                    intent.setDataAndType(message.getAttachedImage().getUri(),message.getAttachedImage().getMimeType());

                    mContext.startActivity(intent);
                }
            }
        });

        if(message.hasImage()){
            ViewGroup.LayoutParams params = image.getLayoutParams();

            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;

            image.setClipToOutline(true);

            image.setLayoutParams(params);

            image.setImageURI(message.getAttachedImage().getUri());
        }
        else{
            ViewGroup.LayoutParams params = image.getLayoutParams();

            params.width = 0;
            params.height = 0;

            image.setLayoutParams(params);

            image.setImageURI(null);
        }

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) messageContainer.getLayoutParams();

        if(message == null) return null;

        if(message.getAuthor().getAddress().equals(User.SELF.getAddress())){

            params.removeRule(RelativeLayout.ALIGN_PARENT_END);
            params.addRule(RelativeLayout.ALIGN_PARENT_START,RelativeLayout.TRUE);

            messageContainer.
                    setBackground(mContext.getResources().getDrawable(R.drawable.message_background
                            ,mContext.getTheme()));
        }
        else{
            params.removeRule(RelativeLayout.ALIGN_PARENT_START);
            params.addRule(RelativeLayout.ALIGN_PARENT_END,RelativeLayout.TRUE);

            messageContainer.
                    setBackground(mContext.getResources().getDrawable(R.drawable.message_background2
                            ,mContext.getTheme()));
        }

        return view;
    }

    public Chat getChat() {
        return mChat;
    }

    public void addMessage(Message message){
        mChat.addMessage(message);
    }
}
