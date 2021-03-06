package com.example.usbserialallaboutcircuits;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {


    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";


    UsbDevice device;
    UsbManager usbManager;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;


    EditText TextEntrada;
    Button BtnIniciar, BtnEnviar, BtnDetener, BtnBorrar;
    TextView TxVwPantalla;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        usbManager = (UsbManager)getSystemService(this.USB_SERVICE);
        TextEntrada = (EditText)findViewById(R.id.editTextInput);
        BtnIniciar = (Button)findViewById(R.id.buttonBeg);
        BtnEnviar = (Button)findViewById(R.id.buttonSd);
        BtnDetener = (Button)findViewById(R.id.buttonSt);
        BtnBorrar = (Button)findViewById(R.id.buttonCl);
        TxVwPantalla = (TextView)findViewById(R.id.TextViewReci);
        TxVwPantalla.setMovementMethod(new ScrollingMovementMethod());

        setUiEnabled(false);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver,filter);
        

    }// Termina OnCreate

    public void onClickStart(View view) {

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()){
            boolean keep = true;
            for (Map.Entry<String, UsbDevice>entry:usbDevices.entrySet()){
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if(deviceVID == 6790)// Arduino Vendor ID
                //if(deviceVID == 0x2341)// Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this,0, new Intent(ACTION_USB_PERMISSION),0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                }else{
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }
    }// Termina onclickStart


    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // Broadcast Receiver to automatically
        // start and stop the serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)){
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted){
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device,connection);
                    if (serialPort != null){
                        if (serialPort.open()){ // Set serial conection parameters
                            setUiEnabled(true); // Enable Buttons in UI
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallBack);
                            tvAppend(TxVwPantalla, "Serial connection Opened!\n");
                            Toast.makeText(context, "Serial connection Opened!", Toast.LENGTH_SHORT).show();
                        }else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                            Toast.makeText(context, "PORT NOT OPEN", Toast.LENGTH_SHORT).show();
                        }
                    }else {
                        Log.d("SERIAL", "PORT IS NULL");
                        Toast.makeText(context, "PORT IS NULL", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Log.d("SERIAL", "PERM NOT GRANTED");
                    Toast.makeText(context, "PERM NOT GRANTED", Toast.LENGTH_SHORT).show();
                }
            }else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)){
                onClickStart(BtnIniciar);
            }else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)){
                onClickStop(BtnDetener);
            }
        };
    };


    UsbSerialInterface.UsbReadCallback mCallBack = new UsbSerialInterface.UsbReadCallback(){
        // DEfining CallBack which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0){
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                data.concat("\n");
                tvAppend(TxVwPantalla, data);
            }catch (UnsupportedEncodingException e){
                e.printStackTrace();
            }
        }
    };


    private void tvAppend(TextView tv, CharSequence text){
        final TextView ftv = tv;
        final CharSequence ftext = text;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
            }
        });
    }

    public void onClickSend(View view) {
        String string = TextEntrada.getText().toString();
        serialPort.write(string.getBytes());
        tvAppend(TxVwPantalla, "\nData Sent: " + string + "\n");
    }


    public void onClickStop(View view) {
        setUiEnabled(false);
        serialPort.close();
        tvAppend(TxVwPantalla,"\nSerial Connection Closed! \n");
    }

    public void onClickClear(View view) {
        TxVwPantalla.setText(" ");
    }


    public void setUiEnabled(boolean bool){
        BtnIniciar.setEnabled(!bool);
        BtnEnviar.setEnabled(bool);
        BtnDetener.setEnabled(bool);
        TxVwPantalla.setEnabled(bool);

    }


}// End activity