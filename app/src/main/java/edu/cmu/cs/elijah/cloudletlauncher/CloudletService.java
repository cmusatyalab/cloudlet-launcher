package edu.cmu.cs.elijah.cloudletlauncher;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.blinkt.openvpn.api.IOpenVPNAPIService;
import de.blinkt.openvpn.api.IOpenVPNStatusCallback;
import edu.cmu.cs.elijah.cloudletlauncher.api.ICloudletService;
import edu.cmu.cs.elijah.cloudletlauncher.api.ICloudletServiceCallback;

public class CloudletService extends Service {
    private static final String LOG_TAG = "CloudletService";

    private String cloudletIP = "128.2.213.25";

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
        public String findCloudlet() {
            return cloudletIP;
        };

        public void connectVPN() {
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
                Log.e(LOG_TAG, "Error reading .conf file");
            }

            // Start connection
            if (mVPNService != null) {
                try {
                    mVPNService.addNewVPNProfile("test_launcher", true, configStr); // true allows user to edit
                    mVPNService.startVPN(configStr);
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "Error in starting VPN connection");
                }
            }
        };

        public void disconnectVPN() {
            if (mVPNService != null) {
                try {
                    mVPNService.disconnect();
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "Error in disconnecting VPN service");
                }
            }
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


    /***** Begin handling connection to OpenVPN service ***********************/
    private IOpenVPNStatusCallback mCallback = new IOpenVPNStatusCallback.Stub() {
        @Override
        public void newStatus(String uuid, String state, String message, String level)
                throws RemoteException {
            int n = callbackList.beginBroadcast();
            for(int i = 0; i < n; i++)
            {
                try {
                    callbackList.getBroadcastItem(i).message(state + "|" + message);
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
                Log.e(LOG_TAG, "Error in registering callback to OpenVPN service");
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mVPNService = null;
        }
    };
    /***** End handling connection to OpenVPN service *************************/

}
