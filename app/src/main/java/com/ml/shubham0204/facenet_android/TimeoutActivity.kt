package com.ml.shubham0204.facenet_android

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity

abstract class TimeoutActivity : ComponentActivity() {
    private val inactivityHandler = Handler(Looper.getMainLooper())
    private val INACTIVITY_TIMEOUT = 2 * 60 * 1000L
    private val WARNING_BEFORE_CLOSE = 1 * 60 * 1000L
    private var startTime = 0L
    private var TICK_SEC = 10L
    public var time_elapsed_ms:Long
        get() {
            if (startTime == 0L){
                return 0
            }
            return (System.currentTimeMillis() - startTime)
        }
        private set(value){}

    private var countdownRunnable: Runnable? = null
//    private val countdownRunnable = object : Runnable {
//        override fun run() {
//            if ( 0L == startTime)
//                startTime = System.currentTimeMillis()
//            val elapsed = System.currentTimeMillis() - startTime
//            var time_left = (WARNING_BEFORE_CLOSE - elapsed)/1000
//            time_left = (Math.round((time_left / tick_sec.toDouble())) * 10) //以10為單位
//            if (time_left > 0) {
//                showWarningToast(time_left)
////                remainingSeconds--
//                inactivityHandler.postDelayed(this, tick_sec*1_000)
//            } else {
//                finish()
//            }
//        }
//    }

    protected open fun showWarningToast(timeLeft: Long) {
        Toast.makeText(
            this@TimeoutActivity,
            "即將在${timeLeft}秒後關閉應用",
            Toast.LENGTH_SHORT
        ).show()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupInactivityTimer(onTimeout={finish()})
    }

//    private fun setupInactivityTimer() {
//        startTime = 0L
//        inactivityHandler.postDelayed(
//            countdownRunnable,
//            INACTIVITY_TIMEOUT - WARNING_BEFORE_CLOSE
//        )
//    }
    open fun setupInactivityTimer(onTimeout:(() -> Unit)?=null,
                                            inactivity_timeout_ms:Long = INACTIVITY_TIMEOUT,
                                            warning_before_close_ms: Long = WARNING_BEFORE_CLOSE) {
        inactivityHandler.removeCallbacksAndMessages(null)
        val first_delay = inactivity_timeout_ms - warning_before_close_ms


        // overwrite onTimeout()
        if (onTimeout != null){
            countdownRunnable = object: Runnable {
                override fun run() {
//                    if (startTime == 0L) startTime = System.currentTimeMillis()
                    var timeLeft = (warning_before_close_ms - time_elapsed_ms) / 1000
                    timeLeft = (Math.round((timeLeft / TICK_SEC.toDouble())) * 10) // in units of 10

                    if (timeLeft > 0) {
                        showWarningToast(timeLeft)
                        inactivityHandler.postDelayed(this, TICK_SEC * 1000)
                    } else {
                        onTimeout?.invoke()
                    }
                }
            }
        }
        countdownRunnable?.let {
            startTime = System.currentTimeMillis() + first_delay
            Log.e("aaaaa", "Delay ${inactivity_timeout_ms - warning_before_close_ms}")
            inactivityHandler.postDelayed(
                it,
                first_delay
            )
        }
    }
    override fun onUserInteraction() {
        super.onUserInteraction()
//        inactivityHandler.removeCallbacksAndMessages(null)
        setupInactivityTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        inactivityHandler.removeCallbacksAndMessages(null)
    }
}