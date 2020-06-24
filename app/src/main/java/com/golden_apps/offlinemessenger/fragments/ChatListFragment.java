package com.golden_apps.offlinemessenger.fragments;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;

import com.golden_apps.offlinemessenger.Chat;
import com.golden_apps.offlinemessenger.ChatActivity;
import com.golden_apps.offlinemessenger.R;
import com.golden_apps.offlinemessenger.User;
import com.golden_apps.offlinemessenger.adapters.ChatListAdapter;
import com.golden_apps.offlinemessenger.io.Service;
import com.golden_apps.offlinemessenger.io.ServiceDiscoveringEventListener;
import com.golden_apps.offlinemessenger.io.ServiceDiscoveringStateListener;
import com.golden_apps.offlinemessenger.io.WifiDirectManager;

import java.util.ArrayList;

public class ChatListFragment extends ListFragment {

    private ChatListAdapter mAdapter;

    private ServiceDiscoveringStateListener mDiscoveringStateListener;

    public ChatListFragment() {

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new ChatListAdapter(getContext());

        setListAdapter(mAdapter);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        WifiDirectManager.getInstance().registerReceiver();

        mAdapter.getChats().setChats(new ArrayList<Chat>());

        getListView().setDivider(getResources().getDrawable(R.drawable.chat_list_divider,null));
        getListView().setDividerHeight(1);

        WifiDirectManager.getInstance().discoverServices(new ServiceDiscoveringEventListener() {
            @Override
            public void onServiceFound(Service service) {
                if(service.getRecord().get("visibility").equals("visible")){
                    User owner = new User(service.getRecord().get("username")
                            ,0
                            ,service.getOwner().deviceAddress);

                    Chat chat = new Chat(getActivity(),owner);

                    boolean contains = false;

                    for(Chat element : mAdapter.getChats().getChats()){
                        if(element.getOwner().getAddress().equals(chat.getOwner().getAddress())){
                            contains = true; break;
                        }
                    }

                    if(!contains){
                        mAdapter.getChats().addChat(chat);

                        mAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onDiscoveringStarted() {
                if(mDiscoveringStateListener != null){
                    mDiscoveringStateListener.onDiscoveringStarted();
                }
            }

            @Override
            public void onDiscoveringFinished() {
                if(mDiscoveringStateListener != null){
                    mDiscoveringStateListener.onDiscoveringFinished();
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        WifiDirectManager.getInstance().stopServiceDiscovering();
        WifiDirectManager.getInstance().unregisterReceiver();
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if(WifiDirectManager.getInstance().isWifiEnabled()){
            Intent intent = new Intent(getActivity(),ChatActivity.class);
            intent.putExtra("chat",mAdapter.getChats().getChat(position).toJson().toString());

            startActivity(intent);
        }
        else{
            Toast.makeText(getActivity(),R.string.enable_wifi,Toast.LENGTH_SHORT).show();

            WifiDirectManager.getInstance().enableWifi();
        }
    }

    public void setDiscoveringStateListener(ServiceDiscoveringStateListener discoveringStateListener) {
        mDiscoveringStateListener = discoveringStateListener;
    }

    public void rescanServices(){
        WifiDirectManager.getInstance().rescanServices();
    }

    public static float convertDpToPixel(float dp, Context context){
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public void createChat() {
        if(WifiDirectManager.getInstance().isWifiEnabled()){
            if (!User.SELF.equals("")) {
                Intent intent = new Intent(getActivity(), ChatActivity.class);

                Chat selfChat = new Chat(getActivity(), User.SELF);

                intent.putExtra("chat", selfChat.toJson().toString());

                startActivity(intent);
            }
        }
        else{
            Toast.makeText(getActivity(), R.string.enable_wifi,Toast.LENGTH_SHORT).show();

            WifiDirectManager.getInstance().enableWifi();
        }

    }
}
