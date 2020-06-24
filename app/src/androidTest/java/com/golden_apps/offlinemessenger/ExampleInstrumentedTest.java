package com.golden_apps.offlinemessenger;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        File f = appContext.getFilesDir();

        deleteAll(f);

        f = appContext.getFilesDir();

        File f1 = appContext.getExternalFilesDir(null);

        deleteAll(f1);

        assert (f.list() == null);
    }

    public void deleteAll(File dir){
        File[] files = dir.listFiles();

        if(files == null) return;

        for(int i = 0;i<files.length;i++){
            if(files[i].isDirectory()){
                if(files[i].list() != null){
                    deleteAll(files[i]);
                }
            }

            files[i].delete();
        }
    }

    @Test
    public void clearExternalStorage(){

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        File root = context.getExternalFilesDir(null);

        deleteAll(root);
    }
}
