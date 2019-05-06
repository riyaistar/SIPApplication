package com.example.sipapplication;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public SipManager sipManager = null;
    public SipProfile sipProfile = null;
    public SipAudioCall call = null;
    public IncomingCallReceiver callReceiver;

    String username , serverdomain, password, receiver;
    private Button  makecall, endcall,register,unregister;
    private EditText peer, auth, server, pass;
    private ConstraintLayout call_layout, main_layout;
    private ImageButton back;
    String[] PERMISSIONS = {Manifest.permission.INTERNET,Manifest.permission.READ_PHONE_STATE, Manifest.permission.WAKE_LOCK,Manifest.permission.CALL_PHONE,Manifest.permission.PROCESS_OUTGOING_CALLS,Manifest.permission.ACCESS_WIFI_STATE,Manifest.permission.RECORD_AUDIO,Manifest.permission.DISABLE_KEYGUARD,Manifest.permission.READ_CONTACTS,Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.USE_SIP, Manifest.permission.MODIFY_AUDIO_SETTINGS};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestAllpermission();
        register = (Button)findViewById(R.id.register);
        unregister = (Button)findViewById(R.id.unregister);
        auth = (EditText)findViewById(R.id.auth);
        server = (EditText)findViewById(R.id.server);
        pass = (EditText)findViewById(R.id.pass);
        makecall = (Button)findViewById(R.id.makecall);
        endcall = (Button)findViewById(R.id.endcall);
        peer = (EditText) findViewById(R.id.peer);
        call_layout = (ConstraintLayout)findViewById(R.id.call_layout);
        main_layout = (ConstraintLayout)findViewById(R.id.main_layout);
        back = (ImageButton) findViewById(R.id.back);

        // Incoming call register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.SipDemo.INCOMING_CALL");
        callReceiver = new IncomingCallReceiver();
        this.registerReceiver(callReceiver, filter);



        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerSIP();
            }
        });
        unregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeLocalProfile();
            }
        });
        makecall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeCall();
            }
        });

        endcall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endCall();
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBack();
            }
        });
    }

    private void goBack() {
        main_layout.setVisibility(View.VISIBLE);
        call_layout.setVisibility(View.GONE);
    }

    private void registerSIP() {
        username = auth.getText().toString();
        serverdomain = server.getText().toString();
        password = pass.getText().toString();

        if (sipManager == null) {
            sipManager = SipManager.newInstance(this);
        }
        SipProfile.Builder builder = null;

        try {
            builder = new SipProfile.Builder(username, serverdomain);
            builder.setPassword(password);
            sipProfile = builder.build();
            Intent i = new Intent();
            i.setAction("android.SipDemo.INCOMING_CALL");
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, Intent.FILL_IN_DATA);
            sipManager.open(sipProfile, pi, null);

            sipManager.setRegistrationListener(sipProfile.getUriString(), new SipRegistrationListener() {
                public void onRegistering(String localProfileUri) {
                    updateStatus("Registering with SIP Server...");
                }
                public void onRegistrationDone(String localProfileUri, long expiryTime) {
                    updateStatus("Ready");

                }
                public void onRegistrationFailed(String localProfileUri, int errorCode,
                                                 String errorMessage) {

                    updateStatus(errorCode+" Registration error");
                }
            });
        } catch (ParseException pe) {
            pe.printStackTrace();
            updateStatus("Connection Error.");
        } catch (SipException se) {
            se.printStackTrace();
            updateStatus("Connection error.");
        }
        try {
            if(sipManager.isRegistered(sipProfile.getUriString())){
                main_layout.setVisibility(View.GONE);
                call_layout.setVisibility(View.VISIBLE);
            }
        } catch (SipException e) {
            e.printStackTrace();
        }

    }



    @Override
    protected void onDestroy() {
        closeLocalProfile();
        super.onDestroy();
    }

    public void closeLocalProfile() {
        if (sipManager == null) {
            return;
        }


        try {
            if (sipProfile != null) {
                sipManager.unregister(sipProfile, null);
                sipManager.close(sipProfile.getUriString());
                updateStatus("closed local profile.");

            }
        } catch (Exception ee) {
            updateStatus( "Failed to close local profile.");
        }
    }

    public boolean requestAllpermission(){
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


    private void makeCall() {
        receiver = peer.getText().toString();
        SipAudioCall.Listener listener = new SipAudioCall.Listener() {
            @Override
            public void onRinging(SipAudioCall call, SipProfile caller) {
                // TODO Auto-generated method stub
                Log.e("ringing", "ringing");

                super.onRinging(call, caller);
            }

            @Override
            public void onCallEstablished(SipAudioCall call) {
                call.startAudio();
                call.setSpeakerMode(false);
                if (call.isMuted()) {
                    Log.d(TAG, "Muted");
                    call.toggleMute();
                }

                updateStatus("onCallEstablished");

            }


            @Override

            public void onCallEnded(SipAudioCall call) {
                updateStatus("onCallEnded");
            }
        };
        try {
            call = sipManager.makeAudioCall(sipProfile.getUriString(), receiver, listener, 30);
        } catch (SipException e) {
            e.printStackTrace();
        }

    }

    private void endCall() {
        Log.d(TAG,"end call logger 1");
        if(call != null) {
            try {
                call.endCall();

            } catch (SipException se) {
                updateStatus("Error ending call");
            }

            call.close();
        }
    }

    public void updateStatus(final String status) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                TextView labelView = (TextView) findViewById(R.id.textView);
                labelView.setText(status);
            }
        });
    }

    @Override
    public void onBackPressed() {
        goBack();
    }
}
