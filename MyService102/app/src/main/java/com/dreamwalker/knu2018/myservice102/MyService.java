package com.dreamwalker.knu2018.myservice102;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.support.v4.app.ActivityCompat.startActivityForResult;

/**
 * Created by KNU2017 on 2018-02-12.
 */

public class MyService extends Service {

    private static final String TAG = "MyService";

    private static BluetoothGattCharacteristic mSCharacteristic, mModelNumberCharacteristic, mSerialPortCharacteristic,
            mCommandCharacteristic;
    private static BluetoothGattCharacteristic mHexiCharacteristic, mHexiStartCharacteristic;
    private static BluetoothGattCharacteristic mSCharacteristic2, mHexiPressureCharacteristic;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

    public static final String SerialPortUUID = "0000dfb1-0000-1000-8000-00805f9b34fb";
    public static final String CommandUUID = "0000dfb2-0000-1000-8000-00805f9b34fb";
    public static final String ModelNumberStringUUID = "00002a24-0000-1000-8000-00805f9b34fb";

    // TODO: 2018-01-24 헥시웨어 전용
    public static final String HexiStartUUID = "00002011-0000-1000-8000-00805f9b34fb";
    public static final String HexiStringUUID = "00002032-0000-1000-8000-00805f9b34fb";

    public static final String UUID_CHAR_ACCEL = "00002001-0000-1000-8000-00805f9b34fb";
    public static final String UUID_CHAR_GYRO = "00002002-0000-1000-8000-00805f9b34fb";
    public static final String UUID_CHAR_MAGNET = "00002003-0000-1000-8000-00805f9b34fb";

    public static final String UUID_CHAR_AMBIENT_LIGHT = "00002011-0000-1000-8000-00805f9b34fb";
    public static final String UUID_CHAR_TEMPERATURE = "00002012-0000-1000-8000-00805f9b34fb";
    public static final String UUID_CHAR_HUMIDITY = "00002013-0000-1000-8000-00805f9b34fb";
    public static final String UUID_CHAR_PRESSURE = "00002014-0000-1000-8000-00805f9b34fb";
    public static final String UUID_CHAR_HEARTRATE = "00002021-0000-1000-8000-00805f9b34fb";
    public static final String UUID_CHAR_BATTERY = "00002a19-0000-1000-8000-00805f9b34fb";


    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    BluetoothGatt bluetoothGatt;

    Boolean btScanning = false;

    ArrayList<BluetoothDevice> devicesDiscovered = new ArrayList<>();

    // Stops scanning after 5 seconds.
    private Handler mHandler = new Handler();
    private final static int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 5000;
    private static final long TIMER_PERIOD = 15000;
    int deviceIndex = 0;
    private String device_address;

    Timer myTimer;
    TimerTask timerTask;
    TimerTask readTimerTask;

    AsyncTask startScanTask, stopScanTask;
    Boolean findDeviceFlag = false;
    Boolean timerTaskFlag = false;
    BluetoothDevice a;

    private String mBluetoothDeviceAddress;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private int mConnectionState = STATE_DISCONNECTED;

    public MyService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Log.e(TAG, "onCreate: 블루투스 켜주세요");
        }


        //timer = new Timer();
        //timer2 = new Timer();
        myTimer = new Timer();

        timerTask = new TimerTask() {
            @Override
            public void run() {
                Log.e(TAG, "onStartCommand: 서비스 동작 중 이에요 ");
                startScanning();
            }
        };

      /*  stopTimerTask = new TimerTask() {
            @Override
            public void run() {
                Log.e(TAG, "onStartCommand: 스캔종료  ");
                stopScanning();
            }
        };*/
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        Notification notification = new Notification.Builder(MyService.this)
//                .setContentTitle("Title example")
//                .setContentText("exex")
//                .setSmallIcon(R.mipmap.ic_launcher)
//                .build();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("알림바 실험")
                .setContentText("알림바입니다..");
        Intent resultIntent = new Intent(getApplicationContext(), Main2Activity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(Main2Activity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPandingIntent = stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        mBuilder.setContentIntent(resultPandingIntent);

        startForeground(1, mBuilder.build());
        btScanner.startScan(leScanCallback);

        //timer.schedule(timerTask, 0, TIMER_PERIOD);
        //timer2.schedule(stopTimerTask, 5000, TIMER_PERIOD);
        return START_STICKY;
    }

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //peripheralTextView.append("Index: " + deviceIndex + ", Device Name: " + result.getDevice().getName() + " rssi: " + result.getRssi() + "\n");
            // peripheralTextView.append("Index: " + deviceIndex + result.getDevice() + "\n");
            Log.e(TAG, "onScanResult: " + "Device Name :" + result.getDevice().getName() + "\n"
                    + "Device Address :" + result.getDevice().getAddress() + "\n "
                    + "Device UUID :" + result.getDevice().getUuids() + "\n "
                    + "Device RSSI :" + result.getRssi() + "\n");
            if (result.getDevice().getAddress().equals("00:43:40:08:00:0F")) {
                mBluetoothDeviceAddress = result.getDevice().getAddress();
                connect(mBluetoothDeviceAddress);
                btScanner.stopScan(leScanCallback);
            }
            devicesDiscovered.add(result.getDevice());

            //deviceIndex++;
            // auto scroll for text view
            //final int scrollAmount = peripheralTextView.getLayout().getLineTop(peripheralTextView.getLineCount()) - peripheralTextView.getHeight();
            // if there is no need to scroll, scrollAmount will be <=0
            //if (scrollAmount > 0) {
            //    peripheralTextView.scrollTo(0, scrollAmount);
            //}

        }
    };

    public void startScanning() {


        System.out.println("start scanning");
        btScanning = true;
        deviceIndex = 0;
        Log.e(TAG, "startScanning: ");

        startScanTask.execute(new Runnable() {
            @Override
            public void run() {
                devicesDiscovered.clear();
                btScanner.startScan(leScanCallback);
            }
        });

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScanning();
                //bluetoothGatt.disconnect();
                // bluetoothGatt.close();
            }
        }, SCAN_PERIOD);
    }

    public void stopScanning() {
        Log.e(TAG, "stopScanning: ");
        btScanning = false;
        // mBluetoothDeviceAddress ="";
        //bluetoothGatt.close();

 /*       stopScanTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
//                for (int i = 0; i < devicesDiscovered.size(); i++) {
//                    a = devicesDiscovered.get(i);
//                    if (a.getAddress().equals("00:43:40:08:00:0F")) {
//                        Log.e(TAG, "run: 확인해서 들어갔어요 조건문을");
//                        findDeviceFlag = true;
//                        a = devicesDiscovered.get(i);
//                        Log.e(TAG, "a값 확인해요 " + a.getAddress() + a.getName());
//                    } else {
//                        findDeviceFlag = false;
//                        a = null;
//                    }
//                }
//                int index = 0;
//                if (findDeviceFlag) {
//                    index = devicesDiscovered.indexOf(a);
//                    Log.e(TAG, "index 값 뽑아내요 " + index);
//                }
                bluetoothGatt = devicesDiscovered.get(index).connectGatt(getApplicationContext(), false, bluetoothGattCallback);
            }
        });
    }*/
    }

    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            // TODO: 2018-02-12 newState == 0 ==> STATE_DISCONNECTED
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                btScanner.startScan(leScanCallback);
                // TODO: 2018-02-13 타이머
                readCharStop();
                //bluetoothGatt.disconnect();
                Log.e(TAG, "onConnectionStateChange: STATE_DISCONNECTED");
            }
            // TODO: 2018-02-12 newState == 1 ==> STATE_CONNECTING
            if (newState == BluetoothProfile.STATE_CONNECTING) {
                Log.e(TAG, "onConnectionStateChange: STATE_CONNECTING");
            }
            // TODO: 2018-02-12 newState == 2 ==> 연결됨 상태값
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                bluetoothGatt.discoverServices();
                btScanner.stopScan(leScanCallback);
                Log.e(TAG, "onConnectionStateChange: STATE_CONNECTED");
//                timerTaskFlag = true;
//                // TODO: 2018-02-13 연결이되면 서비스를 탐색하고 1초 간격인 타이머를 생성한다.
//                readCharStart(1000);
                //stopScanning();

            }
            if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                Log.e(TAG, "onConnectionStateChange: STATE_DISCONNECTING");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                displayGattServices(bluetoothGatt.getServices());
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
//
//                if (mSCharacteristic == mHexiCharacteristic) {
//                    setCharacteristicNotification(mSCharacteristic, true);
//                    byte[] data = mHexiCharacteristic.getValue();
//                    for (int i = 0; i < data.length; i++) {
//                        Log.e(TAG, "onCharacteristicRead: " + data[i]);
//                    }
//                }
                Log.e(TAG, "onCharacteristicRead: 콜백 발생했어요  ");
                if (mHexiPressureCharacteristic != null) {
                    //setCharacteristicNotification(mSCharacteristic, true);
                    byte[] data = mHexiPressureCharacteristic.getValue();
                    for (int i = 0; i < data.length; i++) {
                        Log.e(TAG, "mHexiPressureCharacteristic: " + data[i]);
                    }
                    broadcastUpdate(Const.CHAR_READ_ACTION, mHexiPressureCharacteristic);
                }

//                if (mSCharacteristic2 == mHexiPressureCharacteristic) {
//                    //setCharacteristicNotification(mSCharacteristic, true);
//                    byte[] data = mHexiPressureCharacteristic.getValue();
//                    for (int i = 0; i < data.length; i++) {
//                        Log.e(TAG, "mHexiPressureCharacteristic: " + data[i]);
//                    }
//                }
                //bluetoothGatt.disconnect();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            String uuid = characteristic.getUuid().toString();

            if (mSCharacteristic == mHexiCharacteristic) {
                //setCharacteristicNotification(mSCharacteristic, true);
                byte[] data = characteristic.getValue();
                for (int i = 0; i < data.length; i++) {
                    Log.e(TAG, "onCharacteristicChanged: " + data[i]);
                }
            }
//            if (mSCharacteristic2 == mHexiPressureCharacteristic) {
//                //setCharacteristicNotification(mSCharacteristic, true);
//                byte[] data = characteristic.getValue();
//                for (int i = 0; i < data.length; i++) {
//                    Log.e(TAG, "mHexiPressureCharacteristic: " + data[i]);
//                }
//            }

//            if (mHexiPressureCharacteristic != null) {
//                readCharacteristic(mHexiPressureCharacteristic);
//                byte[] data = characteristic.getValue();
//                for (int i = 0; i < data.length; i++) {
//                    Log.e(TAG, "mHexiPressureCharacteristic: " + data[i]);
//                }
//            }
            //bluetoothGatt.disconnect();
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }
    };

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        mModelNumberCharacteristic = null;
        mSerialPortCharacteristic = null;
        mCommandCharacteristic = null;
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
        mHexiCharacteristic = null;
        mHexiStartCharacteristic = null;
        mHexiPressureCharacteristic = null;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            String uuid = gattService.getUuid().toString();
            Log.e(TAG, "displayGattServices: " + uuid + "\n");
            //System.out.println("Service discovered: " + uuid);
//            MainActivity.this.runOnUiThread(new Runnable() {
//                public void run() {
//                    peripheralTextView.append("Service disovered: " + uuid + "\n");
//                }
//            });

            //new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {

                String charUuid = gattCharacteristic.getUuid().toString();

                charas.add(gattCharacteristic);
                Log.e(TAG, "gattCharacteristic: " + charUuid + "\n");

                if (charUuid.equals(ModelNumberStringUUID)) {
                    mModelNumberCharacteristic = gattCharacteristic;
                    //System.out.println("mModelNumberCharacteristic  " + mModelNumberCharacteristic.getUuid().toString());
                } else if (charUuid.equals(SerialPortUUID)) {
                    mSerialPortCharacteristic = gattCharacteristic;
                    //System.out.println("mSerialPortCharacteristic  " + mSerialPortCharacteristic.getUuid().toString());
//                    updateConnectionState(R.string.comm_establish);
                } else if (charUuid.equals(CommandUUID)) {
                    mCommandCharacteristic = gattCharacteristic;
                    //System.out.println("mSerialPortCharacteristic  " + mSerialPortCharacteristic.getUuid().toString());
//                    updateConnectionState(R.string.comm_establish);
                } else if (charUuid.equals(HexiStartUUID)) {
                    mHexiStartCharacteristic = gattCharacteristic;
                    Log.e(TAG, "HexiStartCharacteristic  " + mHexiStartCharacteristic.getUuid().toString());
                    //System.out.println("HexiStartCharacteristic  " + mHexiStartCharacteristic.getUuid().toString());
//                    updateConnectionState(R.string.comm_establish);
                } else if (charUuid.equals(UUID_CHAR_PRESSURE)) {
                    mHexiPressureCharacteristic = gattCharacteristic;
                    Log.e(TAG, "displayGattServices: " + "HexiCharacteristic  " + mHexiPressureCharacteristic.getUuid().toString());
                    //System.out.println("HexiCharacteristic  " + mHexiCharacteristic.getUuid().toString());
//                    updateConnectionState(R.string.comm_establish);
                } else if (charUuid.equals(HexiStringUUID)) {
                    mHexiCharacteristic = gattCharacteristic;
                    Log.e(TAG, "displayGattServices: " + "HexiCharacteristic  " + mHexiCharacteristic.getUuid().toString());
                    //System.out.println("HexiCharacteristic  " + mHexiCharacteristic.getUuid().toString());
//                    updateConnectionState(R.string.comm_establish);
                }
            }
            mGattCharacteristics.add(charas);
        }

//        if (mHexiPressureCharacteristic != null){
//            readCharacteristic(mHexiPressureCharacteristic);
//        }

        if (mHexiCharacteristic == null) {
            //Toast.makeText("", "Please select wearable devices", Toast.LENGTH_SHORT).show();

        } else {
            mSCharacteristic = mHexiCharacteristic;
            setCharacteristicNotification(mSCharacteristic, true);
            readCharacteristic(mSCharacteristic);
        }

        if (mHexiPressureCharacteristic == null) {
            //Toast.makeText("", "Please select wearable devices", Toast.LENGTH_SHORT).show();

        } else {
            Log.e(TAG, "displayGattServices: mHexiPressureCharacteristic" + mHexiPressureCharacteristic.getUuid().toString());
            mSCharacteristic2 = mHexiPressureCharacteristic;
            //mBluetoothLeService.setCharacteristicNotification(mSCharacteristic, true);
            Log.e(TAG, "displayGattServices: mSCharacteristic2" + mSCharacteristic2.getUuid().toString());
            readCharacteristic(mSCharacteristic2);
            readCharStart(1000, mSCharacteristic2);
        }
        //System.out.println("Characteristic discovered for service: " + charUuid);
//                MainActivity.this.runOnUiThread(new Runnable() {
//                    public void run() {
//                        peripheralTextView.append("Characteristic discovered for service: " + charUuid + "\n");
//                    }
//                });
    }

    //ReadCharTask readCharTask;
    //TimerTask readTimerTask;

    public class ReadCharTask extends TimerTask {
        public void run() {
            bleReadCharacteristic(mSCharacteristic2);
           /* if (mSCharacteristic2 != null) {
                Log.e(TAG, "Timer run 동작 진행합니다.");
                bleReadCharacteristic(mSCharacteristic2);
            }*/
        }
    }

    public TimerTask timerTaskMaker(final BluetoothGattCharacteristic characteristic) {
        final TimerTask tempTimeTask = new TimerTask() {
            @Override
            public void run() {
                if (characteristic != null) {
                    Log.e(TAG, "timerTaskMaker: mSCharacteristic2" + characteristic.getUuid().toString());
                    bleReadCharacteristic(characteristic);
                }
                //bleReadCharacteristic(mSCharacteristic2);
            }
        };
        return tempTimeTask;
    }

    public void readCharStart(long interval, BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        if (mSCharacteristic2 == null) {
            mSCharacteristic2 = mHexiPressureCharacteristic;
        }
        //myTimer = new Timer();
        readTimerTask = timerTaskMaker(bluetoothGattCharacteristic);
        //ReadCharTask readCharTask = new ReadCharTask();
        myTimer.schedule(readTimerTask, 100, interval);

        //readTimerTask = timerTaskMaker();
//        readTimerTask = new TimerTask() {
//            @Override
//            public void run() {
//                if (mSCharacteristic2 != null) {
//                    Log.e(TAG, "run: mSCharacteristic2" + mSCharacteristic2.getUuid().toString());
//                    bleReadCharacteristic(mSCharacteristic2);
//                } else {
//                    mSCharacteristic2 = mHexiPressureCharacteristic;
//                }
//            }
//        };
        //readCharTask = new ReadCharTask();
//        if (timerTaskFlag){
//            Log.e(TAG, "readCharStart: timerTaskFlag " + "들어왔네요 " );
//            myTimer.schedule(readTimerTask, 200, interval);
//
//        }
        //myTimer.schedule(readCharTask, 200, interval);
    }

    public void readCharStop() {
        // TODO: 2018-02-13 NUll 예외 처리
//        if (myTimer != null) {
//            myTimer.cancel();
//            myTimer.purge();
//        }
        readTimerTask.cancel();
        Log.e(TAG, "readCharStop: scheduledExecutionTime ." + readTimerTask.scheduledExecutionTime());
    }

    public void bleReadCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (characteristic != null) {
            final int charaProp = characteristic.getProperties();
            Log.e(TAG, "bleReadCharacteristic: charaProp : " + charaProp);
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                while (readCharacteristic(characteristic) == false) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "InterruptedException");
                    }
                }
            }
        }
    }


    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy: 서비스 종료요  ");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public boolean connect(final String address) {

        if (btAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // TODO: 2018-02-12  Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && bluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (bluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = btAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        // TODO: 2018-02-13 Gatt연결하는 중요한 코드
        bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
//        synchronized (this) {
//            bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
//        }
        //bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (btAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return true;
        }
        return bluetoothGatt.readCharacteristic(characteristic);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (btAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        //BluetoothGattDescriptor descriptor = characteristic.getDescriptor(characteristic.getUuid());
        //descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        //mBluetoothGatt.writeDescriptor(descriptor);

        // This is specific to Heart Rate Measurement.
//        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
//            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
//                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
//            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//            mBluetoothGatt.writeDescriptor(descriptor);
//        }
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        String uuid = characteristic.getUuid().toString();
        Log.e(TAG, "broadcastUpdate uuid : " + uuid);
        StringBuilder stringBuilder = new StringBuilder();

        if (uuid.equals(UUID_CHAR_PRESSURE)) {
            Log.e(TAG, "UUID_CHAR_GYRO Passed ");
            byte[] value = characteristic.getValue();
            for (int i = 0; i < value.length; i++) {
                stringBuilder.append(value[i] + ",");
            }
            stringBuilder.append("\n");
            intent.putExtra(Const.EXTRA_DATA, stringBuilder.toString());
        }
        sendBroadcast(intent);
    }
}
