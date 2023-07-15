package com.oofstudios.alwaysring;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Contacts.Photo;
import android.support.v4.widget.ResourceCursorAdapter;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
//import android.widget.ResourceCursorAdapter;


public class AlwaysRingMainActivity extends Activity {

    static final String TAG = "AR_MAIN";
    static final int PICK_CONTACT_REQUEST = 1;

    
    /* Used to bind data to the layouts (ar_contact_item.xml) in the listview */
    public class ClientCursorAdapter extends ResourceCursorAdapter {
        public ClientCursorAdapter(Context context, int layout, Cursor c, int flags) {
            super(context, layout, c, flags);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView name = (TextView) view.findViewById(R.id.firstLine);
            name.setText(cursor.getString(cursor.getColumnIndex("display_name")));

            TextView phone = (TextView) view.findViewById(R.id.secondLine);
            phone.setText(cursor.getString(cursor.getColumnIndex("display_number")));
            
            // Set icon to contact thumbnail if it exists.
            ImageView contactIcon = (ImageView) view.findViewById(R.id.contact_icon);
            String imguri = cursor.getString(cursor.getColumnIndex("photo_thumbnail_uri"));
            if(null!=imguri){
                contactIcon.setImageURI(Uri.parse(imguri));
                contactIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
            
            ImageView delimg = (ImageView) view.findViewById(R.id.delete_icon);
            delimg.setTag(R.id.TAG_NBR_ID,cursor.getString(cursor.getColumnIndex("_id")));
        }
    }
    

    public SharedPreferences mPreferences;

    private AlwaysRingContactList mARContacts;
    private SQLiteDatabase mARC;
    AudioManager mAM = (AudioManager) null;

    private AlwaysRingNotifier mNotifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG,"In main.");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mNotifier = new AlwaysRingNotifier(this);
        
        setEnabledCheck();

        mARContacts = new AlwaysRingContactList(this);
        mARC = mARContacts.getWritableDatabase(); 

        updateArList();
        
    }

    public void onClickPickContact(View view){
        pickContact();
    }
    
    public void onNumberDelete(View view){
        ImageView v = (ImageView) view;
        String del_id = String.valueOf(v.getTag(R.id.TAG_NBR_ID));
        Log.i(TAG,"DELETING CONTACT #"+del_id);
        mARC.delete(AlwaysRingContactList.AR_CONTACTS_TABLE,
                    "_id=?",
                    new String[]{del_id}
        );
        updateArList();
    }
    
    public void updateArList(){
        // On create, or after pickContact build the list
        // of for whom we're going to always ring.
        //TODO Since you're lazy, you're doing it all here.  Good programmers
        //     would create a data-access shim (like you did for CarbCounter).
        
        ListView lv = (ListView) findViewById(R.id.contactsListView);

        AlwaysRingContactList arc = new AlwaysRingContactList(this);
        SQLiteDatabase arcdb = arc.getReadableDatabase();
        Cursor car = arcdb.query("AR_CONTACTS",
                new String[] {"_id","display_number","display_name","photo_thumbnail_uri"},
                null,null,null,null,"_id desc");
        
        ClientCursorAdapter adapter = new ClientCursorAdapter(
                this, R.layout.ar_contact_item, car, 0 );

        lv.setAdapter(adapter);
        
    }
    
    @SuppressLint("InlinedApi")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (PICK_CONTACT_REQUEST == requestCode){
            // Make sure the request was successful
            if (RESULT_OK == resultCode) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                if (data != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        // Starting with API 11 (Honeycomb), we can store the uri to the
                        // contact icon easily.  Before that, we have to look it up
                        // using the contact's id.
                        String photoField;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            photoField = Phone.PHOTO_THUMBNAIL_URI;
                        } else {
                            photoField = Phone._ID;
                        }
                        
                        Cursor c = null;
                        try {
                            c = getContentResolver().query(uri,
                                    new String[] {Phone.NUMBER,
                                                  Phone.DISPLAY_NAME,
                                                  photoField
                                                 },
                                    null, null, null
                                );
                            if (c != null && c.moveToFirst()) {
                                ContentValues cv = new ContentValues();
                                cv.put("display_number",c.getString(0));
                                cv.put("striprev_number",PhoneNumberUtils.getStrippedReversed(c.getString(0)));
                                cv.put("display_name",c.getString(1));
                                
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                                    cv.put("photo_thumbnail_uri",c.getString(2));
                                } else {
                                    // Prior to Android 3.0, constructs a photo Uri using _ID
                                    /*
                                     * Creates a contact URI from the Contacts content URI
                                     * incoming photoData (_ID)
                                     */
                                    final Uri contactUri = Uri.withAppendedPath(Contacts.CONTENT_URI, c.getString(2));
                                    /*
                                     * Creates a photo URI by appending the content URI of
                                     * Contacts.Photo.
                                     */
                                    Uri thumbUri =  Uri.withAppendedPath(contactUri, Photo.CONTENT_DIRECTORY);
                                    cv.put("photo_thumbnail_uri",thumbUri.toString());
                                }
                                
                                long i = mARC.insert("AR_CONTACTS", null, cv);
                                Log.i(TAG,"Inserted at row "+i+": "+cv.toString());
                                Toast.makeText(AlwaysRingMainActivity.this,
                                        "Added "+cv.get("display_number")+"\n("+cv.get("display_name")+")",
                                        Toast.LENGTH_LONG).show();
                            }
                        } finally {
                            if (c != null) {
                                c.close();
                            }
                        }
                    }
                }              
            }
        }
        updateArList();
    }

    private void pickContact() {
        /* This style gives me the id of the phone number (i.e., 10855)  */
        Intent pickContactIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
        pickContactIntent.setType(Phone.CONTENT_TYPE); // Show user only contacts w/ phone numbers
        startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
    }

    public void setEnabledCheck(){
        CheckBox cb = (CheckBox) findViewById(R.id.checkBoxEnabled);

        boolean enabled = mPreferences.getBoolean("ar_enabled",false);
        if(true==enabled){
            cb.setChecked(true);
        } else {
            cb.setChecked(false);
        }
        
        mNotifier.updateNotification();
    }

    public void onEnabledCheck(View view){
        //Respond to users clicking the checkbox.
        Editor e = mPreferences.edit();
        CheckBox cb = (CheckBox) view;
        if(cb.isChecked()){
            e.putBoolean("ar_enabled", true);
        } else {
            e.putBoolean("ar_enabled", false);
        }
        e.commit();
        mNotifier.updateNotification();
    }
}
