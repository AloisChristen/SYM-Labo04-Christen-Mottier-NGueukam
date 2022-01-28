package ch.heigvd.iict.sym_labo4

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import ch.heigvd.iict.sym_labo4.gl.OpenGLRenderer


/**
 * Project: Labo4
 * Created by fabien.dutoit on 21.11.2016
 * Updated by fabien.dutoit on 06.11.2020
 * (C) 2016 - HEIG-VD, IICT
 */
class CompassActivity : AppCompatActivity(), SensorEventListener {

    //opengl
    private lateinit var opglr: OpenGLRenderer
    private lateinit var m3DView: GLSurfaceView
    private lateinit var rotMatrix: FloatArray
    private lateinit var gravity : FloatArray
    private lateinit var geomagnetic: FloatArray
    private lateinit var accelerometerSensor: Sensor
    private lateinit var geomagneticSensor: Sensor
    private lateinit var sensorManager: SensorManager



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // we need fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // we initiate the view
        setContentView(R.layout.activity_compass)

        //we create the renderer
        opglr = OpenGLRenderer(applicationContext)

        // link to GUI
        m3DView = findViewById(R.id.compass_opengl)

        //init opengl surface view
        m3DView.setRenderer(opglr)

        //Register sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        geomagneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        gravity = floatArrayOf(
            1f, 0f, 0f
        )
        geomagnetic = floatArrayOf(
            1f, 0f, 0f
        )
        rotMatrix = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, geomagneticSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this, accelerometerSensor)
        sensorManager.unregisterListener(this, geomagneticSensor)
    }

    override fun onSensorChanged(event: SensorEvent) {

        if(event.sensor.type == Sensor.TYPE_ACCELEROMETER)
            gravity = event.values

        if(event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD)
            geomagnetic = event.values

        SensorManager.getRotationMatrix(rotMatrix, null, gravity, geomagnetic)

        opglr.swapRotMatrix(rotMatrix)
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }


}


    /*
        TODO
        your activity need to register to accelerometer and magnetometer sensors' updates
        then you may want to call
        opglr.swapRotMatrix()
        with the 4x4 rotation matrix, every time a new matrix is computed
        more information on rotation matrix can be found online:
        https://developer.android.com/reference/android/hardware/SensorManager.html#getRotationMatrix(float[],%20float[],%20float[],%20float[])
    */
