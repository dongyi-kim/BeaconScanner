package cdmst.beaconscanner;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.pusher.client.Pusher;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.SubscriptionEventListener;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.altbeacon.beacon.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class ActMain extends AppCompatActivity implements BeaconConsumer {
    private Pusher pusher;
    public Channel channel;

    private BeaconManager bmManager;
    private String[] sBeaconId = new String[3];
    private List<Beacon> listBeacon = new ArrayList<>();

    private String sBeaconDescription[] = {"Starting Point", "Middle Point", "Ending Point"};
    private int iPastBeaconIndex[] = {-1, -1, -1};
    private double lfMaxDistance = -1;
    private double lfMinDistance = -1;
    private boolean bAlarmArraival = false;

    private int cnt = 0;

    TextView txtBeacon, test;
    Button btnRun;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_main);

        test = (TextView)findViewById(R.id.txt_alram);
        txtBeacon = (TextView)findViewById(R.id.txt_beacon);
        btnRun = (Button)findViewById(R.id.btn_run);
        btnRun.setOnClickListener(listenDetectBeacons);


        pusher = new Pusher("2b689e6f0d54d07da5bd");

        channel = pusher.subscribe("test_channel");

        channel.bind("my_event", new SubscriptionEventListener() {
            @Override
            public void onEvent(String channelName, String eventName, final String data) {
                System.out.println(data);
            }
        });

        pusher.connect();


        sBeaconId[0] = "74278bda-b644-4520-8f0c-720eaf059935";
        sBeaconId[1] = "5a4bcfce-174e-4bac-a814-092e77f6b7e5";
        sBeaconId[2] = "e2c56db5-dffb-48d2-b060-d0f5a71096e0";

        bmManager = BeaconManager.getInstanceForApplication(this);

        bmManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        bmManager.bind(this); //start detecting

        regist();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        bmManager.unbind(this);
    }

    Button.OnClickListener listenDetectBeacons = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            handler.sendEmptyMessage(0);
        }
    };

    @Override
    public void onBeaconServiceConnect() {
        bmManager.setRangeNotifier(new RangeNotifier() {
            @Override
            // call this method when beacon detected
            // Collection<Beacon> beacons : 감지된 비콘 리스트
            // region: 비콘에 대응하는 Region 객체
            public void didRangeBeaconsInRegion(Collection<Beacon> collectionBeacon, Region region) {
                if (!collectionBeacon.isEmpty()) {
                    listBeacon.clear();
                    for (Beacon beacon : collectionBeacon)
                        listBeacon.add(beacon);
                }
            }
        });

        try{
            bmManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e){}
    }

    Handler handler = new Handler(){
        public void handleMessage(Message msg){
            int iNearIndex = -1; double lfShortDist = -1;

            for(Beacon beacon : listBeacon){
                for(int i = 0; i < 3; i++){
                    if(beacon.getId1().toString().compareTo(sBeaconId[i]) == 0){
                        if(lfShortDist < 0 || beacon.getDistance() < lfShortDist){
                            lfShortDist = beacon.getDistance();
                            iNearIndex = i;
                        }
                    }
                }
            }


            if(iNearIndex < 0 || lfShortDist > 3){
                txtBeacon.setText("No Points");
                lfMaxDistance = lfMinDistance = -1;
                bAlarmArraival = false;
            }
            else{
                if(iNearIndex != iPastBeaconIndex[0]){
                    for(int i = 0; i < 2; i++)
                        iPastBeaconIndex[i + 1] = iPastBeaconIndex[i];

                    bAlarmArraival = false;
                    iPastBeaconIndex[0] = iNearIndex;
                    lfMinDistance = lfMaxDistance = lfShortDist;
                }

                txtBeacon.setText(sBeaconDescription[iNearIndex] + " " + Double.parseDouble(String.format("%.3f", lfShortDist)) + "m");

                lfMaxDistance = Math.max(lfMaxDistance, lfShortDist);
                lfMinDistance = Math.min(lfMinDistance, lfShortDist);

                if(!bAlarmArraival && lfMaxDistance - lfMinDistance > 0.1){
                    bAlarmArraival = true;

                    if(iPastBeaconIndex[0] == 2 && iPastBeaconIndex[1] == 1 && iPastBeaconIndex[0] == 2) sendArrivalToUser();
                    else sendAreaPush(iNearIndex);
                }
            }

            handler.sendEmptyMessageDelayed(0, 500);
        }
    };

    private void sendAreaPush(int idx){
        test.setText("" + cnt++ + " in " + sBeaconDescription[idx]);
    }

    private void sendArrivalToUser(){
        test.setText("" + cnt++ + "arrival!");
    }

    public static String rid;
    public static String senderId = "236428530901";
    public static String regId;

    void regist(){
        final String TAG = "regist";
        new AsyncTask<Void,Void,String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";

                Log.d(TAG, msg);
                try {
                    InstanceID instanceID = InstanceID.getInstance(getApplicationContext());
                    rid = instanceID.getToken(senderId, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                    //instanceID.deleteToken(rid,GoogleCloudMessaging.INSTANCE_ID_SCOPE);
                    msg = "Device registered, registration ID=" + rid;
                    sendRequest("http://kd2kr.iptime.org/regist?token=" + rid);

                    //발급 받은 토큰을 Sharedpreference로 저장 (미구현)

                    //storeRegistrationId(context, regId);

                    Log.d(TAG, msg);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                }
                return msg;
            }
        }.execute(null, null, null);
    }

    void unregist(){
        final String TAG = "unregist";
        new AsyncTask<Void,Void,String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";

                Log.d(TAG, msg);
                try {
                    InstanceID instanceID = InstanceID.getInstance(getApplicationContext());
                    instanceID.deleteInstanceID();
                    msg = "Device unRegistered";

                    //server에 토큰 삭제 요청 (미구현)
                    //unRegistToServer();

                    //Sharedpreference로 저장된 토큰 제거 (미구현)
                    //deleteRegistrationId(context, regId);

                    Log.d(TAG, msg);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                }
                return msg;
            }
        }.execute(null, null, null);
    }

    void getPush(int deviceNumber){
        sendRequest("http://kd2kr.iptime.org/send?device_number=" + deviceNumber);
    }

    void sendRequest(String url){
        try
        {
            HttpClient client = new DefaultHttpClient();
            HttpGet get = new HttpGet(url);
            HttpResponse responseGet = client.execute(get);
            HttpEntity resEntityGet = responseGet.getEntity();
            if (resEntityGet != null) {
                // 결과를 처리합니다.
                Log.i("RESPONSE", EntityUtils.toString(resEntityGet));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

}
