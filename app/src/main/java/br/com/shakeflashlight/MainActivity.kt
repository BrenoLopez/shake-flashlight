package br.com.shakeflashlight

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    MainScreen()
                }
        }
    }

}
@Composable
fun MainScreen(lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current) {
    val context = LocalContext.current

     var cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
     lateinit var cameraId: String

     val (status,setStatus) = remember {
        mutableStateOf(false);
    }
    try {
        cameraId = cameraManager.cameraIdList[0]
    } catch (e: CameraAccessException) {
        Snackbar(backgroundColor = MaterialTheme.colors.error) {
               Text(text = "${e.reason}")
        }
    }
     fun switchLight(status:Boolean){
         setStatus(status)
         cameraManager.setTorchMode(cameraId,status)
    }

    SystemBroadcastReceiver("accelerometerStatus") { onSystemEvent ->
       switchLight(onSystemEvent!!.getBooleanExtra("status",false))
    }
    DisposableEffect(Unit) {
        val intent = Intent(context,AccelerometerSensorService::class.java)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                }
                else
                    context.startService(intent)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            context.stopService(intent)
        }
    }

    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick ={
          switchLight(!status)
        }) {
            Text(text = if (!status)  "LIGAR" else "DESLIGAR")
        }
    }

}

@Composable
fun SystemBroadcastReceiver(
    systemAction: String,
    onSystemEvent: (intent: Intent?) -> Unit
) {
    val context = LocalContext.current
    val currentOnSystemEvent by rememberUpdatedState(onSystemEvent)
    DisposableEffect(context, systemAction) {
        val intentFilter = IntentFilter(systemAction)
        val broadcast = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                currentOnSystemEvent(intent)
            }
        }
        context.registerReceiver(broadcast, intentFilter)
        onDispose {
            context.unregisterReceiver(broadcast)
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun DefaultPreview() {
//        Greeting()
//}