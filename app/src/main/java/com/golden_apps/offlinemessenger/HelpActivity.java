package com.golden_apps.offlinemessenger;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.webkit.WebView;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        WebView helpView = findViewById(R.id.help_text);

        AssetManager assetManager = getAssets();

        try{
            Reader reader = new InputStreamReader(assetManager.open("help.html"),StandardCharsets.UTF_8);

            StringBuffer buff = new StringBuffer();

            int c;

            while((c = reader.read()) != -1){
                buff.append((char) c);
            }

            helpView.loadData(buff.toString(),"text/html","utf-8");
        }
        catch (Exception ex){

        }
    }
}
