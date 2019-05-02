package com.example.dlibtest.ARcamera;


import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;

import org.litepal.LitePalApplication;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class MyApplication extends LitePalApplication {

    private static Context mContext;
    private static AssetManager assetManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        assetManager=getAssets();

        Realm.init(this);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(config);
    }

    public static Context getAppContext() {
        return mContext;
    }
    public static AssetManager getAPPAssets(){ return assetManager;}
}
