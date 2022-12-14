package com.example.wheelboardapplication;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;

public class IdraActivity extends AppCompatActivity {
    //constant for requestPermission
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    //constant for mqttPublish
    private static final String SERVER_URI = "tcp://broker.hivemq.com:1883";
    private static final String SPEED_CHANNEL = "H2polito/Idra/Speed";


    //varUsb
    DataReceiver dataReceiver; //class for parse the dataIn
    UsbDevice device;
    Intent intent = new Intent(ACTION_USB_PERMISSION);

    //varMqtt
    String clientId = MqttClient.generateClientId();
    MqttSender mqttSender;

    //UI var
    TextView tv_deviceUsb;
    TextView tv_speed;
    TextView tv_pressure;
    TextView tv_emergences;
    Button bt_publish;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_idra);

        tv_deviceUsb = findViewById(R.id.usbDevice);
        tv_speed = findViewById(R.id.speed);
        tv_pressure = findViewById(R.id.pressure);
        tv_emergences = findViewById(R.id.emergences);
        bt_publish = findViewById(R.id.publishButton);

        //initSerial communication, and request permission
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        //hash map if are more than one device
        HashMap<String, UsbDevice> deviceMap = usbManager.getDeviceList();
        //check if device are available
        if(deviceMap.size() == 1){//!isEmpty
            String firstKey = deviceMap.keySet().iterator().next();
            device = deviceMap.get(firstKey);
            tv_deviceUsb.setText("Device found");

            //call method permission if necessary
            PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            IntentFilter intentFilter = new IntentFilter(ACTION_USB_PERMISSION);
            registerReceiver(usbReceiver, intentFilter);

            usbManager.requestPermission(device, permissionIntent);
            //initSerial();
        }else{
            tv_deviceUsb.setText("Device not found");
        }

        //connection with client mqtt
        mqttSender = new MqttSender(IdraActivity.this, SERVER_URI, clientId, Ack.AUTO_ACK);

        bt_publish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mqttSender.publishMsg(SPEED_CHANNEL, "25");
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        initSerial();
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
                        }else {
                            finish();
                        }
                    }
                }
            }
        }
    };

    //dataReceiver init Thread for read data from usb
    @SuppressLint("SetTextI18n")
    private void initSerial(){
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        dataReceiver = new DataReceiver(usbManager, IdraActivity.this, DataReceiver.DIRECT_MODE, () -> {
            runOnUiThread(() ->{
                //set ui interface
                tv_speed.setText(Integer.toString(dataReceiver.speedKmH));
                mqttSender.publishMsg(SPEED_CHANNEL, Integer.toString(dataReceiver.speedKmH));
            });
        });

    }

}