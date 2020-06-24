package com.golden_apps.offlinemessenger;

import android.content.Context;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class Chats {

    private static String SAVE_DIR = "chats";
    private static String CHAT_LIST_FILE = "chats.json";

    private Context mContext;

    private ArrayList<Chat> mChats;

    public Chats(Context context){
        mContext = context;
        mChats = new ArrayList<>();

        load();
    }

    public void addChat(Chat chat){
        mChats.add(chat);
    }

    public Chat getChat(int i){
        return mChats.get(i);
    }

    private void load(){
        try{
            JsonArray chats
                    = JsonParser
                    .parseReader(new InputStreamReader(mContext.openFileInput(CHAT_LIST_FILE)))
                    .getAsJsonArray();

            for(int i = 0;i<chats.size();i++){
                mChats.add(Chat.createFromJson(mContext,chats.get(i).getAsJsonObject(),false));
            }
        }
        catch (FileNotFoundException ex){
            ex.printStackTrace();
        }
    }

    public void save(){
        JsonArray chats = new JsonArray();

        for(Chat chat:mChats){
            if(!chat.getOwner().equals(User.SELF)){
                chats.add(chat.toJson());
            }
        }

        try{
            BufferedWriter writer
                    = new BufferedWriter
                    (new OutputStreamWriter(mContext.openFileOutput(CHAT_LIST_FILE,Context.MODE_PRIVATE)));

            writer.write(chats.toString());

            writer.close();
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
    }

    public int getNumberOfChats() {
        return mChats.size();
    }

    public void setChats(ArrayList<Chat> chats) {
        mChats = chats;
    }

    public boolean contains(Chat chat){
        return mChats.contains(chat);
    }

    public ArrayList<Chat> getChats() {
        return mChats;
    }
}
