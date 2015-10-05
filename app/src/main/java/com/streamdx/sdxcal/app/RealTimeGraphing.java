package com.streamdx.sdxcal.app;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;
import com.streamdx.sdx.app.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by alvinl on 10/4/2014.
 */

public class RealTimeGraphing extends Activity {
        private final static String TAG = RealTimeGraphing.class.getSimpleName();

        public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
        public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

        private static final String UUID_CHAR =      "e7add780-b042-4876-aae1-112855353dd1";
        private static final UUID UUID_SERVICE =    UUID.fromString("00001234-0000-1000-8000-00805f9b34fb");
        public static final String okaystring = "uuidCharacteristic";

        public String datastring = "0";

        public String normstring = "0";

        public String valuea;

        public byte[] value;

        private TextView mConnectionState;
        private TextView mDataField;
        private String mDeviceName;
        private String mDeviceAddress;
        private ExpandableListView mGattServicesList;
        private BluetoothLeService mBluetoothLeService;
        private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
                new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
        private boolean mConnected = false;
        private BluetoothGatt mbluetoothgatt;
        private BluetoothGattCharacteristic mNotifyCharacteristic;
        private BluetoothGattCallback mBluetoothCallback;
        private final String LIST_NAME = "NAME";
        private final String LIST_UUID = "UUID";

        public BluetoothGattCharacteristic characteristic;

        private boolean notify = false;

        public final static String ACTION_DATA_AVAILABLE =
                "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";

        private final Handler mHandler = new Handler();
        private Runnable mTimer1;
        private Runnable mTimer2;
        private GraphView flowView;
        private GraphView volumeView;

        private GraphViewSeries flowSeries;
        private GraphViewSeries volumeSeries;
        private GraphViewSeries medialSeries;
        private GraphViewSeries lateralSeries;
        private GraphViewSeries totalSeries;

        private double graph2LastXValue = 5d;
        private Context mContext = null;

        // Code to manage Service lifecycle.
        private final ServiceConnection mServiceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
                if (!mBluetoothLeService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    finish();
                }

                // Automatically connects to the device upon successful start-up initialization.
                mBluetoothLeService.connect(mDeviceAddress);


            }
            public void onServiceDisconnected(ComponentName componentName) {
                mBluetoothLeService = null;
            }
        };

        //    Handles various events fired by the Service.
//    ACTION_GATT_CONNECTED: connected to a GATT server.
//    ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
//    ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
//    ACTION_DATA_AVAILABLE: received data from the device. This can be a result of read
//    or notification operations.
        private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                    mConnected = true;
                    updateConnectionState(R.string.connected);
                    invalidateOptionsMenu();
                } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                    mConnected = false;
                    updateConnectionState(R.string.disconnected);
                    invalidateOptionsMenu();
                    clearUI();

                } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                    // Show all the supported services and characteristics on the user interface.
                    displayGattServices(mBluetoothLeService.getSupportedGattServices());

                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                    String okay = intent.getExtras().getString(BluetoothLeService.okay);
                    value = mNotifyCharacteristic.getValue();
                    getData(value);

                }
            }
        };

        // If a given GATT characteristic is selected, check for supported features. This sample
        // demonstrates 'Read' and 'Notify' features. See
        // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
        // list of supported characteristic features.




        private void clearUI() {

        }

        TextView maxflowText;
        TextView averageflowText;
        TextView timeflowText;
        TextView volumeText;
        TextView voltageText;
        TextView packetdataText;
        TextView normvoltageText;
        TextView filenameText;
        EditText filenameEdit;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.graphingdata);

            final Intent intent = getIntent();
            mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
            mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

            // Sets up UI references.

            maxflowText =(TextView)findViewById(R.id.maximum_flow);
            averageflowText =(TextView)findViewById(R.id.average_flow);
            timeflowText =(TextView)findViewById(R.id.flow_time);
            volumeText =(TextView)findViewById(R.id.void_volume);
            voltageText =(TextView)findViewById(R.id.voltage_data);
            packetdataText =(TextView)findViewById(R.id.packet_data);
            normvoltageText = (TextView) findViewById(R.id.norm_voltage);
            filenameText = (TextView) findViewById(R.id.filename);
            filenameEdit = (EditText) findViewById(R.id.filenameEditText);


            getActionBar().setTitle("StreamDx: Uroflow Graph");
            getActionBar().setDisplayHomeAsUpEnabled(true);
            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

            // graph with dynamically generated horizontal and vertical labels

            volumeView = new LineGraphView(this, "Volume (mL) vs. Time(s)");
            flowView = new LineGraphView(this, "Flow rate(mL/s) vs. Time(s)");  //(context,graph title)


            // shaded under the curve
            ((LineGraphView) volumeView).setDrawBackground(true);
            ((LineGraphView) flowView).setDrawBackground(true);

//        ((LineGraphView) lateralView).setDrawBackground(true);
//            ((LineGraphView) totalView).setDrawBackground(true);

            volumeSeries = new GraphViewSeries( new GraphView.GraphViewData[] {
                    new GraphView.GraphViewData(1, 0d)
            });

            flowSeries = new GraphViewSeries( new GraphView.GraphViewData[] {
                    new GraphView.GraphViewData(1, 0d)
            });

            volumeView.addSeries(volumeSeries); // data
            volumeView.setViewPort(0,400);
            volumeView.setManualYAxisBounds(700, 0);
            volumeView.setScrollable(true);
            volumeView.setScalable(true);
            volumeView.getGraphViewStyle().setNumHorizontalLabels(11);
            volumeView.getGraphViewStyle().setNumVerticalLabels(6);
            volumeView.getGraphViewStyle().setGridColor(Color.GRAY);
            volumeView.setHorizontalLabels(new String[] {"0s", " ", " ", " ", " ", "10s", " ", " ", " ", " ", "20s"});
            volumeView.getGraphViewStyle().setTextSize(50);

            flowView.addSeries(flowSeries); // data
            flowView.setViewPort(0,400);
            flowView.setManualYAxisBounds(40, 0);
            flowView.setScrollable(true);
            flowView.setScalable(true);
            flowView.getGraphViewStyle().setNumHorizontalLabels(11);
            flowView.getGraphViewStyle().setNumVerticalLabels(5);
            flowView.getGraphViewStyle().setGridColor(Color.GRAY);
            flowView.setHorizontalLabels(new String[] {"0s", " ", " ", " ", " ", "10s", " ", " ", " ", " ", "20s"});
            flowView.getGraphViewStyle().setTextSize(50);

            LinearLayout volume_layout = (LinearLayout) findViewById(R.id.volumeGraph);
            volume_layout.addView(volumeView);

            LinearLayout flow_layout = (LinearLayout) findViewById(R.id.flowGraph);
            flow_layout.addView(flowView);

            mContext = this.getApplicationContext();
            Button btn = (Button) findViewById(R.id.uploadButton);
            btn.setOnClickListener(new Button.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Bundle b = new Bundle(1);
                    b.putString("DATA", datastring);
                    Intent upload = new Intent(mContext, UploadToServer.class);
                    upload.putExtras(b);
                    startActivity(upload);
                }
            });
        }

        public double c;
        public double d;

        public int i=0;
        public int t=0;

        public String v1;
        public int v2 = 0;


        public int f1 = 0;
        public int f2 = 0;

        public int f3 = 0;
        public int f4 = 0;

        public double ft = 0;
        public double ftd = 0;
        public double ft_avg;

        public double norm_val = 0;

        public double flow_rate = 0;
        public double sum_flow = 0;

        public double flow_time = 0;
        public double average_flow = 0;
        public double moving_avg_flow = 0;
        public double max_flow = 0;

        public double v_ratio = 0;

        public double prev_volume = 0;
        public double cal_volume = 0;

        public double fcomp = 0;

        public int packetlen;

        public void getData(byte[] value) {

            //convert the data packet into a string for display
            v1 = Arrays.toString(value);

            //check packet length
            packetlen = value.length;

            //if all data is present
            if(packetlen == 4) {

                //perform a bitwise and function with 255 to convert into unsigned 8 bit integer
                f1 = value[0] & 0XFF;
                f2 = value[1] & 0XFF;

                f3 = value[2] & 0XFF;
                f4 = value[3] & 0XFF;

                //combination of both bytes into one double type number

                ft = combinebytes(f1, f2);

                norm_val = combinebytes(f3,f4);

                v_ratio = ft/norm_val;

                ftd = ft;

                //write raw data to output before any data processing
                datastring = datastring+","+ft;

                normstring = normstring+","+norm_val;

                //begin data processing

                //remove any negative changes in raw voltage
                if(ft<fcomp){
                    ft = fcomp;
                }else{
                    fcomp = ft;
                }

                //30 point moving average of raw signal
                ft_avg = averagesignal(ft);

                //y = 1528x - 576.38
                //y = 1438.2x - 466.29

                cal_volume = 1528*(ft_avg) - 550;

                if(cal_volume<0.1){
                    cal_volume = 0;
                }

                //4 segment calibration
//                if(ft_avg>0.314299) {
//                    cal_volume = (ft_avg*179.97) + 107.95;
//                }else if(ft_avg>0.131408){
//                    cal_volume = (ft_avg*367.33) + 49.063;
//                }else if (ft_avg>0.0442524){
//                    cal_volume = (ft_avg*573.23) + 22.006;
//                }else{
//                    cal_volume = (ft_avg*1132.4) - 2.7386;
//                }

                //calculation of flow rate
                flow_rate = (cal_volume - prev_volume)/0.05;
                prev_volume = cal_volume;


                //20 point moving average applied to the flow rate
                moving_avg_flow = averageflow(flow_rate);

                if(moving_avg_flow > 40){
                    moving_avg_flow = 40;
                }

                if(moving_avg_flow<0.1){
                    moving_avg_flow = 0;
                }

                //find maximum_flow rate
                if(moving_avg_flow > max_flow) {
                    max_flow = moving_avg_flow;
                }


                //the threshold for a sample to count as a valid flow is set at 0.5mL/s
                if(moving_avg_flow>0.5){
                    sum_flow = sum_flow + moving_avg_flow;
                    t++;
                    average_flow = sum_flow/t;
                    flow_time = t*0.05;
                }

                graph2LastXValue += 1d;
                volumeSeries.appendData(new GraphView.GraphViewData(graph2LastXValue, cal_volume), true, 1600);
                flowSeries.appendData(new GraphView.GraphViewData(graph2LastXValue, moving_avg_flow), true, 1600);


                //set back to zero for next reading
                f1 = 0;
                f2 = 0;

                //Increase counter of number of readings
                i++;
            }

            //Update all shown values

            //max flow
            maxflowText.setText(String.format("Maximum flow: %.2f", max_flow) + "mL/s");

            //average flow
            averageflowText.setText(String.format("Average flow: %.2f",average_flow) + "mL/s");

            //flow time
            timeflowText.setText(String.format("Flow time: %.2f", flow_time) + "s");

            //voided volume
            volumeText.setText(String.format("Voided volume: %.2f", cal_volume) + "mL");

            //normalization voltage
            normvoltageText.setText(String.format("Norm voltage: %.7f", norm_val) + "V");

            //voltage data
            voltageText.setText(String.format("Voltage: %.7f", ftd) +"V");

            //packet data
            packetdataText.setText("Packet data: " + v1);

//            maxflowText.setText(" ");
//
//            //average flow
//            averageflowText.setText(" ");
//
//            //flow time
//            timeflowText.setText(" ");
//
//            //voided volume
//            volumeText.setText(" ");
//
//            //normalization voltage
//            normvoltageText.setText(" ");
//
//            //ratio voltage
//            v_ratioText.setText(" ");
//
//            //voltage data
//            voltageText.setText(" ");
//
//            //packet data
//            packetdataText.setText(" ");

        }

    public double a1=0;
    public double a2=0;
    public double a3=0;
    public double a4=0;
    public double a5=0;
    public double a6=0;
    public double a7=0;
    public double a8=0;
    public double a9=0;
    public double a10=0;
    public double a11=0;
    public double a12=0;
    public double a13=0;
    public double a14=0;
    public double a15=0;
    public double a16=0;
    public double a17=0;
    public double a18=0;
    public double a19=0;
    public double a20=0;
    public double a21=0;
    public double a22=0;
    public double a23=0;
    public double a24=0;
    public double a25=0;
    public double a26=0;
    public double a27=0;
    public double a28=0;
    public double a29=0;
    public double a30=0;
    public double avg_signal;

    public double averagesignal(double raw_avg ) {

        a1=a2;
        a2=a3;
        a3=a4;
        a4=a5;
        a5=a6;
        a6=a7;
        a7=a8;
        a8=a9;
        a9=a10;
        a10=a11;
        a11=a12;
        a12=a13;
        a13=a14;
        a14=a15;
        a15=a16;
        a16=a17;
        a17=a18;
        a18=a19;
        a19=a20;
        a20=a21;
        a21=a22;
        a22=a23;
        a23=a24;
        a24=a25;
        a25=a26;
        a26=a27;
        a27=a28;
        a28=a29;
        a29=a30;
        a30=raw_avg;

        avg_signal = (a1+a2+a3+a4+a5+a6+a7+a8+a9+a10+a11+a12+a13+a14+a15+a16+a17+a18+a19+a20+a21+a22+a23+a24+a25+a26+a27+a28+a29+a30)/30;

        return avg_signal;
    }

    public double c1=0;
    public double c2=0;
    public double c3=0;
    public double c4=0;
    public double c5=0;
    public double c6=0;
    public double c7=0;
    public double c8=0;
    public double c9=0;
    public double c10=0;
    public double c11=0;
    public double c12=0;
    public double c13=0;
    public double c14=0;
    public double c15=0;
    public double c16=0;
    public double c17=0;
    public double c18=0;
    public double c19=0;
    public double c20=0;
    public double avg_flow;

    public double averageflow(double raw_avg ) {

        c1=c2;
        c2=c3;
        c3=c4;
        c4=c5;
        c5=c6;
        c6=c7;
        c7=c8;
        c8=c9;
        c9=c10;
        c10=c11;
        c11=c12;
        c12=c13;
        c13=c14;
        c14=c15;
        c15=c16;
        c16=c17;
        c17=c18;
        c18=c19;
        c19=c20;
        c20=raw_avg;

        avg_flow = (c1+c2+c3+c4+c5+c6+c7+c8+c9+c10+c11+c12+c13+c14+c15+c16+c17+c18+c19+c20)/20;

        return avg_flow;
    }


        public double combinebytes(int v0, int v3) {

            if (v3 == 0) {
                v2 = v0;
            } else if (v3 == 1) {
                v2 = v0 + 256;
            } else if (v3 == 2) {
                v2 = v0 + 512;
            } else if (v3 == 3) {
                v2 = v0 + 768;
            } else if (v3 == 4) {
                v2 = v0 + 1024;
            } else if (v3 == 5) {
                v2 = v0 + 1280;
            } else if (v3 == 6) {
                v2 = v0 + 1536;
            } else if (v3 == 7) {
                v2 = v0 + 1792;
            } else if (v3 == 8) {
                v2 = v0 + 2048;
            } else if (v3 == 9) {
                v2 = v0 + 2304;
            } else if (v3 == 10) {
                v2 = v0 + 2560;
            } else if (v3 == 11) {
                v2 = v0 + 2816;
            } else if (v3 == 12) {
                v2 = v0 + 3072;
            } else if (v3 == 13) {
                v2 = v0 + 3328;
            } else if (v3 == 14) {
                v2 = v0 + 3584;
            } else if (v3 == 15) {
                v2 = v0 + 3840;
            } else if (v3 == 16) {
                v2 = v0 + 4096;
            } else if (v3 == 17) {
                v2 = v0 + 4352;
            } else if (v3 == 18) {
                v2 = v0 + 4608;
            } else if (v3 == 19) {
                v2 = v0 + 4864;
            } else if (v3 == 20) {
                v2 = v0 + 5120;
            } else if (v3 == 21) {
                v2 = v0 + 5376;
            } else if (v3 == 22) {
                v2 = v0 + 5632;
            } else if (v3 == 23) {
                v2 = v0 + 5888;
            } else if (v3 == 24) {
                v2 = v0 + 6144;
            } else if (v3 == 25) {
                v2 = v0 + 6400;
            } else if (v3 == 26) {
                v2 = v0 + 6656;
            } else if (v3 == 27) {
                v2 = v0 + 6912;
            } else if (v3 == 28) {
                v2 = v0 + 7168;
            } else if (v3 == 29) {
                v2 = v0 + 7424;
            } else if (v3 == 30) {
                v2 = v0 + 7680;
            } else if (v3 == 31) {
                v2 = v0 + 7936;
            } else if (v3 == 32) {
                v2 = v0 + 8192;
            } else if (v3 == 33) {
                v2 = v0 + 8448;
            } else if (v3 == 34) {
                v2 = v0 + 8704;
            } else if (v3 == 35) {
                v2 = v0 + 8960;
            } else if (v3 == 36) {
                v2 = v0 + 9216;
            } else if (v3 == 37) {
                v2 = v0 + 9472;
            } else if (v3 == 38) {
                v2 = v0 + 9728;
            } else if (v3 == 39) {
                v2 = v0 + 9984;
            } else if (v3 == 40) {
                v2 = v0 + 10240;
            } else if (v3 == 41) {
                v2 = v0 + 10496;
            } else if (v3 == 42) {
                v2 = v0 + 10752;
            } else if (v3 == 43) {
                v2 = v0 + 11008;
            } else if (v3 == 44) {
                v2 = v0 + 11264;
            } else if (v3 == 45) {
                v2 = v0 + 11520;
            } else if (v3 == 46) {
                v2 = v0 + 11776;
            } else if (v3 == 47) {
                v2 = v0 + 12032;
            } else if (v3 == 48) {
                v2 = v0 + 12288;
            } else if (v3 == 49) {
                v2 = v0 + 12544;
            } else if (v3 == 50) {
                v2 = v0 + 12800;
            } else if (v3 == 51) {
                v2 = v0 + 13056;
            } else if (v3 == 52) {
                v2 = v0 + 13312;
            } else if (v3 == 53) {
                v2 = v0 + 13568;
            } else if (v3 == 54) {
                v2 = v0 + 13824;
            } else if (v3 == 55) {
                v2 = v0 + 14080;
            } else if (v3 == 56) {
                v2 = v0 + 14336;
            } else if (v3 == 57) {
                v2 = v0 + 14592;
            } else if (v3 == 58) {
                v2 = v0 + 14848;
            } else if (v3 == 59) {
                v2 = v0 + 15104;
            } else if (v3 == 60) {
                v2 = v0 + 15360;
            } else if (v3 == 61) {
                v2 = v0 + 15616;
            } else if (v3 == 62) {
                v2 = v0 + 15872;
            } else if (v3 == 63) {
                v2 = v0 + 16128;
            }

            if(v0+v3==0) {
                v2 = 0;
            }

            c = (1.0*(v2));

            d = c*3.3/4096;

            //d = c*250/16384;

            return d;
        }


        @Override
        protected void onResume() {
            super.onResume();
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
            if (mBluetoothLeService != null) {
                final boolean result = mBluetoothLeService.connect(mDeviceAddress);
                Log.d(TAG, "Connect request result=" + result);
            }
        }


        FileOutputStream outputStream;
        String filename;
        String filenamencap = "";
        String time = "";
        public int countseconds;

        @Override
        protected void onPause() {
            super.onPause();
            mHandler.removeCallbacks(mTimer1);
            unregisterReceiver(mGattUpdateReceiver);

//            maximumText.setText("Maximum: N/A");
//            averageText.setText("Average: N/A");

            Calendar c = Calendar.getInstance();
            int month = c.get(Calendar.MONTH) + 1;
            int day = c.get(Calendar.DAY_OF_MONTH);
            int hour24 = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);
            int seconds = c.get(Calendar.SECOND);

            countseconds = i/20;

            //Format Mo, DD, HH, MM, Duration

            filename = filenameEdit.getText().toString();
            if(filename.matches("")) {
                filename = String.format("%02d, %02d, %02d, %02d, ", month, day, hour24, minute) + countseconds;
            }
            filenamencap = filename +" P6.1";

            generateTextfile(filename,datastring);
            generateTextfile(filenamencap,normstring);
        }


        public void generateTextfile(String sFileName, String sBody){
            try
            {
                File root = new File(Environment.getExternalStorageDirectory(), "Verification Data");
                if (!root.exists()) {
                    root.mkdirs();
                }
                File textfile = new File(root, sFileName);
                FileWriter writer = new FileWriter(textfile);
                writer.append(sBody);
                writer.flush();
                writer.close();
                Toast.makeText(this, "Saved as " + filename, Toast.LENGTH_LONG).show();
            }
            catch(IOException e)
            {
                e.printStackTrace();

            }
        }




        @Override
        protected void onDestroy() {
            super.onDestroy();
            unbindService(mServiceConnection);
            mBluetoothLeService = null;
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.gatt_services, menu);
            if (mConnected) {
                menu.findItem(R.id.menu_connect).setVisible(false);
                menu.findItem(R.id.menu_disconnect).setVisible(true);
            } else {
                menu.findItem(R.id.menu_connect).setVisible(true);
                menu.findItem(R.id.menu_disconnect).setVisible(false);
            }
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch(item.getItemId()) {
                case R.id.menu_connect:
                    mBluetoothLeService.connect(mDeviceAddress);
                    return true;
                case R.id.menu_disconnect:
                    mBluetoothLeService.disconnect();
                    return true;
                case android.R.id.home:
                    onBackPressed();
                    return true;
            }
            return super.onOptionsItemSelected(item);
        }

        private void updateConnectionState(final int resourceId) {
            runOnUiThread( new Runnable() {
                @Override
                public void run() {
                }
            });
        }

        private void displayGattServices(List<BluetoothGattService> gattServices) {
            if (gattServices == null) return;
            String uuid = null;
            String unknownServiceString = getResources().getString(R.string.unknown_service);
            String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
            ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
            ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                    = new ArrayList<ArrayList<HashMap<String, String>>>();
            mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

            // Loops through available GATT Services.
            for (BluetoothGattService gattService : gattServices) {
                HashMap<String, String> currentServiceData = new HashMap<String, String>();
                uuid = gattService.getUuid().toString();
                currentServiceData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
                currentServiceData.put(LIST_UUID, uuid);
                gattServiceData.add(currentServiceData);

                ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                        new ArrayList<HashMap<String, String>>();
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                ArrayList<BluetoothGattCharacteristic> charas =
                        new ArrayList<BluetoothGattCharacteristic>();

                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    charas.add(gattCharacteristic);
                    HashMap<String, String> currentCharaData = new HashMap<String, String>();
                    uuid = gattCharacteristic.getUuid().toString();
                    characteristic = gattCharacteristic;
                    currentCharaData.put(
                            LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                    currentCharaData.put(LIST_UUID, uuid);
                    gattCharacteristicGroupData.add(currentCharaData);
                }
                mGattCharacteristics.add(charas);
                gattCharacteristicData.add(gattCharacteristicGroupData);
            }

            mNotifyCharacteristic = characteristic;

            String UUID = characteristic.getUuid().toString();
            Toast.makeText(RealTimeGraphing.this, "Connected", Toast.LENGTH_SHORT).show();
            //Toast.makeText(RealTimeGraphing.this, UUID, Toast.LENGTH_SHORT).show();
            mBluetoothLeService.setCharacteristicNotification(
                    characteristic, true);

        }

        private static IntentFilter makeGattUpdateIntentFilter() {
            final IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
            intentFilter.addAction(  BluetoothLeService.ACTION_GATT_DISCONNECTED);
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
            return intentFilter;
        }
    }

