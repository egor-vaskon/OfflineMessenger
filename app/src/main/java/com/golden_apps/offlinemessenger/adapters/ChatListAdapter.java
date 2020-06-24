package com.golden_apps.offlinemessenger.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.graphics.drawable.DrawableCompat;

import com.golden_apps.offlinemessenger.Chats;
import com.golden_apps.offlinemessenger.R;
import com.golden_apps.offlinemessenger.User;

import java.util.ArrayList;

public class ChatListAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    private Chats mChats;
    private Context mContext;

    public ChatListAdapter(Context context){
        mContext = context;
        mChats = new Chats(mContext);
        mInflater = LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        return mChats.getNumberOfChats();
    }

    @Override
    public Object getItem(int i) {
        return mChats.getChat(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        if(view == null){
            view = mInflater.inflate(R.layout.chat_list_item,viewGroup,false);
        }

        RelativeLayout chatContainer = view.findViewById(R.id.chat_container);

        TextView chatName = view.findViewById(R.id.chat_name);

        chatName.setText(mChats.getChat(i).getTitle());

        return view;
    }

    public Chats getChats() {
        return mChats;
    }
}
