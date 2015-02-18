package com.derekwhatley.bluefruitdatalogger;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.os.AsyncTask;

import com.derekwhatley.bluefruitcontroller.R;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


public class LoggerControl extends Activity {
    private TextView mTxtMAC;
    private ToggleButton mBluetoothToggle;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mBluetoothSocket;

    //callbacks
    final int REQUEST_ENABLE_BT = 1;

    //constants
    final String DEVICE_IDENTIFIER = "022f";


    final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    private class StartSerialOverBT extends AsyncTask<BluetoothAdapter, Void, Boolean> {
        protected void onPreExecute() {
            mBluetoothToggle.setEnabled(false); //disable the toggle while task running
        }

        protected Boolean doInBackground(BluetoothAdapter... params) {
            Set<BluetoothDevice> pairedDevices = params[0].getBondedDevices();
            return getSerialSocket(pairedDevices);
        }

        protected void onPostExecute(Boolean result) {
            mBluetoothToggle.setEnabled(true); //re-enable the toggle when finished
            if(result == true) {
                mTxtMAC.setText(getMACAddress());
                mBluetoothToggle.setChecked(true);
            }
            else {
                Toast.makeText(getBaseContext(), "Connection failed", Toast.LENGTH_SHORT).show();
                mBluetoothToggle.setChecked(false);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.datalog_control);

        mTxtMAC = (TextView) findViewById(R.id.txtMAC);
        mBluetoothToggle = (ToggleButton) findViewById(R.id.btnBluetoothToggle);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        if (mBluetoothSocket != null) {
            if (mBluetoothAdapter.isEnabled()) {
                StartSerialOverBT SerialStarter = new StartSerialOverBT();
                SerialStarter.execute(mBluetoothAdapter);
            }
        }

        mBluetoothToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

                if (mBluetoothToggle.isChecked()) {
                    if (!mBluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    } else {
                        //adapter is enabled, Serial Connection is not.
                        Toast.makeText(getBaseContext(), "Connecting", Toast.LENGTH_SHORT).show();
                        StartSerialOverBT SerialStarter = new StartSerialOverBT();
                        SerialStarter.execute(mBluetoothAdapter);
                    }
                } else {
                    //user wants to turn off connection
                    try {
                        mBluetoothSocket.close();
                        mTxtMAC.setText("n/a");
                    } catch (IOException e) {
                        //not much to be done
                        Toast.makeText(getBaseContext(), "Failed to close connection.", Toast.LENGTH_SHORT).show();
                        //don't change the button state if we failed to connect
                        mBluetoothToggle.setChecked(true);
                    }
                    mBluetoothToggle.setEnabled(true);
                }
            }
        });
    }

    private void sendBTData(byte[] data) {

        if(mBluetoothSocket != null) {
            if(mBluetoothSocket.isConnected()) {
                try {
                    OutputStream btOut = mBluetoothSocket.getOutputStream();
                    btOut.write(data);
                }
                catch (IOException e) {
                    //not much to do here
                    System.out.println("failed to send data :: " + e.getMessage());
                }
            }
        }
    }

    private String getMACAddress() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if(device.getName().contains(DEVICE_IDENTIFIER)) {
                    return device.getAddress();
                }
            }
        }
        return "";
    }

    private boolean getSerialSocket(Set<BluetoothDevice> pairedDevices) {
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String name = device.getName();
                if(device.getName().contains(DEVICE_IDENTIFIER)) {
                    try {
                        mBluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID));
                    }
                    catch (IOException e) {
                        //no handling for now.
                        return false;
                    }
                    try {
                        mBluetoothSocket.connect();
                        return true;
                    }
                    catch (IOException e) {
                        //no handling for now
                        return false;
                    }
                }
            }
        }
        return false;
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == RESULT_OK){
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                StartSerialOverBT SerialStarter = new StartSerialOverBT();
                SerialStarter.execute(mBluetoothAdapter);
                //Toast.makeText(getBaseContext(), "Bluetooth enabled", Toast.LENGTH_SHORT).show();
                //mBluetoothToggle.setEnabled(true);
            }
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getBaseContext(), "Bluetooth enable failed", Toast.LENGTH_SHORT).show();
                mBluetoothToggle.setEnabled(true);
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.datalog_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
