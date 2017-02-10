package edu.cmu.cs.elijah.cloudletlauncher;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;


import javax.net.ssl.HttpsURLConnection;

import de.blinkt.openvpn.api.IOpenVPNAPIService;
import de.blinkt.openvpn.api.IOpenVPNStatusCallback;
import edu.cmu.cs.elijah.cloudletlauncher.api.ICloudletService;
import edu.cmu.cs.elijah.cloudletlauncher.api.ICloudletServiceCallback;

public class CloudletService extends Service {
    private static final String LOG_TAG = "CloudletService";

    // User info
    private final String userID = "abc";
    private final String appID = "xyz";

    // Cloudlet info
    private String cloudletIP = "8.225.186.10";
    private int cloudletPort = 9127;
    private String vmIP = "";

    // Message types
    private static final int MSG_POST_DONE = 0;
    private static final int MSG_GET_DONE = 1;

    // Consts
    private static final int CONST_OPENVPN_PERMISSION = 0;

    // OpenVPN connection
    private IOpenVPNAPIService mVPNService = null;

    // Callbacks
    private final RemoteCallbackList<ICloudletServiceCallback> callbackList = new RemoteCallbackList<ICloudletServiceCallback>();

    @Override
    public void onCreate() {
        Log.v(LOG_TAG, "++onCreate");
        super.onCreate();

        // Bind to the OpenVPN service
        Intent intentVPNService = new Intent(IOpenVPNAPIService.class.getName());
        Log.i(LOG_TAG, IOpenVPNAPIService.class.getName());
        intentVPNService.setPackage("de.blinkt.openvpn");

        bindService(intentVPNService, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        if (mVPNService != null) {
            try {
                mVPNService.unregisterStatusCallback(mCallback);
                mVPNService.disconnect();
            } catch (RemoteException e) {}
        }

        unbindService(mConnection);

        Log.v(LOG_TAG, "++onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final ICloudletService.Stub mBinder = new ICloudletService.Stub() {
        public void findCloudlet() {
            new SendPostRequestAsync().execute("http://" + cloudletIP + ":" + cloudletPort, "create");
        };

        public void disconnectCloudlet() {
            if (mVPNService != null) {
                try {
                    mVPNService.disconnect();
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "Error in disconnecting VPN service: " + e.getMessage());
                }
            }
            new SendPostRequestAsync().execute("http://" + cloudletIP + ":" + cloudletPort, "delete");
        };

        public void registerCallback(ICloudletServiceCallback cb) {
            callbackList.register(cb);
            Log.d(LOG_TAG, "Callback registered");
        }

        public void unregisterCallback(ICloudletServiceCallback cb) {
            callbackList.unregister(cb);
            Log.d(LOG_TAG, "Callback unregistered");
        }
    };


    /***** Begin handling http connections ********************************************************/
    private String sendPostRequest(String... paras) {
        try {
            URL url = new URL(paras[0]); // here is your URL path
            String action = paras[1];

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("user_id", userID)
                    .appendQueryParameter("app_id", appID)
                    .appendQueryParameter("action", action);
            String query = builder.build().getEncodedQuery();

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(query);
            writer.flush();
            writer.close();
            os.close();

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String serverResponse = readStream(conn.getInputStream());
                return serverResponse;
            }
        }
        catch(Exception e){
            Log.e(LOG_TAG, "Error in sending POST message: " + e.getMessage());
        }
        return null;
    }
    private class ParaWrapper
    {
        public String response;
        public String action;
    }
    public class SendPostRequestAsync extends AsyncTask<String, Void, ParaWrapper> {
        @Override
        protected ParaWrapper doInBackground(String... paras) {
            ParaWrapper p = new ParaWrapper();
            p.response = sendPostRequest(paras);
            p.action = paras[1];
            return p;
        }

        @Override
        protected void onPostExecute(ParaWrapper p) {
            if (p.response != null) {
                Message msg = Message.obtain();
                msg.what = MSG_POST_DONE;
                msg.obj = p.action;
                mHandler.sendMessage(msg);
            }
        }
    }


    private String sendGetRequest(String... paras) {
        try {
            URL url = new URL(paras[0]);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String serverResponse = readStream(conn.getInputStream());
                return serverResponse;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error in sending GET message: " + e.getMessage());
        }

        return null;
    }

    public class SendGetRequestAsync extends AsyncTask<String , Void ,String> {
        @Override
        protected String doInBackground(String... paras) {
            return sendGetRequest(paras);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Message msg = Message.obtain();
                msg.what = MSG_GET_DONE;
                mHandler.sendMessage(msg);
            }
        }
    }

    // Converting InputStream to String
    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error in reading response from server: " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {}
            }
        }
        return response.toString();
    }

    /***** Begin handling connection to OpenVPN service *******************************************/
    private void connectVPN() {
        // load client configuration file for OpenVPN
        String configStr = "";
        try {
            InputStream conf = getApplicationContext().getAssets().open("test.conf");
            InputStreamReader isr = new InputStreamReader(conf);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while(true) {
                line = br.readLine();
                if(line == null)
                    break;
                configStr += line + "\n";
            }
            br.readLine();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error reading .conf file: " + e.getMessage());
        }

        // Start connection
        if (mVPNService != null) {
            try {
                mVPNService.startVPN(configStr);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Error in starting VPN connection: " + e.getMessage());
            }
        }
    }

    private IOpenVPNStatusCallback mCallback = new IOpenVPNStatusCallback.Stub() {
        @Override
        public void newStatus(String uuid, String state, String message, String level)
                throws RemoteException {
            Log.d(LOG_TAG, state + "|" + message);
            int n = callbackList.beginBroadcast();
            for(int i = 0; i < n; i++)
            {
                try {
                    callbackList.getBroadcastItem(i).message(state + "|" + message);
                    if (state.equals("CONNECTED")) {
                        callbackList.getBroadcastItem(i).newServerIP(vmIP);
                    }
                } catch (RemoteException e) {
                    // RemoteCallbackList will take care of removing dead objects
                }
            }
            callbackList.finishBroadcast();
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been established
            mVPNService = IOpenVPNAPIService.Stub.asInterface(service);
            try {
                mVPNService.registerStatusCallback(mCallback);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Error in registering callback to OpenVPN service: " + e.getMessage());
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mVPNService = null;
        }
    };
    /***** End handling connection to OpenVPN service **********************************************/

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == MSG_POST_DONE) {
                String action = (String) msg.obj;
                if (action.equals("create")) {
                    final Timer pollingTimer = new Timer();
                    TimerTask pollingTask = new TimerTask() {
                        @Override
                        public void run() {
                            String response = sendGetRequest("http://" + cloudletIP + ":" + cloudletPort + "?user_id=" + userID + "&app_id=" + appID);
                            Log.d(LOG_TAG, response);
                            if (response != null && !response.equals("None")) {
                                pollingTimer.cancel();
                                vmIP = response;
                                connectVPN();
                            }
                        }
                    };
                    pollingTimer.schedule(pollingTask, 10000, 3000);
                }
            }
        }
    };
}
