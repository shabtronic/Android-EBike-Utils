package com.example.speedgrad

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.speedandgradient.R
import com.example.speedgrad.ui.theme.SpeedAndGradientTheme
import java.util.Timer
import kotlin.math.abs
import kotlin.math.tan

var DisplayString = mutableStateOf("")
var startAngle = 10.0f
var floatOrientationAnglesDisplay = FloatArray(3) { 0.0f }

class MainActivity : ComponentActivity() {
private val isPipSupported by lazy {
packageManager.hasSystemFeature(
    PackageManager.FEATURE_PICTURE_IN_PICTURE
)

}

    private lateinit var mSensorManager: SensorManager
    private var mGyroscope: Sensor? = null
    private var sensorAccelerometer: Sensor? = null
    private var sensorMagneticField: Sensor? = null
    private var resume = true;
    var time = 0
    var timer: Timer = Timer()

    var floatGravity = FloatArray(3)
    var floatGeoMagnetic = FloatArray(3)
    var floatOrientationAngles = FloatArray(3)

    var floatRotationMatrix = FloatArray(9)
    var floatInclinationMatrix = FloatArray(9)

    var SENSITIVITY = 0.01; // lower this number higher will be the sensitivity
    private var videoViewBounds = Rect()
    class MyReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            println("Clicked on PIP action")
        }
    }
    fun UpdateValues() {
        SensorManager.getRotationMatrix(
            floatRotationMatrix,
            floatInclinationMatrix,
            floatGravity,
            floatGeoMagnetic
        );
        SensorManager.getOrientation(floatRotationMatrix, floatOrientationAngles);
        if (abs(floatOrientationAnglesDisplay[1] - floatOrientationAngles[1]) > SENSITIVITY) {
            floatOrientationAnglesDisplay[1] = floatOrientationAngles[1];
            floatOrientationAnglesDisplay[1] =
                ((floatOrientationAnglesDisplay[1] * 180.0f / Math.PI) * -1).toFloat();
            floatOrientationAnglesDisplay[1] =
                ((Math.round(floatOrientationAnglesDisplay[1] * 100)) / 100).toFloat();
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {


        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // val sensorEvent = SensorEvent()
        mSensorManager.registerListener(object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

            override fun onSensorChanged(event: SensorEvent?) {
                floatGravity = event!!.values
                UpdateValues();
                DisplayString.value =
                    "Grad: %.0f%%".format((tan((floatOrientationAnglesDisplay[1] - startAngle).toDouble() * Math.PI / 180) * 100))
            }
        }, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        mSensorManager.registerListener(object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

            }

            override fun onSensorChanged(event: SensorEvent?) {
                floatGeoMagnetic = event!!.values
                UpdateValues();
                DisplayString.value =
                    "Grad: %.0f%%".format((tan((floatOrientationAnglesDisplay[1] - startAngle).toDouble() * Math.PI / 180) * 100))
            }
        }, sensorMagneticField, SensorManager.SENSOR_DELAY_NORMAL)

        setTheme(R.style.Theme_Transparent);
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {

                SpeedAndGradientTheme {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(0.dp), containerColor = Color(0x00ff0000),
                        content = { innerPadding ->
                            DrawRow(
                                "",
                                modifier = Modifier
                                    .padding(innerPadding)
                                    .fillMaxSize(),
                            )
                        })
                }

        }
    }
    private fun updatedPipParams(): PictureInPictureParams? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PictureInPictureParams.Builder()
                //.setSourceRectHint(videoViewBounds)
                //.setAspectRatio(Rational(16, 7))
                .setActions(
                    listOf(
                        RemoteAction(
                            Icon.createWithResource(
                                applicationContext,
                                R.mipmap.logo_round
                            ),
                            "",
                            "",
                            PendingIntent.getBroadcast(
                                applicationContext,
                                0,
                                Intent(applicationContext, MyReceiver::class.java),
                                PendingIntent.FLAG_IMMUTABLE
                            )
                        )
                    )
                )
                .build()
        } else null
    }
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if(!isPipSupported) {
            return
        }
        updatedPipParams()?.let { params ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                enterPictureInPictureMode(params)
            }
        }
    }
}
        @Composable
        fun DrawRow(name: String, modifier: Modifier = Modifier) {
            val myText by DisplayString
Column {
    Row(modifier = Modifier.fillMaxWidth() , horizontalArrangement = Arrangement.Center ,verticalAlignment = Alignment.CenterVertically)
    {
        Text(
            text = myText, textAlign = TextAlign.Center, modifier = Modifier
                .padding(0.dp),
            fontSize = 38.sp, color = Color.White
        )
    }
    Row(modifier = Modifier)
    {
        Text(text = "", textAlign = TextAlign.Center, modifier = Modifier.padding(0.dp),
            fontSize = 30.sp, color = Color.White     )
    }
    Row(modifier = Modifier)
    {
        Text(text = "", textAlign = TextAlign.Center, modifier = Modifier .padding(0.dp),
            fontSize = 30.sp, color = Color.White      )
    }
    Row(modifier = Modifier.fillMaxWidth())
    {
        Text(text = "", textAlign = TextAlign.Center, modifier = Modifier .padding(0.dp),
            fontSize = 30.sp, color = Color.White      )
    }
    Row(modifier = Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.Center) {
        Button(
            onClick = {
                //UpdateValues();
                startAngle = floatOrientationAnglesDisplay[1];
                DisplayString.value =
                    "Grad %.0f%%".format((tan((floatOrientationAnglesDisplay[1] - startAngle).toDouble() * Math.PI / 180) * 100))
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xff505050)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(4.dp)
        ) { Text("Reset", fontSize = 40.sp, color = Color.White) }
    }
    Row(modifier = Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.Center)
    {
        Text(text = "\n\n\n\nPress home to minimize into picture-in-picture mode\n\n\n", textAlign = TextAlign.Center, modifier = Modifier .padding(0.dp),
            fontSize = 30.sp, color = Color.White      )
    }
    Row(modifier = Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.Center)
    {
        Image(
            painter = painterResource(id = R.mipmap.logoxx),
            contentDescription = "Logo",
            contentScale = ContentScale.FillWidth,
        )
    }
    }
}