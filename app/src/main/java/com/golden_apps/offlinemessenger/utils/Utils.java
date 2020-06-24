package com.golden_apps.offlinemessenger.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import com.golden_apps.offlinemessenger.Message;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class Utils {

    public static int readInt(InputStream is) throws IOException {
        byte[] bytes = new byte[4];

        if(is.read(bytes) < 4){
            return -1;
        }

        return ByteBuffer.wrap(bytes).getInt();
    }

    public static Message.Binary getBinaryFromUri(Context context, Uri uri){
        String[] projection =
                {
                        MediaStore.Files.FileColumns.DISPLAY_NAME
                        ,MediaStore.Files.FileColumns.SIZE
                        ,MediaStore.Files.FileColumns.MIME_TYPE
                        , MediaStore.Files.FileColumns.TITLE
                };

        Cursor cursor = context.getContentResolver().query(uri,projection,null,null,null);

        if(cursor != null){
            if(cursor.moveToFirst()){
                try{
                    int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
                    int displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
                    int mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE);
                    int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.TITLE);

                    Message.Binary binary = new Message.Binary();

                    binary.setUri(uri);
                    binary.setMimeType(cursor.getString(mimeTypeColumn));
                    binary.setFilename(cursor.getString(displayNameColumn));
                    binary.setFileSize(cursor.getInt(sizeColumn));
                    binary.setTitle(cursor.getString(titleColumn));

                    return binary;
                }
                catch (Exception ex){
                    return null;
                }
            }
        }

        return null;
    }

}
