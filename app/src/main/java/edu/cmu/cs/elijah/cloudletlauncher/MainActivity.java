package edu.cmu.cs.elijah.cloudletlauncher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import org.w3c.dom.Text;

import de.blinkt.openvpn.api.IOpenVPNAPIService;
import edu.cmu.cs.elijah.cloudletlauncher.api.ICloudletService;
import edu.cmu.cs.elijah.cloudletlauncher.api.ICloudletServiceCallback;

public class MainActivity extends Activity {

    private static final String LOG_TAG = "MainActivity";

    // Application ID
    private final String appId = "test";

    // Message types
    private static final int MSG_STATUS = 0;

    // Consts
    private static final int CONST_OPENVPN_PERMISSION = 0;
    private static final int CONST_OPENVPN_PERMISSION2 = 1;

    // Views
    TextView textStatus;
    Button buttonFindCloudlet;
    Button buttonDisconnectCloudlet;
    Button buttonStartVpn, buttonEndVpn;
    CheckBox checkBoxProfile;
    TextView textNotify;

    // Service for cloudlet functionalities
    private ICloudletService mCloudletService = null;
    private boolean isCloudletServiceConnected = false;
    private String profileUuid;

    // Service for OpenVPN client control; used only for registering app
    private IOpenVPNAPIService mVpnService = null;

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
        buttonDisconnectCloudlet = (Button) findViewById(R.id.button_disconnect_cloudlet);
        buttonStartVpn = (Button) findViewById(R.id.button_start_vpn);
        buttonEndVpn = (Button) findViewById(R.id.button_end_vpn);
        checkBoxProfile = (CheckBox) findViewById(R.id.checkbox_profile);
        textNotify = (TextView) findViewById(R.id.text_notify);

        checkBoxProfile.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateProfileUuid();
                try {
                    mCloudletService.useTestProfile(isChecked);
                } catch (RemoteException e) {}
            }
        });
    }

    @Override
    protected void onResume() {
        Log.v(LOG_TAG, "++onResume");
        super.onResume();

        // Check if OpenVPN for Android has been installed
        PackageManager pm = getPackageManager();
        boolean isInstalled = isPackageInstalled("de.blinkt.openvpn", pm);
        if (!isInstalled) {
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=de.blinkt.openvpn"));
            startActivity(marketIntent);
        } else {
            // Bind to the OpenVPN service to make sure CloudletLauncher has been registered
            // After this is done, connection to CloudletLauncher service will be established
            Intent intentVpnService = new Intent(IOpenVPNAPIService.class.getName());
            Log.i(LOG_TAG, IOpenVPNAPIService.class.getName());
            intentVpnService.setPackage("de.blinkt.openvpn");
            bindService(intentVpnService, mVpnConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onPause() {
        Log.v(LOG_TAG, "++onPause");
        super.onDestroy();
    }

    @Override
    protected void onDestroy() {
        if (mCloudletService != null) {
            try {
                mCloudletService.endOpenVpn();
                mCloudletService.unregisterCallback(mCallback);
            } catch (RemoteException e) {}
        }
        unbindService(mCloudletConnection);
        isCloudletServiceConnected = false;

        Log.v(LOG_TAG, "++onDestroy");
        super.onDestroy();
    }


    /***** Begin handling button events ***********************************************************/

    // Called when the "start_openvpn" button is clicked
    public void startOpenVpn(View view) {
        if (mCloudletService != null) {
            try {
                mCloudletService.startOpenVpn();
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Error in getting cloudlet IP");
            }
        }
    }

    // Called when the "end_openvpn" button is clicked
    public void endOpenVpn(View view) {
        if (mCloudletService != null) {
            try {
                mCloudletService.endOpenVpn();
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Error in getting cloudlet IP");
            }
        }
    }

    // Called when the "find_cloudlet" button is clicked
    public void findCloudlet(View view) {
        if (mCloudletService != null) {
            try {
                mCloudletService.findCloudlet(appId);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Error in getting cloudlet IP");
            }
        }
    }

    // Called when the "disconnect_from_cloudlet" button is clicked
    public void disconnectCloudlet(View view) {
        if (mCloudletService != null) {
            try {
                mCloudletService.disconnectCloudlet(appId);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Error in disconnecting VPN service");
            }
        }
    }
    /***** End handling button events *************************************************************/

    /***** Begin handling connection to cloudlet service ******************************************/
    private ICloudletServiceCallback mCallback = new ICloudletServiceCallback.Stub() {
        public void message(String message) throws RemoteException {
            Message msg = Message.obtain();
            msg.what = MSG_STATUS;
            msg.obj = message;
            mHandler.sendMessage(msg);
        }
        public void newServerIP(String IP_addr) throws RemoteException {
            Message msg = Message.obtain();
            msg.what = MSG_STATUS;
            msg.obj = "New server IP:" + IP_addr;
            mHandler.sendMessage(msg);
        }
    };

    private ServiceConnection mCloudletConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been established
            Log.i(LOG_TAG, "Connection to cloudlet service established");
            mCloudletService = ICloudletService.Stub.asInterface(service);
            try {
                mCloudletService.registerCallback(mCallback);

                updateProfileUuid();
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Error in registering callback to cloudlet service");
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mCloudletService = null;
        }
    };
    /***** End handling connection to cloudlet service ********************************************/

    /***** Begin helper functions *****************************************************************/
    private boolean isPackageInstalled(String packagename, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packagename, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void updateProfileUuid() {
        try {
            profileUuid = mCloudletService.getVpnProfileUuid();
            if (!checkBoxProfile.isChecked() && profileUuid == null) {
                textNotify.setText("Please create a profile named \"cloudlet\" in OpenVPN first");
                textNotify.setVisibility(View.VISIBLE);

                buttonFindCloudlet.setEnabled(false);
                buttonDisconnectCloudlet.setEnabled(false);
                buttonStartVpn.setEnabled(false);
                buttonEndVpn.setEnabled(false);
            } else {
                textNotify.setVisibility(View.INVISIBLE);

                buttonFindCloudlet.setEnabled(true);
                buttonDisconnectCloudlet.setEnabled(true);
                buttonStartVpn.setEnabled(true);
                buttonEndVpn.setEnabled(true);
            }
        } catch (RemoteException e) {}
    }
    /***** End helper functions *******************************************************************/

    private ServiceConnection mVpnConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been established
            mVpnService = IOpenVPNAPIService.Stub.asInterface(service);

            // Request permission to use the API
            // Don't know the difference between prepare() and prepareVPNService()
            try {
                Intent i = mVpnService.prepare(getApplicationContext().getPackageName());
                if (i != null) {
                    startActivityForResult(i, CONST_OPENVPN_PERMISSION);
                } else {
                    onActivityResult(CONST_OPENVPN_PERMISSION, Activity.RESULT_OK, null);
                }
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Error in VPN service call 'prepare'");
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mVpnService = null;
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CONST_OPENVPN_PERMISSION) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    Intent i = mVpnService.prepareVPNService();
                    if (i != null) {
                        startActivityForResult(i, CONST_OPENVPN_PERMISSION2);
                    } else {
                        onActivityResult(CONST_OPENVPN_PERMISSION2, Activity.RESULT_OK, null);
                    }
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "Error in VPN service call 'prepareVPNService'");
                }

//                Message msg = Message.obtain();
//                msg.what = MSG_STATUS;
//                msg.obj = "Cloudlet Launcher successfully gained control over OpenVPN client";
//                mHandler.sendMessage(msg);
            } else {
                Log.w(LOG_TAG, "The user hasn't permitted this app to control OpenVPN client");

                Message msg = Message.obtain();
                msg.what = MSG_STATUS;
                msg.obj = "Cloudlet Launcher failed to gain control over OpenVPN client";
                mHandler.sendMessage(msg);
            }
        }
        if (requestCode == CONST_OPENVPN_PERMISSION2) {
            if (resultCode == Activity.RESULT_OK) {
                // Bind to the Cloudlet service
                if (!isCloudletServiceConnected) {
                    Intent intentCloudletService = new Intent(ICloudletService.class.getName());
                    intentCloudletService.setPackage("edu.cmu.cs.elijah.cloudletlauncher");
                    bindService(intentCloudletService, mCloudletConnection, Context.BIND_AUTO_CREATE);
                    isCloudletServiceConnected = true;
                } else {
                    updateProfileUuid();
                }

                unbindService(mVpnConnection);
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
