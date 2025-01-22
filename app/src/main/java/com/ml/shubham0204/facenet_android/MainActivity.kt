package com.ml.shubham0204.facenet_android

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ml.shubham0204.facenet_android.presentation.screens.add_face.AddFaceScreen
import com.ml.shubham0204.facenet_android.presentation.screens.detect_screen.DetectScreen
import com.ml.shubham0204.facenet_android.presentation.screens.face_list.FaceListScreen
import com.ml.shubham0204.facenet_android.presentation.screens.LoginScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.post {
            setFullScreenMode()
        }

        enableEdgeToEdge()
        val intentStartDestination = intent.getStringExtra("startDestination")
        val intentPersonId = intent.getLongExtra("personID", 0)

        setContent {
            val navHostController = rememberNavController()
            var isAuthenticated by remember { mutableStateOf(intentStartDestination != null) }

            NavHost(
                navController = navHostController,
                startDestination = if (isAuthenticated) {
                    intentStartDestination ?: "detect"
                } else {
                    "login"
                },
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() }
            ) {
                // 登入畫面
                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            isAuthenticated = true
                            //移除登入頁面
                            navHostController.navigate("detect") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }

                // Intent 開啟的新增臉部畫面
                composable(
                    route = "intent-add-face"
                ) {
                    DetectScreen(
                        from_external = intentStartDestination != null,
                        adding_user = true,
                        onNavigateBack = {
                            this@MainActivity.finish()
                        },
                        onOpenFaceListClick = {
                            if (intentPersonId > 0L) {
                                navHostController.navigate("add-face/$intentPersonId")
                            } else {
                                navHostController.navigate("face-list")
                            }
                        }
                    )
                }

                // 其他現有的路由
                composable(
                    route = "add-face/{personID}",
                    arguments = listOf(navArgument("personID") { type = NavType.LongType; defaultValue = 0 })
                ) { backStackEntry ->
                    val personID = backStackEntry.arguments?.getLong("personID") ?: 0
                    AddFaceScreen(personID = personID) { navHostController.navigateUp() }
                }

                composable("detect") {
                    DetectScreen(
                        from_external = intentStartDestination != null,
                        adding_user = false,
                        onNavigateBack = {
                            this@MainActivity.finish()
                        },
                        onOpenFaceListClick = { navHostController.navigate("face-list") }
                    )
                }

                composable("face-list") {
                    FaceListScreen(
                        onNavigateBack = { navHostController.navigateUp() },
                        onAddFaceClick = { navHostController.navigate("add-face/0") },
                        onFaceItemClick = { personRecord ->
                            navHostController.navigate("add-face/${personRecord.personID}")
                        }
                    )
                }
            }
        }
    }

    private fun setFullScreenMode() {
        // 保持原有的全螢幕模式設定
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { insetsController ->
                insetsController.hide(
                    WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
                )
                insetsController.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                            View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    )
        }
    }
}