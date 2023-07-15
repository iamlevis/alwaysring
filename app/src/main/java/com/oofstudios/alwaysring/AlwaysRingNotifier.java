package com.oofstudios.alwaysring;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class AlwaysRingNotifier {
    private final static String TAG = "AR_NOTIFICATION";
    private final static int NOTIFICATION_ID = 1;
    
    private Context mContext;
    private NotificationManager mNotificationManager;
    private SharedPreferences mSharedPreferences;
    
    public AlwaysRingNotifier(Context context) {
        Log.i(TAG,"In notifier ctor.");
        mContext = context;
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    public void updateNotification() {
        boolean enabled = mSharedPreferences.getBoolean("ar_enabled",false);
        if(true==enabled){
            this.enableNotification();
        } else {
            this.disableNotification();
        }
    }

    @SuppressWarnings("deprecation")
    public void enableNotification(){
        Log.i(TAG,"In enable notification.");
        Intent i = new Intent(mContext,AlwaysRingMainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(mContext, 0, i, 0);
        
        Notification n = new Notification(R.drawable.ic_jog_dial_sound_on,null,0);
        n.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
        n.setLatestEventInfo(mContext,
                mContext.getString(R.string.notifier_line1),
                mContext.getString(R.string.notifier_line2),
                pi);
        mNotificationManager.notify(NOTIFICATION_ID,n);
    }
    
    public void disableNotification(){
        mNotificationManager.cancel(NOTIFICATION_ID);
    }
    

}
