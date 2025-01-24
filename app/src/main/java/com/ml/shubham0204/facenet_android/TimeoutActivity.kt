package com.ml.shubham0204.facenet_android

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity

abstract class TimeoutActivity : ComponentActivity() {
    companion object {
        const val INACTIVITY_TIMEOUT = BuildConfig.INACTIVITY_TIMEOUT
        const val WARNING_BEFORE_CLOSE = BuildConfig.WARNING_BEFORE_CLOSE
    }
    private val mInactivityHandler = Handler(Looper.getMainLooper())
    private var mStartTime = 0L
    private var mToast: Toast? = null
    public var time_elapsed_ms:Long
        get() {
            if (mStartTime == 0L){
                return 0
            }
            return (System.currentTimeMillis() - mStartTime)
        }
        private set(value){}

    private var mCountdownRunnable: Runnable? = null

    private var mInactiveTimeout_ms: Long = INACTIVITY_TIMEOUT
    private var mWarningClose_ms: Long = WARNING_BEFORE_CLOSE

    fun defaultCloseWarnMsg(timeLeft: Long): String {
        return "即將在${timeLeft}秒後關閉應用"
    }
    private var mWarningMsg: (timeLeft: Long) -> String = ::defaultCloseWarnMsg

    fun defaultOnTimeout(): Unit {
        finish()
    }
    private var mOnTimeout: ()-> Unit = ::defaultOnTimeout
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
        setupDefaultInactivityTimer()

    }


    open fun clearInactivityTimer(){
        mInactivityHandler.removeCallbacksAndMessages(null)
        mCountdownRunnable = null
    }


    open fun setupInactivityTimer(onTimeout:(() -> Unit)?=null,
                                            newinactivity_timeout_ms:Long? = null,
                                            newwarning_before_close_ms: Long? = null,
                                  newWarningMsg:((sec:Long)->String)?=null) {
        mToast?.cancel()
        mInactivityHandler.removeCallbacksAndMessages(null)
        mInactiveTimeout_ms = newinactivity_timeout_ms?:mInactiveTimeout_ms
        mWarningClose_ms = newwarning_before_close_ms?:mWarningClose_ms

        mOnTimeout = onTimeout?:mOnTimeout
        mOnTimeout?.let{
            mCountdownRunnable = object: Runnable {
                override fun run() {
                    if (mStartTime == 0L) mStartTime = System.currentTimeMillis()
                    var timeLeft = (mWarningClose_ms - time_elapsed_ms) / 1000

                    if (timeLeft >= 0) {
                        if (newWarningMsg != null) {
                            mWarningMsg = newWarningMsg
                        }
                        showWarningToast(mWarningMsg(timeLeft))
                        mInactivityHandler.postDelayed(this, 1000)
                    } else {
                        mToast?.cancel()
                        it?.invoke()
                    }
                }
            }
            val first_delay = mInactiveTimeout_ms - mWarningClose_ms
            mCountdownRunnable?.let {
                mStartTime = 0L
                mInactivityHandler.postDelayed(
                    it,
                    first_delay
                )
            }
        }

    }
    override fun onUserInteraction() {
        super.onUserInteraction()
//        inactivityHandler.removeCallbacksAndMessages(null)
        setupInactivityTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        mInactivityHandler.removeCallbacksAndMessages(null)
    }

    fun setupDefaultInactivityTimer(){
        setupInactivityTimer(
            ::defaultOnTimeout,
            newinactivity_timeout_ms = INACTIVITY_TIMEOUT,
            newwarning_before_close_ms = WARNING_BEFORE_CLOSE,
            ::defaultCloseWarnMsg
        )
    }
}