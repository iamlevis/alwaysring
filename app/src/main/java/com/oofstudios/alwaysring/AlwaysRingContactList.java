package com.oofstudios.alwaysring;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class AlwaysRingContactList extends SQLiteOpenHelper {

    public static final String TAG="AR_CONTACTLIST";
    public static final String AR_DB_NAME="alwaysring.db";
    public static final String AR_CONTACTS_TABLE="AR_CONTACTS";
    public static final int DATABASE_VERSION=2;
    
    public AlwaysRingContactList(Context context){
        super(context,AR_DB_NAME,null,DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String q_create =
                "create table "+ AR_CONTACTS_TABLE +" ("
               +"  _id integer primary key autoincrement"
               +" ,normalized_number varchar(255)"
               +" ,striprev_number varchar(255)"
               +" ,display_number varchar(255)"
               +" ,display_name varchar(255)"
               +" ,photo_thumbnail_uri varchar(255)"
               +" ,always_ring_ind integer"
               +" ,varch01 varchar(255)"
               +" ,varch02 varchar(255)"
               +" ,varch03 varchar(255)"
               +" ,int01 integer"
               +" ,int02 integer"
               +" ,int03 integer"
               +");"
              ;
        db.execSQL(q_create);
        
        String q_create2 =
                "create table AR_GENERAL ("
               +"  varch01 varchar(255)"
               +" ,varch02 varchar(255)"
               +" ,varch03 varchar(255)"
               +" ,int01 integer"
               +" ,int02 integer"
               +" ,int03 integer"
               +");"
              ;
        db.execSQL(q_create2);
        
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG,"Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + AR_CONTACTS_TABLE);
            db.execSQL("DROP TABLE IF EXISTS ar_general");
            onCreate(db);
        ;
    }

}
