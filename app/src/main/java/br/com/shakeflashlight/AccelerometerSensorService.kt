package br.com.shakeflashlight

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock.elapsedRealtime
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlin.math.sqrt


class AccelerometerSensorService: Service(),SensorEventListener {

    lateinit  var sensorManager: SensorManager
    private val sensorType = Sensor.TYPE_ACCELEROMETER
    lateinit var accelerometerSensor: Sensor
    var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f
    private var lightStatus = false

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val channel = NotificationChannel(channelId,
            channelName, NotificationManager.IMPORTANCE_NONE)
        channel.lightColor = Color.WHITE
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
        return channelId
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if (sensorManager.getDefaultSensor(sensorType) != null) {
            accelerometerSensor = sensorManager.getDefaultSensor(sensorType)
        }else {
            Toast.makeText(this, "Houve um erro ao encontrar sensor!", Toast.LENGTH_LONG).show()
        }
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 1, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE)
            }

        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel("AccelerometerSensorService", "AccelerometerSensorService background service")
            } else {
                ""
            }
        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("Shake Flashlight")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this);
    }

    override fun onBind(p0: Intent?): IBinder? {
       return null;
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        lastAcceleration = currentAcceleration
        currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta: Float = currentAcceleration - lastAcceleration
        acceleration = acceleration * 0.9f + delta
        if (acceleration > 16) {
            lightStatus = !lightStatus
            sendBroadcast(Intent("accelerometerStatus").putExtra("status",lightStatus))
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)

        val restartServicePendingIntent = PendingIntent.getService(
            applicationContext, 1, restartServiceIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val alarmService = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmService[AlarmManager.ELAPSED_REALTIME, elapsedRealtime() + 500] =
            restartServicePendingIntent

        super.onTaskRemoved(rootIntent)
    }
}