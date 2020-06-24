package com.golden_apps.offlinemessenger;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.golden_apps.offlinemessenger.fragments.ChatListFragment;
import com.golden_apps.offlinemessenger.io.ConnectionListener;
import com.golden_apps.offlinemessenger.io.ServiceDiscoveringStateListener;
import com.golden_apps.offlinemessenger.io.WifiDirectManager;

import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences settings;

    private ChatListFragment mListFragment;

    private EditText mUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CHANGE_NETWORK_STATE,
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.ACCESS_MEDIA_LOCATION});
        }
        else{
            requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CHANGE_NETWORK_STATE,
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_NETWORK_STATE});
        }

        try{
            if(getIntent().getBooleanExtra("disconnected",false)){
                openInfoDialog(this,"Offline Messenger","Disconnected.");

                setIntent(null);
            }
        } catch (NullPointerException ex){
            //Ignore
        }

        WifiDirectManager.getInstance().setConnectionListener(new ConnectionListener() {
            @Override
            public void onConnected() {

            }

            @Override
            public void onDisconnected() {
                openInfoDialog(MainActivity.this,"Offline Messenger","Disconnected.");
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        settings = getSharedPreferences("Main",MODE_PRIVATE);

        mUserName = findViewById(R.id.user_name);

        if(!settings.contains("username")){
            openChangeOrCreateUsernameDialog(true);
        }
        else{
            User.SELF.setUserName(settings.getString("username","self"));

            mUserName.setText(settings.getString("username","self"));
        }

        User.SELF.setAddress(getMacAddr());

        mUserName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openChangeOrCreateUsernameDialog(false);
            }
        });

        mListFragment = new ChatListFragment();

        mListFragment.setDiscoveringStateListener(new ServiceDiscoveringStateListener() {
            @Override
            public void onDiscoveringStarted() {

            }

            @Override
            public void onDiscoveringFinished() {

            }
        });

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container,mListFragment)
                .commit();

        hideKeyboard(this);

        Button createChat = findViewById(R.id.create_chat);

        createChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListFragment = (ChatListFragment)
                        getSupportFragmentManager().findFragmentById(R.id.fragment_container);

                mListFragment.createChat();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
    }

    public String getMacAddr() {
        WifiDirectManager.getInstance().enableWifi();

        while(!WifiDirectManager.getInstance().isWifiEnabled()){};

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        return wifiManager.getConnectionInfo().getMacAddress();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);

        Drawable drawable = menu.findItem(R.id.action_help).getIcon();

        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, ContextCompat.getColor(this,R.color.textColorDefault));
        menu.findItem(R.id.action_help).setIcon(drawable);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId() == R.id.action_help){
            startActivity(new Intent(MainActivity.this,HelpActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    private void openChangeOrCreateUsernameDialog(final boolean create){
        final EditText usernameEdit = new EditText(this);

        usernameEdit.setHint(R.string.enter_username);
        usernameEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(15)});

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        if(create){
            dialogBuilder.setTitle(R.string.create_user);
        }
        else{
            dialogBuilder.setTitle(R.string.change_username);
        }

        dialogBuilder.setCancelable(true);

        dialogBuilder.setView(usernameEdit);

        dialogBuilder.setNegativeButton(android.R.string.cancel,null);

        dialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                String username = usernameEdit.getText().toString();

                if(username.isEmpty()){
                    dialogInterface.dismiss();

                    openChangeOrCreateUsernameDialog(create);
                }
                else{
                    setUsername(username);

                    dialogInterface.dismiss();
                }

            }
        });

        dialogBuilder.show();

    }

    public static void openInfoDialog(Context context,String title, String text){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(title);
        builder.setMessage(text);

        builder.setCancelable(false);
        builder.setPositiveButton(android.R.string.ok,null);

        builder.show();
    }

    private void requestPermissions(String[] permissions){
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermission(permission);
            }
        }
    }

    private void requestPermission(String permission){
        ActivityCompat.requestPermissions(this,new String[]{permission},1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 1){
            for(int i = 0;i<grantResults.length;i++){
                if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                    requestPermission(permissions[i]);
                }
            }
        }
    }

    private void setUsername(String username){
        settings.edit()
                .clear()
                .putString("username",username)
                .apply();

        User.SELF.setUserName(username);

        mUserName.setText(username);

        mListFragment = new ChatListFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container,mListFragment)
                .commit();
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}