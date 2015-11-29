package cdmst.beaconscanner.gcm;

/**
 * Created by waps12b on 2015. 11. 30..
 */
import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * Created by wowan on 2015-07-10
 */
public class MyInstanceIDListenerService extends InstanceIDListenerService {
    @Override
    public void onTokenRefresh() {
        //send new registration token to app server
    }
}