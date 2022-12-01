package com.example.wheelboardapplication;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class IdraActivity extends AppCompatActivity {
    //variable for requestPermission
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";


    Intent intent = new Intent();
    //catch the device from the intent filter in androidManifest
    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

    //textView
    TextView tv_deviceUsb;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idra);

        tv_deviceUsb = findViewById(R.id.usbDevice);

        if(device != null){
            tv_deviceUsb.setText(device.getDeviceName());
        }else{
            tv_deviceUsb.setText("No device result");
        }

    }
}