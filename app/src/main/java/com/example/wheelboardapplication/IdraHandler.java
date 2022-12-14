package com.example.wheelboardapplication;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class IdraHandler {
    //EMERGENCIES
    public boolean H2Eme = false;
    public boolean deadEme = false;
    public boolean extEme = false;
    public boolean intEme = false;
    //BUTTONS
    public boolean shorts = false;
    public boolean powermode = false;
    public boolean purge = false;
    public boolean motorOn = false;
    //MISCELLANEOUS
    public boolean actuationOn = false;
    public int strategy = 0;
    public float speedKmH = 0;
    public float fuelCellTemp = 0;
    public float fuelCellVolt = 0;
    public float superCapVolt = 0;
    public float motorAmps = 0;
    public float motorDuty= 0;
    public float fanDuty = 0;

    //ID service board
    static final private int emeID = 0x010;
    static final private int speedID = 0x011;
    static final private int tempID = 0x012;
    static final private int FC_VoltID = 0x013;
    static final private int SC_VoltID = 0x014;
    //ID attuazione
    static final private int actuationHB = 0x03F;
    static final private int currentID = 0x030;
    static final private int motorDutyID = 0x031;
    static final private int fanDutyID = 0x032;
    //ID volante
    static final private int buttonsID = 0x020;

    private long lastActHBTime = 0;
    private long currMsgTime = 0;

    IdraHandler(){
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                //ENSURE parseMessage runs at least once every second
                if(System.currentTimeMillis() - currMsgTime > 750) {
                    parseMessage(-1, 0, null);
                    DataReceiver.forceUpdate();
                }
            }
        }, 0, 1000);//put here time 1000 milliseconds=1 second

    }

    public void parseMessage( int msgId, int msgLen, byte[] RxData){

        currMsgTime = System.currentTimeMillis();
        long test = (currMsgTime - lastActHBTime);
        actuationOn = test <= 1100;

        switch(msgId){
            case actuationHB:
                lastActHBTime = currMsgTime;
                break;
            case emeID:
                H2Eme = (Byte.toUnsignedInt(RxData[0]) == 1);
                deadEme = (Byte.toUnsignedInt(RxData[1]) == 1);
                extEme = (Byte.toUnsignedInt(RxData[2]) == 1);
                intEme = (Byte.toUnsignedInt(RxData[3]) == 1);
                break;
            case speedID:
                speedKmH = byteToFloat(RxData[3], RxData[2], RxData[1], RxData[0]);
                break;
            case tempID:
                fuelCellTemp = byteToFloat(RxData[3], RxData[2], RxData[1], RxData[0]);
                break;
            case FC_VoltID:
                fuelCellVolt = byteToFloat(RxData[3], RxData[2], RxData[1], RxData[0]);
                break;
            case SC_VoltID:
                superCapVolt = byteToFloat(RxData[3], RxData[2], RxData[1], RxData[0]);
                break;
            case currentID:
                motorAmps = byteToFloat(RxData[3], RxData[2], RxData[1], RxData[0]);
                break;
            case motorDutyID:
                motorDuty = byteToFloat(RxData[3], RxData[2], RxData[1], RxData[0]);
                break;
            case fanDutyID:
                fanDuty = byteToFloat(RxData[3], RxData[2], RxData[1], RxData[0]);
                break;
            case buttonsID:
                strategy = Byte.toUnsignedInt(RxData[0]);
                motorOn = (RxData[1] == 1);
                purge = (RxData[2] == 1);
                powermode = (RxData[3] == 1);
                shorts = (RxData[4] == 1);
                break;

            default:
                break;
        }
    }

    private float getFloatMemCpy(byte[] rawData, int msgLen){
        float outValue = -1;
        byte[] data = Arrays.copyOf(rawData, msgLen);
        byte tmp;

        //Flip incoming data array
        for(int i = 0; i < data.length; i ++){
            tmp = data[i];
            data[i] = data[data.length - i - 1];
            data[data.length - i - 1] = tmp;
        }

        if( data.length == 4){
            outValue = ByteBuffer.wrap(data).getFloat();
        }

        return outValue;
    }

    private float byteToFloat(byte... data) {
        return ByteBuffer.wrap(data).getFloat();
    }

    public String getEmergencyString(){
        String out = "";
        if(H2Eme)
            out = "??????Em. H2";
        if(deadEme)
            out = "??????Em. DeadMan";
        if(intEme)
            out = "??????Em. Interna";
        if(extEme)
            out = "??????Em. Esterna";

        return out;
    }


}
