package com.ml.shubham0204.facenet_android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ml.shubham0204.facenet_android.presentation.screens.add_face.AddFaceScreen
import com.ml.shubham0204.facenet_android.presentation.screens.detect_screen.DetectScreen
import com.ml.shubham0204.facenet_android.presentation.screens.face_list.FaceListScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 取得 Intent 中的目標 Screen
        val intent_startDestination = intent.getStringExtra("startDestination")
        val intent_personID = intent.getLongExtra("personID", 0)

        setContent {
            val navHostController = rememberNavController()
            NavHost(
                navController = navHostController,
                startDestination = intent_startDestination?:"detect",
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() }
            ) {
//                composable("add-face") { AddFaceScreen(0) { navHostController.navigateUp() } }
                composable(
                    route = "add-face/{personID}",
                    arguments = listOf(navArgument("personID") { type = NavType.LongType; defaultValue=0 })
                ) { backStackEntry ->

                    if (intent_startDestination !=null && intent_personID > 0L){
                        AddFaceScreen(personID = intent_personID){ navHostController.navigateUp() }
                    } else {
                        val personID = backStackEntry.arguments?.getLong("personID")?: 0
                        AddFaceScreen(personID = personID){ navHostController.navigateUp() }
                    }

                }
                composable("detect") { DetectScreen(intent_startDestination !=null, add_face = false) { navHostController.navigate("face-list") } }
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
}
