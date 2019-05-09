package com.example.sipapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import java.text.ParseException;

public class MyForeGroundService extends Service {
    private static final String TAG = "MyForeGroundService";
    public SipManager sipManager = null;
    public SipProfile sipProfile = null;
    public SipAudioCall call = null;
    public SipRegistrationListener listener;
    String username, serverdomain, password, receiver;
    private Context context;
    public static final String PRIMARY_CHANNEL = "default";
    public static final String CHANNEL_NAME = "notification_channel";
    public static final String CHANNEL_DESC = "notification_channel_description";
    private SharedPreferences sharedPreferences;
    private static final String TAG_FOREGROUND_SERVICE = "FOREGROUND_SERVICE";
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    public static final String ACTION_REGISTER_SIP = "ACTION_REGISTER_SIP";
    public static final String ACTION_INCOMING_CALL = "ACTION_INCOMING_CALL";
    public static final String CALL_ON_LOUDSPEAKER = "CALL_ON_LOUDSPEAKER";
    public static final String ACTION_TAKE_CALL = "android.SipDemo.INCOMING_CALL";
    public static final String CALL_MUTE = "CALL_MUTE";
    public static final String ACTION_UNREGISTER_SIP = "ACTION_UNREGISTER_SIP";


    public MyForeGroundService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.shared_preference_key), MODE_PRIVATE);

        Log.d(TAG, "My foreground service onCreate().");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_START_FOREGROUND_SERVICE:
                    startForegroundService();
                    Toast.makeText(getApplicationContext(), "Foreground service is started.", Toast.LENGTH_LONG).show();
                    break;
                case ACTION_STOP_FOREGROUND_SERVICE:
                    stopForegroundService();
                    Toast.makeText(getApplicationContext(), "Foreground service is stopped.", Toast.LENGTH_LONG).show();
                    break;
                case ACTION_REGISTER_SIP:
                    registerSIP();
                    break;
                case ACTION_UNREGISTER_SIP:
                    closeLocalProfile();
                    stopForegroundService();
                    break;
                case ACTION_TAKE_CALL:
                    Intent intent2 = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                    intent2.setAction(ACTION_TAKE_CALL);
                    takeCall(intent2);
                    Toast.makeText(getApplicationContext(), "You click Play button.", Toast.LENGTH_LONG).show();
                    break;
                case CALL_ON_LOUDSPEAKER:
                    String isspeakeron = intent.getStringExtra("isspeakeron");
                    togglespeaker(isspeakeron);
                    Toast.makeText(getApplicationContext(), "You click Play button.", Toast.LENGTH_LONG).show();
                    break;
                case CALL_MUTE:
                    togglemute();
                    Toast.makeText(getApplicationContext(), "You click Pause button.", Toast.LENGTH_LONG).show();
                    break;
                case ACTION_INCOMING_CALL:
                    Intent intent1 = intent.getParcelableExtra(Intent.EXTRA_INTENT);

                    startIncomingCallService(intent1);
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void togglespeaker(String isspeakeron) {
        if (call != null) {
            if (isspeakeron == "false") {
                call.setSpeakerMode(true);
            } else {
                call.setSpeakerMode(false);
            }
        }
    }

    private void togglemute() {
        if (call != null) {
            call.toggleMute();
        }
    }

    private void startIncomingCallService(Intent intent1) {


        Log.d(TAG_FOREGROUND_SERVICE, "Start foreground service.");

        Intent intent = new Intent(this, CallActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);


        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, PRIMARY_CHANNEL);
        mBuilder.setContentTitle("SIP notification");
        mBuilder.setContentText("Android foreground service is a android service which can run in foreground always, it can be controlled by user via notification.");
        mBuilder.setColor(getResources().getColor(R.color.theme_color));
        mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH); //for heads-up
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setAutoCancel(true);


        // Add Play button intent in notification.

        Intent playIntent = new Intent(this, MyForeGroundService.class);
        playIntent.putExtra(Intent.EXTRA_INTENT, intent1);
        playIntent.setAction(ACTION_TAKE_CALL);
        PendingIntent pendingPlayIntent = PendingIntent.getService(this, 0, playIntent, 0);
        NotificationCompat.Action playAction = new NotificationCompat.Action(android.R.drawable.ic_media_play, "Answer", pendingPlayIntent);
        mBuilder.addAction(playAction);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBuilder.setSmallIcon(R.drawable.ic_launcher_foreground);
            mBuilder.setColor(Color.RED);
        } else {
            mBuilder.setSmallIcon(R.drawable.ic_launcher_foreground);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = CHANNEL_NAME;
            String description = CHANNEL_DESC;
            int importance = NotificationManager.IMPORTANCE_HIGH;   //heads-up
            NotificationChannel channel = new NotificationChannel(PRIMARY_CHANNEL, name, importance);
            channel.enableLights(true);
            channel.setLightColor(getResources().getColor(R.color.theme_color));
            channel.enableVibration(true);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this

            NotificationManager notificationManager1 = getSystemService(NotificationManager.class);
            notificationManager1.createNotificationChannel(channel);

        }
        // Start foreground service.
        startForeground(1, mBuilder.build());
    }

    private void takeCall(Intent intent) {
        Log.d(TAG, "takeCall");
        SipAudioCall incomingCall = null;

        try {

            SipAudioCall.Listener listener1 = new SipAudioCall.Listener() {
                @Override
                public void onRinging(SipAudioCall call, SipProfile caller) {
                    try {
                        call.answerCall(30);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onCallEnded(SipAudioCall call) {
                }
            };

            incomingCall = sipManager.takeAudioCall(intent, listener1);
            incomingCall.answerCall(30);
            incomingCall.startAudio();
            incomingCall.setSpeakerMode(true);
            if (!incomingCall.isMuted()) {
                incomingCall.toggleMute();
            }

            call = incomingCall;

        } catch (Exception e) {
            if (incomingCall != null) {
                incomingCall.close();
            }
        }
    }


    /* Used to build and start foreground service. */
    private void startForegroundService() {
        Log.d(TAG_FOREGROUND_SERVICE, "Start foreground service.");

        Intent intent = new Intent(this, CallActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);


        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, PRIMARY_CHANNEL);
        mBuilder.setContentTitle("SIP notification");
        mBuilder.setContentText("Android foreground service is a android service which can run in foreground always, it can be controlled by user via notification.");
        mBuilder.setColor(getResources().getColor(R.color.theme_color));
        mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH); //for heads-up
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setAutoCancel(true);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBuilder.setSmallIcon(R.drawable.ic_launcher_foreground);

            mBuilder.setColor(Color.RED);
        } else {
            mBuilder.setSmallIcon(R.drawable.ic_launcher_foreground);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = CHANNEL_NAME;
            String description = CHANNEL_DESC;
            int importance = NotificationManager.IMPORTANCE_HIGH;   //heads-up
            NotificationChannel channel = new NotificationChannel(PRIMARY_CHANNEL, name, importance);
            channel.enableLights(true);
            channel.setLightColor(getResources().getColor(R.color.theme_color));
            channel.enableVibration(true);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this

            NotificationManager notificationManager1 = getSystemService(NotificationManager.class);
            notificationManager1.createNotificationChannel(channel);

        }
        // Start foreground service.
        startForeground(1, mBuilder.build());
        makeCall();


    }

    private void stopForegroundService() {
        Log.d(TAG_FOREGROUND_SERVICE, "Stop foreground service.");

        // Stop foreground service and remove the notification.
        stopForeground(true);

        // Stop the foreground service.
        stopSelf();

        endCall();

    }

    private void registerSIP() {

        username = sharedPreferences.getString(SharedPrefKey.USERNAME, "");
        serverdomain = sharedPreferences.getString(SharedPrefKey.SERVER_DOMAIN, "");
        password = sharedPreferences.getString(SharedPrefKey.PASSWORD, "");
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
                    Toast.makeText(getApplicationContext(), "Registering with Sip....", Toast.LENGTH_SHORT).show();

                }

                @Override
                public void onRegistrationDone(String localProfileUri, long expiryTime) {
                    Toast.makeText(getApplicationContext(), "Ready", Toast.LENGTH_LONG).show();

                }

                @Override
                public void onRegistrationFailed(String localProfileUri, int errorCode, String errorMessage) {
                    Toast.makeText(getApplicationContext(), "Sip could not be registered. Registration failed", Toast.LENGTH_LONG).show();

                }
            };

            sipManager.setRegistrationListener(sipProfile.getUriString(), listener);

        } catch (ParseException pe) {
            pe.printStackTrace();
            Toast.makeText(getApplicationContext(), "Connection Error", Toast.LENGTH_LONG).show();
        } catch (SipException se) {
            se.printStackTrace();
            Toast.makeText(getApplicationContext(), "Connection error", Toast.LENGTH_LONG).show();
        }

        try {
            if (sipManager.isRegistered(sipProfile.getUriString())) {
                Toast.makeText(getApplicationContext(), "Registeration Successful", Toast.LENGTH_LONG).show();


                Intent intent = new Intent(this, ServiceActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);


                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, PRIMARY_CHANNEL);
                mBuilder.setContentTitle("SIP notification");
                mBuilder.setContentText("Android registeration Notification");
                mBuilder.setColor(getResources().getColor(R.color.theme_color));
                mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH); //for heads-up
                mBuilder.setContentIntent(pendingIntent);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mBuilder.setSmallIcon(R.drawable.ic_launcher_foreground);

                    mBuilder.setColor(Color.RED);
                } else {
                    mBuilder.setSmallIcon(R.drawable.ic_launcher_foreground);
                }

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    CharSequence name = CHANNEL_NAME;
                    String description = CHANNEL_DESC;
                    int importance = NotificationManager.IMPORTANCE_HIGH;   //heads-up
                    NotificationChannel channel = new NotificationChannel(PRIMARY_CHANNEL, name, importance);
                    channel.enableLights(true);
                    channel.setLightColor(getResources().getColor(R.color.theme_color));
                    channel.enableVibration(true);
                    channel.setDescription(description);
                    // Register the channel with the system; you can't change the importance
                    // or other notification behaviors after this

                    NotificationManager notificationManager1 = getSystemService(NotificationManager.class);
                    notificationManager1.createNotificationChannel(channel);

                }

                startForeground(2, mBuilder.build());


            } else {
                Toast.makeText(getApplicationContext(), "Connection Error", Toast.LENGTH_LONG).show();
            }
        } catch (SipException e) {
            e.printStackTrace();
        }


    }

    private void makeCall() {
        receiver = sharedPreferences.getString(SharedPrefKey.RECEIVER, "");
        if (receiver != null) {

            try {
                if (sipManager != null && sipManager.isRegistered(sipProfile.getUriString())) {

                    SipAudioCall.Listener listener = new SipAudioCall.Listener() {

                        @Override
                        public void onCallEstablished(SipAudioCall call) {
                            call.startAudio();
                            call.setSpeakerMode(false);
                            if (call.isMuted()) {
                                Log.d(TAG, "Muted");
                                call.toggleMute();
                            }
                            Toast.makeText(getApplicationContext(), "onCallEstablished", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onCallEnded(SipAudioCall call) {
                            stopForegroundService();
                            Intent intent = new Intent(context, ServiceActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intent.putExtra("isRegistered", true);
                            intent.putExtra("isOnCall", false);
                            startActivity(intent);
                            Toast.makeText(getApplicationContext(), "onCallEnded", Toast.LENGTH_LONG).show();
                        }


                    };

                    try {
                        call = sipManager.makeAudioCall(sipProfile.getUriString(), receiver + "@" + serverdomain, listener, 30);
                        call.setListener(listener, true);
                    } catch (SipException e) {
                        e.printStackTrace();
                    }
                } else {
                    registerSIP();
                    makeCall();
                }
            } catch (SipException e) {
                e.printStackTrace();
            }
        }
    }

    private void endCall() {
        Log.d(TAG, "end call logger 1");
        if (call != null) {
            try {
                call.endCall();

            } catch (SipException se) {
                Toast.makeText(getApplicationContext(), "onCallEnded", Toast.LENGTH_LONG).show();
            }
            call.close();
        }
    }

    public void closeLocalProfile() {
        if (sipManager == null) {
            return;
        }
        try {
            if (sipProfile != null) {
                sipManager.unregister(sipProfile, listener);
                sipManager.close(sipProfile.getUriString());

            }
        } catch (Exception ee) {

        }
        try {
            if (sipManager.isRegistered(sipProfile.getUriString())) {
                Toast.makeText(context, "Failed to close local profile", Toast.LENGTH_SHORT);
            } else {
                Toast.makeText(context, "Profile unregistered", Toast.LENGTH_SHORT);
            }
        } catch (SipException e) {
            e.printStackTrace();
        }
    }

}
