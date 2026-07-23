package com.matth.scaleconnect.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ConnectionState { DISCONNECTED, SCANNING, CONNECTING, DISCOVERING, READY }

data class LogEntry(val timestamp: String, val direction: String, val hex: String)

/**
 * Talks directly to the scale over BLE, replacing the Senssun app's broken sync path.
 * Unlike the original app (which drops any notification under 8 bytes before it can
 * reach the UI - see BroadCast.java in the decompiled APK - this logs every
 * notification regardless of length, since the real device sends short 3-byte status
 * frames the original app silently discards.
 */
class ScaleBleManager(private val context: Context) {

    companion object {
        @Volatile
        private var instance: ScaleBleManager? = null

        fun getInstance(context: Context): ScaleBleManager =
            instance ?: synchronized(this) {
                instance ?: ScaleBleManager(context.applicationContext).also { instance = it }
            }
    }

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? get() = bluetoothManager.adapter

    private var gatt: BluetoothGatt? = null
    private var writeChar: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _deviceName = MutableStateFlow<String?>(null)
    val deviceName: StateFlow<String?> = _deviceName.asStateFlow()

    private val _log = MutableStateFlow<List<LogEntry>>(emptyList())
    val log: StateFlow<List<LogEntry>> = _log.asStateFlow()

    private val _reading = MutableStateFlow(WeighInResult())
    val reading: StateFlow<WeighInResult> = _reading.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startScan() {
        val bleScanner = adapter?.bluetoothLeScanner
        if (bleScanner == null) {
            appendLog("SYS", "Bluetooth unavailable or disabled")
            return
        }
        appendLog("SYS", "Scanning for scale...")
        _connectionState.value = ConnectionState.SCANNING
        _reading.value = WeighInResult()
        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(ScaleProtocol.SERVICE_UUID_PRIMARY))
                .build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        bleScanner.startScan(filters, settings, scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: result.scanRecord?.deviceName ?: return
            adapter?.bluetoothLeScanner?.stopScan(this)
            appendLog("SYS", "Found $name (${device.address})")
            _deviceName.value = name
            connect(device)
        }

        override fun onScanFailed(errorCode: Int) {
            appendLog("SYS", "Scan failed: code=$errorCode")
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    @SuppressLint("MissingPermission")
    private fun connect(device: BluetoothDevice) {
        _connectionState.value = ConnectionState.CONNECTING
        gatt = device.connectGatt(context, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    appendLog("SYS", "Connected, discovering services...")
                    _connectionState.value = ConnectionState.DISCOVERING
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    appendLog("SYS", "Disconnected (status=$status)")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    writeChar = null
                    g.close()
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                appendLog("SYS", "Service discovery failed: status=$status")
                return
            }
            val service = g.getService(ScaleProtocol.SERVICE_UUID_PRIMARY)
                ?: g.getService(ScaleProtocol.SERVICE_UUID_FALLBACK)
            if (service == null) {
                appendLog("SYS", "Scale GATT service not found")
                return
            }

            val notifyChar = service.getCharacteristic(ScaleProtocol.CHAR_NOTIFY_PRIMARY)
                ?: service.getCharacteristic(ScaleProtocol.CHAR_FALLBACK)
            val secondaryNotify = service.getCharacteristic(ScaleProtocol.CHAR_NOTIFY_SECONDARY)
            writeChar = service.getCharacteristic(ScaleProtocol.CHAR_WRITE_PRIMARY)
                ?: service.getCharacteristic(ScaleProtocol.CHAR_FALLBACK)

            notifyChar?.let { enableNotify(g, it) }
            secondaryNotify?.let { enableNotify(g, it) }

            _connectionState.value = ConnectionState.READY
            appendLog("SYS", "Ready. Syncing time/date...")
            sendCommand(ScaleProtocol.timeSyncCommand())
            sendCommand(ScaleProtocol.dateSyncCommand())
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleNotification(characteristic, value)
        }

        // Deprecated overload, still invoked on some OEM Bluetooth stacks below API 33.
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value = characteristic.value ?: return
            handleNotification(characteristic, value)
        }
    }

    private fun handleNotification(characteristic: BluetoothGattCharacteristic, value: ByteArray) {
        appendLog("RX ${shortUuid(characteristic)}", value.toHex())
        val frame = ScaleFrameParser.parse(value) ?: return
        if (frame.channel == ScaleFrameParser.CHANNEL_IDLE) {
            return
        }
        _reading.value = _reading.value.merge(frame)
    }

    @SuppressLint("MissingPermission")
    private fun enableNotify(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        g.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(ScaleProtocol.CCCD_UUID) ?: return
        val value = if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else {
            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.writeDescriptor(descriptor, value)
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = value
            @Suppress("DEPRECATION")
            g.writeDescriptor(descriptor)
        }
    }

    @SuppressLint("MissingPermission")
    fun sendCommand(bytes: ByteArray) {
        val g = gatt ?: return
        val wChar = writeChar ?: return
        appendLog("TX", bytes.toHex())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.writeCharacteristic(wChar, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else {
            @Suppress("DEPRECATION")
            wChar.value = bytes
            @Suppress("DEPRECATION")
            g.writeCharacteristic(wChar)
        }
    }

    fun sendProfileSync(userIndex: Int, isMale: Boolean, age: Int, heightCm: Int) {
        sendCommand(ScaleProtocol.profileSyncCommand(userIndex, isMale, age, heightCm))
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun shortUuid(characteristic: BluetoothGattCharacteristic): String =
        characteristic.uuid.toString().substring(4, 8)

    private fun appendLog(direction: String, text: String) {
        val ts = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        _log.value = _log.value + LogEntry(ts, direction, text)
        // Also written to logcat: it survives process death/UI recomposition, unlike the
        // in-memory StateFlow above, so a capture isn't lost if the app gets killed.
        Log.i("ScaleBle", "$direction  $text")
    }
}

private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }
