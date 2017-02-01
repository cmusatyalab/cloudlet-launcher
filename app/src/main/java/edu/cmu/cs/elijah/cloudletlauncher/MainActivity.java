package edu.cmu.cs.elijah.cloudletlauncher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.blinkt.openvpn.api.IOpenVPNAPIService;
import de.blinkt.openvpn.api.IOpenVPNStatusCallback;

public class MainActivity extends Activity {

    private static final String LOG_TAG = "MainActivity";

    private String cloudletIP = "128.2.213.25";

    // Consts
    private static final int CONST_OPENVPN_PERMISSION = 0;

    // Message types
    private static final int MSG_STATUS = 0;

    // Views
    TextView textStatus;
    Button buttonFindCloudlet;
    Button buttonConnectVPN;
    Button buttonDisconnectVPN;

    // OpenVPN connection
    private IOpenVPNAPIService mService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(LOG_TAG, "++onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED+
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON+
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Get views
        textStatus = (TextView) findViewById(R.id.text_status);
        buttonFindCloudlet = (Button) findViewById(R.id.button_find_cloudlet);
        buttonConnectVPN = (Button) findViewById(R.id.button_connect_VPN);
        buttonDisconnectVPN = (Button) findViewById(R.id.button_disconnect_VPN);
    }

    @Override
    protected void onResume() {
        Log.v(LOG_TAG, "++onResume");
        super.onResume();

        // Bind to the OpenVPN service
        Intent intentVPNService = new Intent(IOpenVPNAPIService.class.getName());
        Log.i(LOG_TAG, IOpenVPNAPIService.class.getName());
        intentVPNService.setPackage("de.blinkt.openvpn");

        bindService(intentVPNService, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        Log.v(LOG_TAG, "++onPause");
        super.onDestroy();
    }

    @Override
    protected void onDestroy() {
        Log.v(LOG_TAG, "++onDestroy");
        super.onDestroy();

        unbindService(mConnection);
    }


    /***** Begin handling button events ***************************************/
    // Called when the "find_cloulet" button is clicked
    public void findCloudlet(View view) {
        cloudletIP = "128.2.213.25";
        textStatus.setText("Cloudlet found:" + cloudletIP);
    }

    // Called when the "connect_VPN" button is clicked
    public void connectVPN(View view) {
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
        if (mService != null) {
            try {
                mService.addNewVPNProfile("test_launcher", true, configStr); // true allows user to edit
                mService.startVPN(configStr);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Error in starting VPN connection");
            }
        }
    }

    // Called when the "disconnect_VPN" button is clicked
    public void disconnectVPN(View view) {
        if (mService != null) {
            try {
                mService.disconnect();
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Error in disconnecting VPN service");
            }
        }
    }
    /***** End handling button events *****************************************/


    /***** Begin handling connection to OpenVPN service ***********************/
    private IOpenVPNStatusCallback mCallback = new IOpenVPNStatusCallback.Stub() {
        @Override
        public void newStatus(String uuid, String state, String message, String level)
                throws RemoteException {
            Message msg = Message.obtain();
            msg.what = MSG_STATUS;
            msg.obj = state + "|" + message;
            mHandler.sendMessage(msg);
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been established
            mService = IOpenVPNAPIService.Stub.asInterface(service);
            try {
                // Request permission to use the API
                Intent i = mService.prepare(getApplicationContext().getPackageName());
                if (i != null) {
                    startActivityForResult(i, CONST_OPENVPN_PERMISSION);
                } else {
                    onActivityResult(CONST_OPENVPN_PERMISSION, Activity.RESULT_OK, null);
                }

            } catch (RemoteException e) {}
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
        }
    };
    /***** End handling connection to OpenVPN service *************************/


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if(requestCode == CONST_OPENVPN_PERMISSION) {
                try {
                    mService.registerStatusCallback(mCallback);
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "Error in registering callback to OpenVPN service");
                }
            }
        }
    };

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == MSG_STATUS) {
                textStatus.setText((CharSequence) msg.obj);
            }
        }
    };
}
