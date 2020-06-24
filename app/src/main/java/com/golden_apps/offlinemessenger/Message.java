package com.golden_apps.offlinemessenger;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.golden_apps.offlinemessenger.io.WifiDirectManager;
import com.golden_apps.offlinemessenger.utils.Utils;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.function.BiFunction;

public class Message{

    public static final String TEXT_PLAIN = "text/plain";
    public static final String IMAGE_JPEG = "image/jpeg";
    public static final String IMAGE_PNG = "image/png";

    public static final int JSON_MESSAGE = 0;
    public static final int BINARY_MESSAGE = 1;


    public static class Binary{
        private String mTitle;
        private String mFilename;
        private int mFileSize;
        private String mMimeType;
        private transient Uri mUri;

        public Binary(Uri uri,String title, String filename, int fileSize,String mimeType) {
            mTitle = title;
            mFilename = findNotUsedFilename(filename);
            mFileSize = fileSize;
            mMimeType = mimeType;
            mUri = uri;

            File saveFile = new File(getSavePath());
            File saveDir = saveFile.getParentFile();

            if(!saveDir.exists()){
                if(!saveDir.mkdirs()){
                    Log.e(WifiDirectManager.TAG,"Cannot save image.");
                }
            }
        }

        public Binary() {
            mTitle = null;
            mFilename = null;
            mFileSize = 0;
        }

        public String getTitle() {
            return mTitle;
        }

        public void setTitle(String title) {
            mTitle = title;
        }

        public String getFilename() {
            return mFilename;
        }

        public void setFilename(String filename) {
            mFilename = filename;

            if(mUri == null) mUri = Uri.fromFile(new File(getSavePath()));
        }

        public int getFileSize() {
            return mFileSize;
        }

        public void setFileSize(int fileSize) {
            mFileSize = fileSize;
        }

        public String getMimeType() {
            return mMimeType;
        }

        public void setMimeType(String mimeType) {
            mMimeType = mimeType;
        }

        public Uri getUri() {
            return mUri;
        }

        @NonNull
        public String getSavePath(){
            return Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    + "/Offline Messenger/" + getFilename();
        }

        public boolean isImage(){
            return mMimeType.startsWith("image/");
        }

        public boolean isText(){
            return mMimeType.startsWith("text/");
        }

        public void setUri(Uri uri) {
            mUri = uri;
        }
    }


    @NonNull private User mAuthor;
    @NonNull private String mText;

    private Binary mAttachedFile;
    private Binary mAttachedImage;

    public Message(Context context,String json){
        Gson gson = new Gson();

        Message message = gson.fromJson(json,Message.class);

        mText = message.getText();
        mAuthor = message.getAuthor();
        mAttachedFile = message.getAttachedFile();
        mAttachedImage = message.getAttachedImage();
    }

    public Message(Context context, @NonNull User author, @NonNull String text) {
        mAuthor = author;
        mText = text;

        mAttachedImage = null;
        mAttachedFile = null;
    }

    @NonNull
    public User getAuthor() {
        return mAuthor;
    }

    public void setAuthor(@NonNull User author) {
        mAuthor = author;
    }

    @NonNull
    public String getText() {
        return mText;
    }

    public void setText(@NonNull String text) {
        mText = text;
    }

    @NonNull
    @Override
    public String toString() {
        Gson gson = new Gson();

        return gson.toJson(this);
    }

    public boolean hasImage(){
        return (mAttachedImage != null);
    }

    public boolean hasFile(){
        return (mAttachedFile != null);
    }

    public void loadImage(Context context,Uri image){
        String[] projection =
                new String[]{
                        MediaStore.Images.Media.SIZE
                        , MediaStore.Images.Media.TITLE
                        , MediaStore.Images.Media.MIME_TYPE};

        Cursor cursor = context.getContentResolver().query(image,projection
                , null
                , null
                ,null);

        cursor.moveToFirst();

        int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
        int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE);
        int mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE);

        Binary imageBinary = new Binary();

        imageBinary.setFileSize(cursor.getInt(sizeColumn));
        imageBinary.setMimeType(cursor.getString(mimeTypeColumn));
        imageBinary.setTitle(cursor.getString(titleColumn));

        imageBinary.setUri(image);

        if(imageBinary.getMimeType().contains("jpeg")){
            imageBinary.setFilename(imageBinary.getTitle() + ".jpg");
        }
        else if(imageBinary.getMimeType().contains("/")){
            imageBinary.setFilename(imageBinary.getTitle() + "." + imageBinary.getMimeType().split("/")[1]);
        }
        else{
            imageBinary.setFilename(imageBinary.getTitle() + ".jpg");
        }

        mAttachedImage = imageBinary;
    }

    public void loadFile(Context context,Uri uri){
        mAttachedFile = Utils.getBinaryFromUri(context,uri);
    }

    private static String findNotUsedFilename(String source){
        String filenameWithoutSuffix = source.substring(0,source.lastIndexOf("."));
        String suffix = source.substring(source.lastIndexOf("."));

        int i = 1;

        while(true){
            File f = new File(filenameWithoutSuffix);
            if(!f.exists()) break;

            filenameWithoutSuffix = filenameWithoutSuffix + i;
            i++;
        }

        return filenameWithoutSuffix + suffix;
    }

    public Binary getAttachedFile() {
        return mAttachedFile;
    }

    public void setAttachedFile(Binary attachedFile) {
        mAttachedFile = attachedFile;
    }

    public Binary getAttachedImage() {
        return mAttachedImage;
    }

    public void setAttachedImage(Binary attachedImage) {
        mAttachedImage = attachedImage;
    }
}
