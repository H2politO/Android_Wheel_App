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
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

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

    //varMqtt
    String clientId = MqttClient.generateClientId();
    MqttAndroidClient mqttClient;
    boolean connectedClient;


    Intent intent = new Intent(ACTION_USB_PERMISSION);


    //textView
    TextView tv_deviceUsb;
    TextView tv_speed;
    TextView tv_pressure;
    TextView tv_emergences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idra);
        tv_deviceUsb = findViewById(R.id.usbDevice);
        tv_speed = findViewById(R.id.speed);
        tv_pressure = findViewById(R.id.pressure);
        tv_emergences = findViewById(R.id.emergences);

        //initiSerial comunication, and request permission
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
        }else{
            tv_deviceUsb.setText("Device not found");
        }

        //connection with client mqtt
        mqttClient = new MqttAndroidClient(IdraActivity.this, SERVER_URI, clientId, Ack.AUTO_ACK);
        try{
            IMqttToken token = mqttClient.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(IdraActivity.this, "Mqtt client connect", Toast.LENGTH_SHORT).show();
                    connectedClient = true;
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(IdraActivity.this, "Mqtt client not connect",  Toast.LENGTH_SHORT).show();
                    connectedClient = false;
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
        publish(SPEED_CHANNEL, "Hello World onCreate");
    }


    @Override
    protected void onStart() {
        super.onStart();

        initSerial();
        String msgMqtt = "Hello world";
        if(connectedClient)
            publish(SPEED_CHANNEL, msgMqtt);
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


    //function for initialize the serial device
    private void initSerial() {
        //find all device connected
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        ProbeTable customTable = new ProbeTable();
        customTable.addProduct(0x0483, 0x5740, CdcAcmSerialDriver.class);
        //find all drivers for the usb device
        UsbSerialProber prober = new UsbSerialProber(customTable);
        List<UsbSerialDriver> availableDrivers = prober.findAllDrivers(usbManager);

        if(availableDrivers.isEmpty()){
            Toast.makeText(getApplicationContext(), "Driver not found", Toast.LENGTH_SHORT).show();
            return;
        }
        //use first driver available
        UsbSerialDriver usbDriver = availableDrivers.get(0);
        UsbSerialPort port = usbDriver.getPorts().get(0);
        //take usbConnection
        UsbDeviceConnection usbDeviceConnection = usbManager.openDevice(usbDriver.getDevice());

        dataReceiver = new DataReceiver(port, usbDeviceConnection);

        Thread readUsbThread = new Thread(this.serialReceiverFunction);

        readUsbThread.start();
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
                        //Run on separate thred(slow)
                        dataReceiver.startEmptyBuffer(dataBuf, lastBufIndex);

                        //clean buffer
                        dataBuf = new byte[BUFF_SIZE];
                        lastBufIndex = 0;
                    }
                    //call the thread of ui to update value

                    runOnUiThread(() -> {
                        //set the field of ui to update
                        tv_speed.setText(Integer.toString(dataReceiver.speedKmH));
                    });
                }
            }catch (IOException ioE){
                finish();
            }
        }
    };

    private void publish(String topic, byte[] payload){
        if(!connectedClient)
            return;
        MqttMessage message = new MqttMessage(payload);
        message.setQos(0);
        message.setRetained(true);
        mqttClient.publish(topic, message);
    }

    private void publish(String topic, String payload){
        byte[] data = payload.getBytes(StandardCharsets.UTF_8);
        publish(topic, data);
    }
}