package com.oofstudios.alwaysring;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlwaysRingBootReceiver extends BroadcastReceiver {
    private static final String TAG = "AR_BOOT_REC";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG,"In boot receiver.");
        AlwaysRingNotifier n = new AlwaysRingNotifier(context);
        n.updateNotification();
    }
}
