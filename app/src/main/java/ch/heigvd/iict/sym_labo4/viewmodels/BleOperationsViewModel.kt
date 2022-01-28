package ch.heigvd.iict.sym_labo4.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.FORMAT_SINT16
import android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT16
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.observer.ConnectionObserver
import java.math.BigInteger
import java.time.Instant
import java.time.Instant.now
import java.util.*
import java.time.format.DateTimeFormatter as DateTimeFormatter1

/**
 * Project: Labo4
 * Created by fabien.dutoit on 11.05.2019
 * Updated by fabien.dutoit on 18.10.2021
 * (C) 2019 - HEIG-VD, IICT
 */
class BleOperationsViewModel(application: Application) : AndroidViewModel(application) {

    private var ble = SYMBleManager(application.applicationContext)
    private var mConnection: BluetoothGatt? = null

    //live data - observer
    val isConnected = MutableLiveData(false)
    var temperature = MutableLiveData(0)
    var currentTime = MutableLiveData("")
    var integer = MutableLiveData(0)
    var btnClicked = MutableLiveData(0)

    //Services and Characteristics of the SYM Pixl
    private var timeService: BluetoothGattService? = null
    private var symService: BluetoothGattService? = null
    private var currentTimeChar: BluetoothGattCharacteristic? = null
    private var integerChar: BluetoothGattCharacteristic? = null
    private var temperatureChar: BluetoothGattCharacteristic? = null
    private var buttonClickChar: BluetoothGattCharacteristic? = null

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared")
        ble.disconnect()
    }

    fun connect(device: BluetoothDevice) {
        Log.d(TAG, "User request connection to: $device")
        if (!isConnected.value!!) {
            ble.connect(device)
                    .retry(1, 100)
                    .useAutoConnect(false)
                    .enqueue()
        }
    }

    fun disconnect() {
        Log.d(TAG, "User request disconnection")
        ble.disconnect()
        mConnection?.disconnect()
    }

    /* TODO
        vous pouvez placer ici les différentes méthodes permettant à l'utilisateur
        d'interagir avec le périphérique depuis l'activité
     */

    fun readTemperature(): Boolean {
        if (!isConnected.value!! || temperatureChar == null)
            return false
        else
            return ble.readTemperature()
    }

    fun writeInteger(value: BigInteger): Boolean {
        return if (!isConnected.value!! || integerChar == null)
            false
        else
            ble.writeInteger(value)
    }

    fun writeTime(value: String): Boolean {
        return if (!isConnected.value!! || currentTimeChar == null)
            false
        else
            ble.writeTime(value)
    }

    private val bleConnectionObserver: ConnectionObserver = object : ConnectionObserver {
        override fun onDeviceConnecting(device: BluetoothDevice) {
            Log.d(TAG, "onDeviceConnecting")
            isConnected.value = false
        }

        override fun onDeviceConnected(device: BluetoothDevice) {
            Log.d(TAG, "onDeviceConnected")
            isConnected.value = true
        }

        override fun onDeviceDisconnecting(device: BluetoothDevice) {
            Log.d(TAG, "onDeviceDisconnecting")
            isConnected.value = false
        }

        override fun onDeviceReady(device: BluetoothDevice) {
            Log.d(TAG, "onDeviceReady")
        }

        override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
            Log.d(TAG, "onDeviceFailedToConnect")
        }

        override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
            if(reason == ConnectionObserver.REASON_NOT_SUPPORTED) {
                Log.d(TAG, "onDeviceDisconnected - not supported")
                Toast.makeText(getApplication(), "Device not supported - implement method isRequiredServiceSupported()", Toast.LENGTH_LONG).show()
            }
            else
                Log.d(TAG, "onDeviceDisconnected")
            isConnected.value = false
        }

    }

    private inner class SYMBleManager(applicationContext: Context) : BleManager(applicationContext) {
        /**
         * BluetoothGatt callbacks object.
         */
        private var mGattCallback: BleManagerGattCallback? = null

        public override fun getGattCallback(): BleManagerGattCallback {
            //we initiate the mGattCallback on first call, singleton
            if (mGattCallback == null) {
                mGattCallback = object : BleManagerGattCallback() {

                    public override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
                        mConnection = gatt //trick to force disconnection

                        Log.d(TAG, "isRequiredServiceSupported - TODO")

                        symService = gatt.getService(UUID.fromString("3c0a1000-281d-4b48-b2a7-f15579a1c38f"))
                        timeService = gatt.getService(UUID.fromString("00001805-0000-1000-8000-00805f9b34fb"))

                        if (symService == null || timeService == null) return false
                        integerChar = symService!!.getCharacteristic(UUID.fromString("3c0a1001-281d-4b48-b2a7-f15579a1c38f"))
                        temperatureChar = symService!!.getCharacteristic(UUID.fromString("3c0a1002-281d-4b48-b2a7-f15579a1c38f"))
                        buttonClickChar = symService!!.getCharacteristic(UUID.fromString("3c0a1003-281d-4b48-b2a7-f15579a1c38f"))
                        currentTimeChar = timeService!!.getCharacteristic(UUID.fromString("00002A2B-0000-1000-8000-00805f9b34fb"))

                        if(integerChar == null || temperatureChar == null || buttonClickChar == null || currentTimeChar == null) return false

                        return true
                        /* TODO
                        - Nous devons vérifier ici que le périphérique auquel on vient de se connecter possède
                          bien tous les services et les caractéristiques attendues, on vérifiera aussi que les
                          caractéristiques présentent bien les opérations attendues
                        - On en profitera aussi pour garder les références vers les différents services et
                          caractéristiques (déclarés en lignes 39 à 44)
                        */
                    }

                    override fun initialize() {
                        /*  TODO
                            Ici nous somme sûr que le périphérique possède bien tous les services et caractéristiques
                            attendus et que nous y sommes connectés. Nous pouvous effectuer les premiers échanges BLE:
                            Dans notre cas il s'agit de s'enregistrer pour recevoir les notifications proposées par certaines
                            caractéristiques, on en profitera aussi pour mettre en place les callbacks correspondants.
                         */
                        ble.setNotificationCallback(buttonClickChar).with {_, data ->
                            btnClicked.value = data.getIntValue(Data.FORMAT_UINT8, 0)
                        }
                        ble.enableNotifications(buttonClickChar).enqueue()

                        ble.setNotificationCallback(currentTimeChar).with {_, data ->
                            println("Notif from current Time, value : ${data.value}")

                            currentTime.value = data.getByte(255).toString()
                        }
                        ble.enableNotifications(currentTimeChar).enqueue()
                    }

                    override fun onServicesInvalidated() {
                        //we reset services and characteristics
                        timeService = null
                        currentTimeChar = null
                        symService = null
                        integerChar = null
                        temperatureChar = null
                        buttonClickChar = null
                    }
                }
            }
            return mGattCallback!!
        }

        fun readTemperature(): Boolean {
            ble.readCharacteristic(temperatureChar).with {_, data ->
                temperature.value = (data.getIntValue(Data.FORMAT_UINT16, 0)?.div(10))
            }.enqueue()

            return true
        }

        fun writeInteger(value: BigInteger): Boolean {
            println("Send $value to pixj")
            ble.writeCharacteristic(integerChar, Data(value.toByteArray()),BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE).enqueue()
            return true
        }

        fun writeTime(value: String): Boolean {
            ble.writeCharacteristic(currentTimeChar, Data(value.toByteArray()), BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE).enqueue()
            return true
        }
    }

    companion object {
        private val TAG = BleOperationsViewModel::class.java.simpleName
        private val Calendar = java.util.Calendar::class
    }

    init {
        ble.setConnectionObserver(bleConnectionObserver)
    }

}