package com.example.sipapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.os.SystemClock;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class ServiceActivity extends AppCompatActivity {
    private static final String TAG = "ServiceActivity";
    private EditText auth, server, pass, peer;
    private Button register, unregister;
    private ImageButton hang_up, loudspeaker, mute, makecall;
    public IncomingCallReceiver callReceiver;
    public MyCallReceiver myCallReceiver;
    String username, serverdomain, password, receiver;
    private SharedPreferences sharedPreferences;
    private Chronometer timer;
    SharedPreferences.Editor editor;
    private Boolean isOnCall, isRegistered = false;
    private TextView receiver_number, receiver_name;
    public ConstraintLayout call_layout, main_layout, dialscreen;
    String[] PERMISSIONS = {Manifest.permission.INTERNET, Manifest.permission.READ_PHONE_STATE, Manifest.permission.WAKE_LOCK, Manifest.permission.CALL_PHONE, Manifest.permission.PROCESS_OUTGOING_CALLS, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.RECORD_AUDIO, Manifest.permission.DISABLE_KEYGUARD, Manifest.permission.READ_CONTACTS, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.USE_SIP, Manifest.permission.MODIFY_AUDIO_SETTINGS, Manifest.permission.FOREGROUND_SERVICE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);
        requestAllpermission();
        auth = (EditText) findViewById(R.id.auth);
        server = (EditText) findViewById(R.id.server);
        pass = (EditText) findViewById(R.id.pass);
        peer = (EditText) findViewById(R.id.peer);

        register = (Button) findViewById(R.id.registerSIP);
        makecall = (ImageButton) findViewById(R.id.makecall);
        unregister = (Button) findViewById(R.id.unregister);
        hang_up = (ImageButton) findViewById(R.id.hang_up);
        loudspeaker = (ImageButton) findViewById(R.id.loudspeaker);
        mute = (ImageButton) findViewById(R.id.mute);
        call_layout = (ConstraintLayout) findViewById(R.id.call_layout);
        main_layout = (ConstraintLayout) findViewById(R.id.main_layout);
        dialscreen = (ConstraintLayout) findViewById(R.id.dialscreen);
        timer = (Chronometer) findViewById(R.id.call_timer);
        receiver_number = (TextView) findViewById(R.id.caller_name_number);
        receiver_name = (TextView) findViewById(R.id.receiver_number);

        IntentFilter filter = new IntentFilter();
        filter.addAction("incomingCall");
        callReceiver = new IncomingCallReceiver();
        this.registerReceiver(callReceiver, filter);

        isRegistered = getIntent().getBooleanExtra("isRegistered",false);

        isOnCall = getIntent().getBooleanExtra("isOnCall",false);

        if(isRegistered){
            if(isOnCall != null && isOnCall == false){
                call_layout.setVisibility(View.GONE);
                dialscreen.setVisibility(View.VISIBLE);
            }else{
                call_layout.setVisibility(View.VISIBLE);
                dialscreen.setVisibility(View.GONE);
            }
        }else{
            main_layout.setVisibility(View.VISIBLE);
            call_layout.setVisibility(View.GONE);
        }


        sharedPreferences =getSharedPreferences(getResources().getString(R.string.shared_preference_key),MODE_PRIVATE);
        editor = sharedPreferences.edit();

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerSIP();
            }
        });

        makecall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeCall();
            }
        });

        hang_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endCall();
            }
        });

        mute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callmute();
            }
        });

        loudspeaker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callLoudspeaker();
            }

        });

        unregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unregisterService();
            }
        });
    }

    private void unregisterService() {
        Intent intent = new Intent(ServiceActivity.this, MyForeGroundService.class);
        intent.setAction(MyForeGroundService.ACTION_UNREGISTER_SIP);
        startService(intent);
    }

    private void callmute() {
        if (mute.getTag().toString().equalsIgnoreCase("false")) {
            Drawable drawable = ContextCompat.getDrawable(ServiceActivity.this, R.drawable.circle_button_solid).mutate();
            drawable.setColorFilter(getResources().getColor(R.color.input_border), PorterDuff.Mode.SRC_ATOP);
            mute.setBackgroundDrawable(drawable);
            mute.setTag("true");
        } else {
            mute.setBackgroundColor(getResources().getColor(R.color.transparent));
            mute.setTag("false");
        }
        Intent intent = new Intent(ServiceActivity.this, MyForeGroundService.class);
        intent.setAction(MyForeGroundService.CALL_MUTE);
        startService(intent);
    }


    private void callLoudspeaker() {
        if (loudspeaker.getTag().toString().equalsIgnoreCase("false")) {
            Drawable drawable = ContextCompat.getDrawable(ServiceActivity.this, R.drawable.circle_button_solid).mutate();
            drawable.setColorFilter(getResources().getColor(R.color.input_border), PorterDuff.Mode.SRC_ATOP);
            loudspeaker.setBackgroundDrawable(drawable);
            loudspeaker.setTag("true");
        } else {
            loudspeaker.setBackgroundColor(getResources().getColor(R.color.transparent));
            loudspeaker.setTag("false");
        }
        Intent intent = new Intent(ServiceActivity.this, MyForeGroundService.class);
        intent.putExtra("isspeakeron", loudspeaker.getTag().toString());
        intent.setAction(MyForeGroundService.CALL_ON_LOUDSPEAKER);
        startService(intent);
    }

    private void endCall() {
        call_layout.setVisibility(View.GONE);
        dialscreen.setVisibility(View.VISIBLE);
        timer.stop();
        timer.setBase(SystemClock.elapsedRealtime());

        Intent intent = new Intent(ServiceActivity.this, MyForeGroundService.class);
        intent.setAction(MyForeGroundService.ACTION_STOP_FOREGROUND_SERVICE);
        startService(intent);

    }

    private void makeCall() {
        receiver = peer.getText().toString();
        if(receiver != null && !receiver.equalsIgnoreCase("")) {
            editor.putString(SharedPrefKey.RECEIVER, receiver);
            receiver_number.setText(receiver);
        }
        editor.apply();
        editor.commit();
        dialscreen.setVisibility(View.GONE);
        call_layout.setVisibility(View.VISIBLE);

        Intent intent = new Intent(ServiceActivity.this, MyForeGroundService.class);
        intent.setAction(MyForeGroundService.ACTION_START_FOREGROUND_SERVICE);
        startService(intent);
        timer.setBase(SystemClock.elapsedRealtime());
        timer.start();

    }

    private void registerSIP() {
        username = auth.getText().toString();
        serverdomain = server.getText().toString();
        password = pass.getText().toString();
        if(username != null && !username.equalsIgnoreCase("")) {
            editor.putString(SharedPrefKey.USERNAME, username);
        }
        if(serverdomain!= null && !serverdomain.equalsIgnoreCase("")){
            editor.putString(SharedPrefKey.SERVER_DOMAIN, serverdomain);
        }
        if(password != null && !password.equalsIgnoreCase("")){
            editor.putString(SharedPrefKey.PASSWORD, password);
        }
        editor.apply();
        editor.commit();
        main_layout.setVisibility(View.GONE);
        dialscreen.setVisibility(View.VISIBLE);
        Intent intent = new Intent(ServiceActivity.this, MyForeGroundService.class);
        intent.setAction(MyForeGroundService.ACTION_REGISTER_SIP);
        startService(intent);
    }

    public boolean requestAllpermission() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : PERMISSIONS) {
            result = ContextCompat.checkSelfPermission(getApplicationContext(), p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 200);
            return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        dialscreen.setVisibility(View.GONE);
        main_layout.setVisibility(View.VISIBLE);
    }
}


