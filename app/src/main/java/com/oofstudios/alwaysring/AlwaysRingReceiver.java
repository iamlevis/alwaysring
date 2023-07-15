package com.oofstudios.alwaysring;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;

public class AlwaysRingReceiver extends BroadcastReceiver {
    private static final String TAG = "AR_RECEIVER";
    static boolean WE_TOUCHED_RINGER_MODE;
    static Integer ORIG_RINGER_MODE;
    static Integer ORIG_RINGER_VOLUME;
    
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        Log.i(TAG,"Phone activity: "+intent.getStringExtra(TelephonyManager.EXTRA_STATE));
        
        if(!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("ar_enabled", false)){
            Log.i(TAG,"AR disabled.");
            return;
        }
        
        String phone_state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        String phone_nbr = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

        // If we changed the ring mode it's because we got on a call.  So when
        // we hang up that call, and we had messed with the mode, then set the
        // mode back to whatever we had changed it from.
        if (WE_TOUCHED_RINGER_MODE && phone_state.equals(TelephonyManager.EXTRA_STATE_IDLE)){
            Log.i(TAG,"Hung up.  Setting mode/volume back to "+ORIG_RINGER_MODE+"/"+ORIG_RINGER_VOLUME);
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            am.setRingerMode(ORIG_RINGER_MODE);
            am.setStreamVolume(AudioManager.STREAM_RING,ORIG_RINGER_VOLUME,0);
            WE_TOUCHED_RINGER_MODE=false;
            return;
        }
        
        if (phone_state.equals(TelephonyManager.EXTRA_STATE_RINGING)){
            Log.i(TAG,"Incoming call from "+phone_nbr);
            
            /* Check ringer mode.  If the ringer is already loud, return. */
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if((   am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL)
                && am.getStreamVolume(AudioManager.STREAM_RING) == am.getStreamMaxVolume(AudioManager.STREAM_RING)
              ){
                Log.i(TAG,"Ringer is already on and at max volume.  Leave.");
                return;
            }
            
            AlwaysRingContactList arc = new AlwaysRingContactList(context);
            SQLiteDatabase arcdb = arc.getReadableDatabase();
            
            /* Is the calling number in our always-ring contacts? */
            long car_count = 0;
            Cursor car = arcdb.query("AR_CONTACTS",
                    new String[] {"_id"},
                    "striprev_number like '"+PhoneNumberUtils.toCallerIDMinMatch(phone_nbr)+"%'",
                    null, null, null, null);
            car_count = car.getCount();
            Log.i(TAG,"CAR_COUNT is "+car_count);
            car.close();
            arcdb.close();
            
            if(0==car_count){
                Log.i(TAG,"Phone number "+phone_nbr+" isn't in the list.  Do nothing.");
                return;
            } else {
                Log.i(TAG,"IMPORTANT NUMBER!  GET LOUD!");
     
                ORIG_RINGER_MODE = am.getRingerMode();
                ORIG_RINGER_VOLUME = am.getStreamVolume(AudioManager.STREAM_RING);
                WE_TOUCHED_RINGER_MODE = true;
                
                Log.i(TAG,"Current ring mode/volume is "+am.getRingerMode()+"/"
                            +am.getStreamVolume(AudioManager.STREAM_RING));
                am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                am.setStreamVolume(AudioManager.STREAM_RING,
                                   am.getStreamMaxVolume(AudioManager.STREAM_RING),
                                   0);
                Log.i(TAG,"New ring mode/volume is "+am.getRingerMode()+"/"
                        +am.getStreamVolume(AudioManager.STREAM_RING));
            }
        }
        

    } //onReceive
}
