package com.example.wheelboardapplication;

import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class IdraActivity extends AppCompatActivity {
    UsbManager usbManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idra);
    }
}