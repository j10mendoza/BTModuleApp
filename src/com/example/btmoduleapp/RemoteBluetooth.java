package com.example.btmoduleapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("HandlerLeak")
public class RemoteBluetooth extends Activity {
	
	// Layout view
	//private TextView mTitle;
	
	// Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    
    // Key names received from the BluetoothCommandService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
	
	// Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for Bluetooth Command Service
    private BluetoothCommandService mCommandService = null;
	
    /** Called when the activity is first created. */
    /**
     * onCreate the device is queried for bluetooth adapters.
     * If it doesn't have any it will return.
     * Otherwise it will continue. 
     * 
     * onStart will then be called next.
     * 	**/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    	System.out.println("In onCreate");
        
        // Set up the window layout
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_remote_bluetooth);
        getWindow().setFeatureInt(Window.FEATURE_NO_TITLE, R.layout.custom_title);
        
        // Set up the custom title
        //mTitle = (TextView) findViewById(R.id.title_left_text);
        //mTitle.setText(R.string.app_name);
        //mTitle = (TextView) findViewById(R.id.title_right_text);
        
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
    	System.out.println("Out onCreate");

    }

    /**
     * 
     * onStart() the user will be asked to enable bluetooth if
     * it has not already been turn on. 
     * 
     * startActivityForResult will allow to app to continue if 
     * the user selects to turn on the bluetooth. Otherwise,
     * an exception will be thrown. 
     * 
     * setupCommand() initializes mCommandService to set up bluetooth
     * connection.
     * **/
	@Override
	protected void onStart() {
		super.onStart();
		
    	System.out.println("In onStart");

		
		// If BT is not on, request that it be enabled.
        // setupCommand() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}
		// otherwise set up the command service
		else {
			if (mCommandService==null)
				setupCommand();
		}
		
    	System.out.println("Out onStart");

	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
		if (mCommandService != null) {
			if (mCommandService.getState() == BluetoothCommandService.STATE_NONE) {
				mCommandService.start();
			}
		}
	}

	private void setupCommand() {
		// Initialize the BluetoothChatService to perform bluetooth connections
        mCommandService = new BluetoothCommandService(this, mHandler);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (mCommandService != null)
			mCommandService.stop();
	}
	
	private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
	
	// The Handler that gets information back from the BluetoothCommandService
	/**
	 * 
	 * The handler "handles" message between the BluetoothCommandService
	 * and the RemoteBluetooth objects.
	 * 
	 * The BluetoothCommandService executes two threads in order to get
	 * bluetooth comm up and running from a "Not Connected" to "Connected".
	 * The ConnectThread handles establishing the connection while the 
	 * ConnectedThread handles the data transfer via bluetooth.
	 * 
	 * The handler transitions to different connected states and toasts
	 * to the user when it has connected to another device or has lost 
	 * the connection. 
	 * 
	 * **/
	private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	
        	System.out.println("In handler");
            
        	switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothCommandService.STATE_CONNECTED:
                    //mTitle.setText(R.string.title_connected_to);
                    //mTitle.append(mConnectedDeviceName);
                	System.out.println("State Connected");

                    break;
                case BluetoothCommandService.STATE_CONNECTING:
                    //mTitle.setText(R.string.title_connecting);
                	System.out.println("State Connecting");

                    break;
                case BluetoothCommandService.STATE_LISTEN:
                	//TODO: will be implemented for client-server comm
                case BluetoothCommandService.STATE_NONE:
                    //mTitle.setText(R.string.title_not_connected);
                	System.out.println("State None");

                    break;
                }
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    
    /**
     * 
     * This activity method is used to exit if user has chosen not to enable
     * the bluetooth adapter or to get the MAC address from the remote
     * device to connect to the device. 
     * 
     * **/
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mCommandService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupCommand();
            } else {
                // User did not enable Bluetooth or an error occured
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
            // Launch the DeviceListActivity to see devices and do scan
        	Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        }
        return false;
    }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			mCommandService.write(BluetoothCommandService.VOL_UP);
			return true;
		}
		else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
			mCommandService.write(BluetoothCommandService.VOL_DOWN);
			return true;
		}
		
		return super.onKeyDown(keyCode, event);
	}
}