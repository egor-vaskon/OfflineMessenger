package com.golden_apps.offlinemessenger;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.golden_apps.offlinemessenger.io.Service;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;

public class Chat {

    private String mSaveFile;

    @NonNull
    private Context mContext;

    @NonNull
    private ArrayList<Message> mMessages;

    @NonNull
    private User mOwner;

    @NonNull
    private String mTitle;

    public Chat(Context context,JsonObject object){

        Gson gson = new Gson();

        JsonArray messagesJson = object.get("messages").getAsJsonArray();
        String title = object.get("title").getAsString();

        User owner = gson.fromJson(object.get("owner"),User.class);


        ArrayList<Message> messages = new ArrayList<>();

        for(int i = 0;i<messagesJson.size();i++){
            messages.add(gson.fromJson(messagesJson.get(i).toString(),Message.class));
        }

        mContext = context;
        mTitle = title;
        mMessages = messages;
        mSaveFile = mTitle + "_chat.json";
        mOwner = owner;
    }

    public Chat(Context context,User owner){
        mContext = context;
        mTitle = owner.getUserName();
        mMessages = new ArrayList<>();
        mOwner = owner;
        mSaveFile = mTitle + "_chat.json";
    }

    public Chat(String title,User owner, Context context) {

        mMessages = new ArrayList<>();
        mOwner = owner;

        mContext = context;
        mTitle = title;

        mSaveFile = title + "_chat.json";
    }

    public Chat(String title,User owner, Context context, ArrayList<Message> messages) {
        mTitle = title;
        mContext = context;
        mMessages = messages;

        mOwner = owner;
    }


    public Message getMessage(int index) {
        return mMessages.get(index);
    }

    @NonNull
    public String getTitle() {
        return mTitle;
    }

    public void addMessage(Message message) {
        mMessages.add(message);
    }

    @NonNull
    public String getSaveFile() {
        return mSaveFile;
    }

    public void setSaveFile(@NonNull String saveFile) {
        mSaveFile = saveFile;
    }

    public int getNumberOfMessages(){
        return mMessages.size();
    }

    public void setMessages(@Nullable ArrayList<Message> messages) {
        if(messages == null){
            messages = new ArrayList<>();
        }

        mMessages = messages;
    }

    public JsonElement convertMessagesToJson(){
        Gson gson = new Gson();

        JsonArray messages = new JsonArray();

        for(int i = (mMessages.size() - (mMessages.size() % 20));i<mMessages.size();i++){
            JsonObject messageJson = gson.toJsonTree(mMessages.get(i)).getAsJsonObject();

            messages.add(messageJson);
        }

        return messages;
    }

    /*public void saveMessagesToFile(){
        try(Writer writer = new OutputStreamWriter(mContext.openFileOutput(mSaveFile,Context.MODE_PRIVATE))){
            writer.write(convertMessageToJson().toString());
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
    }*/

    public static ArrayList<Message> loadMessagesFromJson(String json){
        Gson gson = new Gson();

        ArrayList<Message> messages = new ArrayList<>();
        JsonArray messagesJson;

        try{
            messagesJson = JsonParser.parseString(json).getAsJsonArray();
        }
        catch (Exception ex){
            messagesJson = new JsonArray();
        }

        for(JsonElement messageJson : messagesJson){
            messages.add(gson.fromJson(messageJson,Message.class));
        }

        return messages;
    }

    public static ArrayList<Message> loadMessagesFromFile(@NonNull Context context,@NonNull String saveFile){
        try{
            Reader reader = new InputStreamReader(context.openFileInput(saveFile));

            StringBuffer buffer = new StringBuffer();

            while(true){
                int c = reader.read();

                if(c == -1) break;

                buffer.append((char) c);
            }

            return loadMessagesFromJson(buffer.toString());
        }
        catch (FileNotFoundException ex){
            return new ArrayList<>();
        }
        catch (IOException ex){
            ex.printStackTrace();

            return new ArrayList<>();
        }
    }

    @NonNull
    public JsonObject toJson(){
        Gson gson = new Gson();

        JsonObject chat = new JsonObject();

        chat.addProperty("saveFile",mSaveFile);
        chat.add("owner",gson.toJsonTree(mOwner));
        chat.addProperty("title",mTitle);
        chat.add("messages",new JsonArray());

        return chat;
    }

    public static Chat createFromJson(@NonNull Context context,@NonNull JsonObject object,boolean loadMessages){
        Gson gson = new Gson();

        String title = object.get("title").getAsString();
        User owner = gson.fromJson(object.get("owner"),User.class);
        String saveFile = object.get("saveFile").getAsString();
        ArrayList<Message> messages = new ArrayList<>();

        /*if(loadMessages){
            messages = loadMessagesFromFile(context,saveFile);
        }*/

        Chat chat = new Chat(title,owner,context,messages);

        chat.setSaveFile(saveFile);

        return chat;
    }

    @NonNull
    private ArrayList<Message> getMessages() {
        return mMessages;
    }

    @NonNull
    public User getOwner() {
        return mOwner;
    }

    public void setOwner(@NonNull User owner) {
        mOwner = owner;
    }
}
