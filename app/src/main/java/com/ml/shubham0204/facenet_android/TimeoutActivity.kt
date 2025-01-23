package com.ml.shubham0204.facenet_android

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity

abstract class TimeoutActivity : ComponentActivity() {
    private val inactivityHandler = Handler(Looper.getMainLooper())
    private val INACTIVITY_TIMEOUT = BuildConfig.INACTIVITY_TIMEOUT
    private val WARNING_BEFORE_CLOSE = BuildConfig.WARNING_BEFORE_CLOSE
    private var startTime = 0L
    private var mToast: Toast? = null
    public var time_elapsed_ms:Long
        get() {
            if (startTime == 0L){
                return 0
            }
            return (System.currentTimeMillis() - startTime)
        }
        private set(value){}

    private var countdownRunnable: Runnable? = null
    private var warningMsg: (timeLeft: Long) -> String = { timeLeft ->
        "即將在${timeLeft}秒後關閉應用"
    }


    protected open fun showWarningToast(text: String) {
        mToast?.cancel()
        mToast = Toast.makeText(
            this@TimeoutActivity,
            text,
            Toast.LENGTH_SHORT
        )
        mToast?.show()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupInactivityTimer(onTimeout={finish()})
    }


    open fun clearInactivityTimer(){
        inactivityHandler.removeCallbacksAndMessages(null)
        countdownRunnable = null
    }
    open fun setupInactivityTimer(onTimeout:(() -> Unit)?=null,
                                            inactivity_timeout_ms:Long = INACTIVITY_TIMEOUT,
                                            warning_before_close_ms: Long = WARNING_BEFORE_CLOSE,
                                  newWarningMsg:((sec:Long)->String)?=null) {
        mToast?.cancel()
        inactivityHandler.removeCallbacksAndMessages(null)
        val first_delay = inactivity_timeout_ms - warning_before_close_ms


        // overwrite onTimeout()
        if (onTimeout != null){
            countdownRunnable = object: Runnable {
                override fun run() {
                    if (startTime == 0L) startTime = System.currentTimeMillis()
                    var timeLeft = (warning_before_close_ms - time_elapsed_ms) / 1000

                    if (timeLeft >= 0) {
                        if (newWarningMsg != null) {
                            warningMsg = newWarningMsg
                        }
                        showWarningToast(warningMsg(timeLeft))
                        inactivityHandler.postDelayed(this, 1000)
                    } else {
                        mToast?.cancel()
                        onTimeout?.invoke()
                    }
                }
            }
        }
        countdownRunnable?.let {
            startTime = 0L
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