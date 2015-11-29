package cdmst.beaconscanner.gcm;



import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

/**
 * Created by wowan on 2015-07-10
 */
public class MyGcmListenerService extends GcmListenerService {
    static final String TAG = "MyGcmListenerService";
    @Override
    public void onMessageReceived(String from, Bundle data) {
        String title = data.getString("title");
        String text = data.getString("text");

        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Title: " + title);
        Log.d(TAG, "Text: " + text);

        //from is sender id
        //data is datas
    }
}