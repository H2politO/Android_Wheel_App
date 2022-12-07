package com.example.wheelboardapplication;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;

import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;

public class MqttSender {
    //const field
    private static final String SERVER_URI = "tcp://broker.hivemq.com:1883";

    MqttAndroidClient mqttAndroidClient;

    public MqttSender(Context context, String serverUri, String clientID, Ack ack){

        mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientID, ack);
        IMqttToken token;

        token = mqttAndroidClient.connect();
        token.setActionCallback(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Toast.makeText(context, "Mqtt client connect", Toast.LENGTH_SHORT).show();
                //insert dequeue list of message if necessary
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Toast.makeText(context, "Mqtt client NOT connect", Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void publishMsg(String topic, String payload){
        publish(topic, payload);
    }

    private void publish(String topic, byte[] payload){
        if(!mqttAndroidClient.isConnected())
            return;
        MqttMessage message = new MqttMessage(payload);
        message.setQos(0);
        message.setRetained(true);
        mqttAndroidClient.publish(topic, message);
    }

    private void publish(String topic, String payload){
        byte[] data = payload.getBytes(StandardCharsets.UTF_8);
        publish(topic, data);
    }
}


