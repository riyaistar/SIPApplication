package com.example.sipapplication;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class CallActivity extends AppCompatActivity {
    public SipAudioCall call = null;
    public SipManager sipManager = null;
    public SipProfile sipProfile = null;
    private ImageButton loudspeaker, mute,endcall;
    String receiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calling_screen);

        loudspeaker=(ImageButton)findViewById(R.id.loudspeaker);
        mute=(ImageButton)findViewById(R.id.mute);
        endcall=(ImageButton)findViewById(R.id.hang_up);

        loudspeaker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callLoudspeaker();
            }
        });
        mute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callmute();
            }
        });
        endcall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endCall();
            }
        });

    }

    private void callmute() {
        if (mute.getTag().toString().equalsIgnoreCase("false")) {
            Drawable drawable = ContextCompat.getDrawable(CallActivity.this, R.drawable.circle_button_solid).mutate();
            drawable.setColorFilter(getResources().getColor(R.color.input_border), PorterDuff.Mode.SRC_ATOP);
            mute.setBackgroundDrawable(drawable);
            mute.setTag("true");
        } else {
            mute.setBackgroundColor(getResources().getColor(R.color.transparent));
            mute.setTag("false");
        }
        Intent intent = new Intent(CallActivity.this, MyForeGroundService.class);
        intent.setAction(MyForeGroundService.CALL_MUTE);
        startService(intent);
    }

    private void callLoudspeaker() {
        if (loudspeaker.getTag().toString().equalsIgnoreCase("false")) {
            Drawable drawable = ContextCompat.getDrawable(CallActivity.this, R.drawable.circle_button_solid).mutate();
            drawable.setColorFilter(getResources().getColor(R.color.input_border), PorterDuff.Mode.SRC_ATOP);
            loudspeaker.setBackgroundDrawable(drawable);
            loudspeaker.setTag("true");
        } else {
            loudspeaker.setBackgroundColor(getResources().getColor(R.color.transparent));
            loudspeaker.setTag("false");
        }
        Intent intent = new Intent(CallActivity.this, MyForeGroundService.class);
        intent.putExtra("isspeakeron", loudspeaker.getTag().toString());
        intent.setAction(MyForeGroundService.CALL_ON_LOUDSPEAKER);
        startService(intent);
    }

    private void endCall() {
        Intent intent = new Intent(CallActivity.this, MyForeGroundService.class);
        intent.setAction(MyForeGroundService.ACTION_STOP_FOREGROUND_SERVICE);
        startService(intent);
    }

}
