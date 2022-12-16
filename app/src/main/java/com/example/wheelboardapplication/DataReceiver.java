package com.example.wheelboardapplication;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class DataReceiver implements SerialInputOutputManager.Listener {

    static final public byte DIRECT_MODE = 0;
    static final public byte NUCLEO_MODE = 1;

    static final public byte IDRA = 1;
    static final public byte JUNO = 1;

    final private byte TERMINATOR = '|';
    final private byte INITIATOR = '@';
    final private int BUFFER_LEN = 100;


    private boolean isMessageSending = false;
    private boolean isCallbackEnabled = false;
    private byte[] dataBuff = new byte[BUFFER_LEN];
    private int buffIndex = 0;

    public UsbSerialPort port;
    private final byte veichle;
    private final Context baseContext;
    private final UsbManager usbManager;
    private Runnable onNewDataCallback;

    //Constructor with callback
    public DataReceiver(UsbManager manager, Context context, byte SerialMode, byte vecihleSelect, Runnable newDataCallback) {

        isCallbackEnabled = true;
        onNewDataCallback = newDataCallback;
        baseContext = context;
        usbManager = manager;
        veichle = vecihleSelect;
        initSerial( SerialMode );

    }

    public void parseMsg(String inMsg){
        int msgId, msgLen;

        byte[] RxData;
        RxData = new byte[8]; //NOTE: the value of RxData could be negative as there is no usnigned option, it should be handeld with getFloat or BYte.getUnsignedInt

        String[] inMsgArr = inMsg.split(":");

        msgId = Integer.parseInt(inMsgArr[1]);
        msgLen = Integer.parseInt(inMsgArr[2]);
        int msgArrIndex = 3;

        for(int rxDataIndex = 0; rxDataIndex < msgLen; rxDataIndex++) {
            RxData[rxDataIndex] = (byte) Integer.parseInt(inMsgArr[msgArrIndex]);
            msgArrIndex++;

        }

        if(veichle == IDRA)
            IdraActivity.veichleHandler.parseMessage(msgId, msgLen, RxData);


        if(isCallbackEnabled)
            onNewDataCallback.run();
    }

    public void pushBuff(byte[] data){
        int i;
        for(i = 0; i < data.length; i++){
            //Put all incoming data in dataBuf
            dataBuff[buffIndex] = data[i];
            buffIndex ++;
            //Start building a message when initiator detected
            if(!isMessageSending && data[i] == INITIATOR){
                isMessageSending = true;
            }
            //Stop building message on terminator
            if(isMessageSending && data[i] == TERMINATOR){
                isMessageSending = false;
                //Parse current message
                parseMsg(new String(Arrays.copyOf(dataBuff, buffIndex)));
                buffIndex = 0;
            }
        }

    }

    private void initSerial(byte mode){

        int baudRate = -1;

        ProbeTable customTable = new ProbeTable();

        switch(mode) {
            case DIRECT_MODE:
                customTable.addProduct(0x0483, 0x5740, CdcAcmSerialDriver.class); //STM32 Direct USB
                baudRate = 9600;
                break;

            case NUCLEO_MODE:
                customTable.addProduct(0x0483, 0x374B, CdcAcmSerialDriver.class);//Nucleo BOARD
                baudRate = 115200;
                break;
        }

        //customTable.addProduct(0x2341, 0x0043, CdcAcmSerialDriver.class);
        UsbSerialProber prober = new UsbSerialProber(customTable);
        List<UsbSerialDriver> availableDrivers = prober.findAllDrivers(usbManager);

        if (availableDrivers.isEmpty()) {
            Toast toast = Toast.makeText(baseContext, "No device accessible", Toast.LENGTH_SHORT);
            toast.show();
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);

        port = driver.getPorts().get(0); // Most devices have just one port (port 0)
        UsbDeviceConnection connection = usbManager.openDevice(driver.getDevice());

        try{
            port.open(connection);
            port.setParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

            //After initializing the serial port dump the serial buffer as it may contain garbage
            byte[] dataNULL = new byte[BUFFER_LEN];
            port.read(dataNULL, 100);

        }
        catch (IOException ex){
            Toast toast = Toast.makeText(baseContext, "Error opening serial port", Toast.LENGTH_SHORT);
            toast.show();
        }

        SerialInputOutputManager usbIoManager = new SerialInputOutputManager(port, this);
        usbIoManager.start();

    }


    @Override
    public void onNewData(byte[] data) {
        pushBuff(data);
    }

    @Override
    public void onRunError(Exception e) {
    }
}