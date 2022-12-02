package com.example.wheelboardapplication;

import android.app.Activity;
import android.hardware.usb.UsbDeviceConnection;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;

public class DataReceiver {

    public byte TERMINATOR = '|';
    public byte INITIATOR = '@';
    public int speedKmH = 0;
    public float pressure = 0;
    public boolean btnEn = false;

    public UsbSerialPort port;

    public DataReceiver(UsbSerialPort portIn, UsbDeviceConnection connection) {

        try {
            port = portIn;
            port.open(connection);
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        }
        catch(IOException ex){
            return;
        }
    }

    public void parse(String inMsg){
        int msgId, msgLen;

        short RxData[] = new short[8];

        String[] inMsgArr = inMsg.split(":");

        msgId = Integer.parseInt(inMsgArr[1]);
        msgLen = Integer.parseInt(inMsgArr[2]);
        int msgArrIndex = 3;

        for(int rxDataIndex = 0; rxDataIndex < msgLen; rxDataIndex++) {
            RxData[rxDataIndex] = (short) Integer.parseInt(inMsgArr[msgArrIndex]);
            msgArrIndex++;

        }

        switch(msgId){
            case 31:
                speedKmH = RxData[0];
                break;
            case 32:
                btnEn = (RxData[0] == 1);
                break;
            case 33:
                pressure = (RxData[0] | RxData[1] << 8) / (float) 100;
            default:
                break;
        }
    }
    public String[] processedDataArr(byte[] data, int msgLen){

        String[] dataStrArr = new String[30];
        String subStr = "";

        boolean insertingNow = false;
        int insertBaseIndex = 0, outArrIndex = 0;

        byte[] subData = new byte[50];


        for(int i = 0; i < msgLen; i ++){

            if(data[i] == INITIATOR){
                insertingNow = true;
                insertBaseIndex = i;
            }
            if((data[i] == TERMINATOR && insertingNow) || i+1 == msgLen){
                insertingNow = false;
                subStr = new String(subData);
                dataStrArr[outArrIndex] = subStr;
                outArrIndex ++;
            }

            if(insertingNow){
                subData[i - insertBaseIndex] = data[i];
            }

        }
        String[] outArr = new String[outArrIndex];
        for(int i = 0; i < outArr.length; i ++){
            outArr[i] = dataStrArr[i];
        }
        return outArr;

    }

    public void startEmptyBuffer(byte[] buf, int lastBufIndex){

        Runnable emptyBuffer = new Runnable() {
            @Override
            public void run() {
                String[] processedDataArr = processedDataArr(buf, lastBufIndex);

                for(int i = 0; i < processedDataArr.length - 1; i++){
                    parse(processedDataArr[i]);
                }
            }
        };

        Thread emptyBuf = new Thread(emptyBuffer);
        emptyBuf.start();

    }
    public boolean containsTerm(byte[] data){
        for(int i = 0; i < data.length; i++){
            if(data[i] == TERMINATOR){
                return true;
            }
        }
        return false;
    }

}