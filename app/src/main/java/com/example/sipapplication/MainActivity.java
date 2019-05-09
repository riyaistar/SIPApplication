package com.example.sipapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
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

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public SipManager sipManager = null;
    public SipProfile sipProfile = null;
    public SipAudioCall call = null;
    public IncomingCallReceiver callReceiver;
    public SipRegistrationListener listener;
    String username, serverdomain, password, receiver;
    private Button register, unregister;
    private EditText auth, server, pass;
    public ConstraintLayout call_layout, main_layout, incoming_layout;
    private TextView caller_name_number;
    private Chronometer call_timer;
    private ImageButton hang_up, loudspeaker, mute, hangUp, answerCall;
    String[] PERMISSIONS = {Manifest.permission.INTERNET, Manifest.permission.READ_PHONE_STATE, Manifest.permission.WAKE_LOCK, Manifest.permission.CALL_PHONE, Manifest.permission.PROCESS_OUTGOING_CALLS, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.RECORD_AUDIO, Manifest.permission.DISABLE_KEYGUARD, Manifest.permission.READ_CONTACTS, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.USE_SIP, Manifest.permission.MODIFY_AUDIO_SETTINGS};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestAllpermission();
        register = (Button) findViewById(R.id.registerSIP);
        unregister = (Button) findViewById(R.id.unregister);
        auth = (EditText) findViewById(R.id.auth);
        server = (EditText) findViewById(R.id.server);
        pass = (EditText) findViewById(R.id.pass);
        call_layout = (ConstraintLayout) findViewById(R.id.call_layout);
        main_layout = (ConstraintLayout) findViewById(R.id.main_layout);
        incoming_layout = (ConstraintLayout) findViewById(R.id.incoming);

        caller_name_number = (TextView) findViewById(R.id.caller_name_number);
        call_timer = (Chronometer) findViewById(R.id.call_timer);
        hang_up = (ImageButton) findViewById(R.id.hang_up);
        loudspeaker = (ImageButton) findViewById(R.id.loudspeaker);
        mute = (ImageButton) findViewById(R.id.mute);
        answerCall = (ImageButton) findViewById(R.id.answerCall);
        hangUp = (ImageButton) findViewById(R.id.hangUp);

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
        loudspeaker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleloudspeaker();
            }
        });
        mute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglemute();
            }
        });
        hang_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endCall();
            }
        });
    }

    private void togglemute() {
        if (call != null) {
                call.toggleMute();
            if(call.isMuted() == true){
                Drawable drawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.circle_button_solid).mutate();
                drawable.setColorFilter(getResources().getColor(R.color.input_border), PorterDuff.Mode.SRC_ATOP);
                mute.setBackgroundDrawable(drawable);
            }else{
                mute.setBackgroundColor(getResources().getColor(R.color.transparent));
            }
        }
    }

    private void toggleloudspeaker() {
        if (call != null) {
            if (loudspeaker.getTag().toString().equalsIgnoreCase("false")) {
                call.setSpeakerMode(true);
                Drawable drawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.circle_button_solid).mutate();
                drawable.setColorFilter(getResources().getColor(R.color.input_border), PorterDuff.Mode.SRC_ATOP);
                loudspeaker.setBackgroundDrawable(drawable);
                loudspeaker.setTag("true");
            } else {
                call.setSpeakerMode(false);
                loudspeaker.setBackgroundColor(getResources().getColor(R.color.transparent));
                loudspeaker.setTag("false");
            }
        }
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

            listener = new SipRegistrationListener() {
                @Override
                public void onRegistering(String localProfileUri) {
                    updateStatus("Registering with SIP Server...");

                }

                @Override
                public void onRegistrationDone(String localProfileUri, long expiryTime) {
                    updateStatus("Ready");
                    register.setText("Make a Call");

                }

                @Override
                public void onRegistrationFailed(String localProfileUri, int errorCode, String errorMessage) {
                    updateStatus(errorCode + " Registration error");

                }
            };

            sipManager.setRegistrationListener(sipProfile.getUriString(), listener);

        } catch (ParseException pe) {
            pe.printStackTrace();
            updateStatus("Connection Error.");
        } catch (SipException se) {
            se.printStackTrace();
            updateStatus("Connection error.");
        }
        try {
            if (sipManager.isRegistered(sipProfile.getUriString())) {
                showCustomDialog();

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
                sipManager.unregister(sipProfile, listener);
                sipManager.close(sipProfile.getUriString());
                updateStatus("closed local profile.");
                register.setText("Register");
            }
        } catch (Exception ee) {
            updateStatus("Failed to close local profile.");
        }

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


    private void makeCall(String receiver) {
        main_layout.setVisibility(View.GONE);
        call_layout.setVisibility(View.VISIBLE);
        caller_name_number.setText(receiver);
        SipAudioCall.Listener listener = new SipAudioCall.Listener() {

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
            call = sipManager.makeAudioCall(sipProfile.getUriString(), receiver + "@" + serverdomain, listener, 30);
        } catch (SipException e) {
            e.printStackTrace();
        }

    }


    private void endCall() {
        Log.d(TAG, "end call logger 1");
        if (call != null) {
            try {
                call.endCall();

            } catch (SipException se) {
                updateStatus("Error ending call");
            }

            call.close();
        }
        call_layout.setVisibility(View.GONE);
        main_layout.setVisibility(View.VISIBLE);
    }

    public void updateStatus(final String status) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                TextView labelView = (TextView) findViewById(R.id.textView);
                labelView.setText(status);
            }
        });
    }

    private void showCustomDialog() {
        //before inflating the custom alert dialog layout, we will get the current activity viewgroup
        final ViewGroup viewGroup = findViewById(android.R.id.content);

        //then we will inflate the custom alert dialog xml that we created
        View dialogView = LayoutInflater.from(this).inflate(R.layout.activity_call, viewGroup, false);


        //Now we need an AlertDialog.Builder object
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText peer = (EditText) dialogView.findViewById(R.id.peer);

        //setting the view of the builder to our custom view that we already inflated
        builder.setView(dialogView);
        builder.setPositiveButton("Call", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                receiver = peer.getText().toString();
                makeCall(receiver);
                // sign in the user ...
            }
        })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        //finally creating the alert dialog and displaying it
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    @Override
    public void onBackPressed() {
        goBack();
    }
}
