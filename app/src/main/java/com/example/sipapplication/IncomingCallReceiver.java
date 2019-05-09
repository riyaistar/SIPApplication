package com.example.sipapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.util.Log;


/**
 * Listens for incoming SIP calls, intercepts and hands them off to WalkieTalkieActivity.
 */
public class IncomingCallReceiver extends BroadcastReceiver {
    private static final String TAG = "MyForeGroundService";

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d(TAG,"receiver");
        Intent intent1 = new Intent(context, MyForeGroundService.class);
        intent1.putExtra(Intent.EXTRA_INTENT, intent);

        intent1.setAction(MyForeGroundService.ACTION_INCOMING_CALL);
        context.startService(intent1);
    }
}

