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

import org.eclipse.paho.client.mqttv3.MqttClient;

import java.util.HashMap;

import info.mqtt.android.service.Ack;

public class IdraActivity extends AppCompatActivity {
    //constant for requestPermission
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    //constant for mqttPublish
    private static final String SERVER_URI = "tcp://broker.hivemq.com:1883";


    private static final String STATUS_CHANNEL = "H2polito/Idra/Status";
    private static final String SHORT_CHANNEL = "H2polito/Idra/Short";
    private static final String POWERMODE_CHANNEL = "H2polito/Idra/PowerMode";
    private static final String PURGE_CHANNEL = "H2polito/Idra/Purge";
    private static final String MOTORON_CHANNEL = "H2polito/Idra/MotorOn";
    private static final String ACTUATIONON_CHANNEL = "H2polito/Idra/ActuationOn";
    private static final String STRATEGY_CHANNEL = "H2polito/Idra/Strategy";
    private static final String SPEED_CHANNEL = "H2polito/Idra/Speed";
    private static final String FUELCELLVOLTS_CHANNEL = "H2polito/Idra/FCVoltage";
    private static final String FUELCELLTEMP_CHANNEL = "H2polito/Idra/Temperature";
    private static final String SUPERCAPVOLTS_CHANNEL = "H2polito/Idra/SCVoltage";
    private static final String CURRENT_CHANNEL = "H2polito/Idra/MotorCurrent";
    private static final String MOTORDUTY_CHANNEL = "H2polito/Idra/MotorDuty";
    private static final String FANDUTY_CHANNEL = "H2polito/Idra/FanDuty";

    //varUsb
    static IdraHandler veichleHandler; //class that contains all veichle specific variables
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
    TextView tv_fccCurrent;
    TextView tv_temperature;
    TextView tv_purge;
    TextView tv_shortTW;
    TextView tv_powerMode;
    TextView tv_actuationOn;

    Button bt_publish;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_idra);

        tv_deviceUsb = findViewById(R.id.usbDevice);
        tv_speed = findViewById(R.id.speed);
        tv_pressure = findViewById(R.id.pressure);
        tv_emergences = findViewById(R.id.emergences);
        tv_fccCurrent = findViewById(R.id.fccCurrent);
        tv_temperature = findViewById(R.id.temperature);
        tv_purge = findViewById(R.id.purge);
        tv_shortTW = findViewById(R.id.shortTW);
        tv_actuationOn = findViewById(R.id.actuationOn);
        tv_powerMode = findViewById(R.id.powerMode);

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
            PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_MUTABLE);
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
                mqttSender.publishMsg(CURRENT_CHANNEL, "25");
            }
        });

        veichleHandler = new IdraHandler();


    }


    @Override
    protected void onStart() {
        super.onStart();
        //initSerial();
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
        dataReceiver = new DataReceiver(usbManager, IdraActivity.this, DataReceiver.NUCLEO_MODE, DataReceiver.IDRA, () -> {
            runOnUiThread(() ->{
                //set ui interface
                tv_speed.setText(String.format("%.2f Km/h", veichleHandler.speedKmH));
                tv_fccCurrent.setText(String.format("%.2f V", veichleHandler.superCapVolt));
                tv_temperature.setText(String.format("%.2f A", veichleHandler.motorAmps));
                tv_pressure.setText(String.format("Strat:%d", veichleHandler.strategy));
                tv_emergences.setText(veichleHandler.getEmergencyString());

                if(veichleHandler.purge)
                    tv_purge.setVisibility(View.VISIBLE);
                else
                    tv_purge.setVisibility(View.INVISIBLE);

                if(veichleHandler.shorts)
                    tv_shortTW.setVisibility(View.VISIBLE);
                else
                    tv_shortTW.setVisibility(View.INVISIBLE);

                if(veichleHandler.powermode)
                    tv_powerMode.setVisibility(View.VISIBLE);
                else
                    tv_powerMode.setVisibility(View.INVISIBLE);

                if(veichleHandler.actuationOn)
                    tv_actuationOn.setVisibility(View.VISIBLE);
                else
                    tv_actuationOn.setVisibility(View.INVISIBLE);

                //SEND MQTT
                if(veichleHandler.shorts)
                    mqttSender.publishMsg(SHORT_CHANNEL, "1");
                else
                    mqttSender.publishMsg(SHORT_CHANNEL, "0");

                if(veichleHandler.powermode)
                    mqttSender.publishMsg(POWERMODE_CHANNEL, "1");
                else
                    mqttSender.publishMsg(POWERMODE_CHANNEL, "0");

                if(veichleHandler.purge)
                    mqttSender.publishMsg(PURGE_CHANNEL, "1");
                else
                    mqttSender.publishMsg(PURGE_CHANNEL, "0");

                if(veichleHandler.motorOn)
                    mqttSender.publishMsg(MOTORON_CHANNEL, "1");
                else
                    mqttSender.publishMsg(MOTORON_CHANNEL, "0");

                if(veichleHandler.actuationOn)
                    mqttSender.publishMsg(ACTUATIONON_CHANNEL, "1");
                else
                    mqttSender.publishMsg(ACTUATIONON_CHANNEL, "0");

                mqttSender.publishMsg(STRATEGY_CHANNEL, Integer.toString(veichleHandler.strategy));
                mqttSender.publishMsg(SPEED_CHANNEL, Float.toString(veichleHandler.speedKmH));
                mqttSender.publishMsg(FUELCELLTEMP_CHANNEL, Float.toString(veichleHandler.fuelCellTemp));
                mqttSender.publishMsg(FUELCELLVOLTS_CHANNEL, Float.toString(veichleHandler.fuelCellVolt));
                mqttSender.publishMsg(SUPERCAPVOLTS_CHANNEL, Float.toString(veichleHandler.superCapVolt));
                mqttSender.publishMsg(CURRENT_CHANNEL, Float.toString(veichleHandler.motorAmps));
                mqttSender.publishMsg(MOTORDUTY_CHANNEL, Float.toString(veichleHandler.motorDuty));
                mqttSender.publishMsg(FANDUTY_CHANNEL, Float.toString(veichleHandler.fanDuty));

                mqttSender.publishMsg(STATUS_CHANNEL, "1");


            });
        });

    }

}