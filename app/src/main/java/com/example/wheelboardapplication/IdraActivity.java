package com.example.wheelboardapplication;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class IdraActivity extends AppCompatActivity {
    //variable for requestPermission
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    //var
    DataReceiver dataReceiver; //class for parse the dataIn
    UsbDevice device;


    Intent intent = new Intent(ACTION_USB_PERMISSION);


    //textView
    TextView tv_deviceUsb;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idra);
        tv_deviceUsb = findViewById(R.id.usbDevice);

        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        //hash map if are more than one device
        HashMap<String, UsbDevice> deviceMap = usbManager.getDeviceList();
        //check if device are available
        if(deviceMap.size() == 1){//!isEmpty
            String firstKey = deviceMap.keySet().iterator().next();
            device = deviceMap.get(firstKey);
            tv_deviceUsb.setText("Device found");
        }else{
            tv_deviceUsb.setText("Device not found");
            finish();
        }

        //call method permission if necessary
        PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        IntentFilter intentFilter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, intentFilter);

        usbManager.requestPermission(device, permissionIntent);

    }


    //function for the permission of user
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(ACTION_USB_PERMISSION.equals(action)){
                synchronized (this){
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)){
                        if(device != null){
                            //method for initialize device comunication
                            initSerial();
                        }
                    }else {
                        finish();
                    }
                }
            }
        }
    };

    //function for initialize the serial device
    private void initSerial() {
        //find all device connected
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        ProbeTable customTable = new ProbeTable();
        customTable.addProduct(0x0483, 0x374B, CdcAcmSerialDriver.class);
        //find all drivers for the usb device
        UsbSerialProber prober = new UsbSerialProber(customTable);
        List<UsbSerialDriver> availableDrivers = prober.findAllDrivers(usbManager);

        if(availableDrivers.isEmpty()){
            Toast.makeText(getApplicationContext(), "Driver not foung", Toast.LENGTH_SHORT).show();
            return;
        }
        //use first driver available
        UsbSerialDriver usbDriver = availableDrivers.get(0);
        UsbSerialPort port = usbDriver.getPorts().get(0);
        //take usbConnection
        UsbDeviceConnection usbDeviceConnection = usbManager.openDevice(usbDriver.getDevice());

        dataReceiver = new DataReceiver(port, usbDeviceConnection);

        Thread readUsbThread = new Thread(this.serialReceiverFunction);
    }

    //operation of read with another Thread
    Runnable serialReceiverFunction = new Runnable() {
        final int BUFF_SIZE = 150;
        @SuppressLint("SetTextI18n")

        @Override
        public void run() {
            int msgLen, lastBufIndex = 0;
            byte[] dataBuf = new byte[BUFF_SIZE];

            try{
                byte[] data;
                while (true) {
                    data = new byte[50];
                    //wait message
                    while ((msgLen = dataReceiver.port.read(data, 100)) == 0) ;
                    //Push message to buffer
                    for(int i = 0; i <msgLen; i++)
                        dataBuf[i + lastBufIndex] = data[i];

                    lastBufIndex += msgLen;
                    //check buffer full
                    if(lastBufIndex > (BUFF_SIZE - 50)){
                        //Run on separete thred(slow)
                        dataReceiver.startEmptyBuffer(dataBuf, lastBufIndex);

                        //clean buffer
                        dataBuf = new byte[BUFF_SIZE];
                        lastBufIndex = 0;
                    }
                    //call the thread of ui to update value

                    runOnUiThread(() -> {
                        //set the field of ui to update
                    });
                }
            }catch (IOException ioE){
                finish();
            }
        }
    };

}