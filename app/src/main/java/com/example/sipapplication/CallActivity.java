package com.example.sipapplication;

import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class CallActivity extends AppCompatActivity {
    public SipAudioCall call = null;
    public SipManager sipManager = null;
    public SipProfile sipProfile = null;
    private Button makecall, endcall;
    private EditText peer;
    String receiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
     /*   sipProfile = (SipProfile) getIntent().getSerializableExtra("sip_profile");
        sipManager = (SipManager) getIntent().getSerializableExtra("sip_manager");*/
        makecall = (Button)findViewById(R.id.makecall);
        endcall = (Button)findViewById(R.id.endcall);
        peer = (EditText) findViewById(R.id.peer);

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

    }


    private void makeCall() {
        receiver = peer.getText().toString();
        SipAudioCall.Listener listener = new SipAudioCall.Listener() {
            @Override
            public void onCallEstablished(SipAudioCall call) {
                call.startAudio();
                call.setSpeakerMode(true);
                call.toggleMute();
                updateStatus("onCallEstablished");
            }

            @Override

            public void onCallEnded(SipAudioCall call) {
                updateStatus("onCallEnded");
            }
        };
        try {
            sipManager.makeAudioCall(sipProfile.getUriString(), receiver, listener, 30);
        } catch (SipException e) {
            e.printStackTrace();
        }

    }


    private void endCall() {
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

}
