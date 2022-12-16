package com.example.wheelboardapplication;

import java.nio.ByteBuffer;
import java.util.Arrays;

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
    public int strategy = 0;
    public float speedKmH = 0;
    public float fuelCellTemp = 0;
    public float fuelCellVolt = 0;
    public float superCapVolt = 0;
    public float fuelCellAmps = 0;
    public float motorDuty= 0;
    public float fanDuty = 0;

    //ID service board
    static final private int emeID = 0x010;
    static final private int speedID = 0x011;
    static final private int tempID = 0x012;
    static final private int FC_VoltID = 0x013;
    static final private int SC_VoltID = 0x014;
    //ID attuazione
    static final private int currentID = 0x030;
    static final private int motorDutyID = 0x031;
    static final private int fanDutyID = 0x032;
    //ID volante
    static final private int buttonsID = 0x020;

    IdraHandler(){
    }

    public void parseMessage( int msgId, int msgLen, byte[] RxData){

        switch(msgId){

            case emeID:
                H2Eme = (Byte.toUnsignedInt(RxData[0]) == 1);
                deadEme = (Byte.toUnsignedInt(RxData[1]) == 1);
                extEme = (Byte.toUnsignedInt(RxData[2]) == 1);
                intEme = (Byte.toUnsignedInt(RxData[3]) == 1);
                break;
            case speedID:
                speedKmH = getFloatMemCpy(Arrays.copyOf(RxData, msgLen));
                break;
            case tempID:
                fuelCellTemp = getFloatMemCpy(Arrays.copyOf(RxData, msgLen));
                break;
            case FC_VoltID:
                fuelCellVolt = getFloatMemCpy(Arrays.copyOf(RxData, msgLen));
                break;
            case SC_VoltID:
                superCapVolt = getFloatMemCpy(Arrays.copyOf(RxData, msgLen));
                break;
            case currentID:
                fuelCellAmps = getFloatMemCpy(Arrays.copyOf(RxData, msgLen));
                break;
            case motorDutyID:
                motorDuty = getFloatMemCpy(Arrays.copyOf(RxData, msgLen));
                break;
            case fanDutyID:
                fanDuty = getFloatMemCpy(Arrays.copyOf(RxData, msgLen));
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
/*
    private float getFloatMemCpy(byte[] data){
        float outValue = -1;

        if( data.length == 4){
            outValue = ByteBuffer.wrap(data).getFloat();
        }

        return outValue;
    }*/
    public float getFloatMemCpy(byte... data) {
        return ByteBuffer.wrap(data).getFloat();
    }

    public String getEmergencyString(){
        String out = "";
        if(H2Eme)
            out = "Emergenza idrogeno";
        if(deadEme)
            out = "Emergenza deadman";
        if(intEme)
            out = "Emergenza interna";
        if(extEme)
            out = "Emergenza esterna";

        return out;
    }
}
