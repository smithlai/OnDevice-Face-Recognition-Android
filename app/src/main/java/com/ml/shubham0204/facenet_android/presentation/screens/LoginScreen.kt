package com.ml.shubham0204.facenet_android.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(2000) // 模擬後端延遲
            onLoginSuccess()
        }
    }

    val validateAndLogin = {

        if (password == "1111") {
            isLoading = true
        } else {
            showError = true
        }
    }


    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = isLoading) { isLoading ->
            if (isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Logging in...", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "USIMSFace Admin",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(bottom = 24.dp, top = 48.dp)
                    )
                    if (showError) {
                        Text(
                            text = "Invalid password",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
//                    Text(
//                        text = "Enter Password",
//                        style = MaterialTheme.typography.headlineMedium,
//                        modifier = Modifier.padding(bottom = 24.dp)
//                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                showError = false
                            },
                            label = { Text("Password") },
                            placeholder = { Text("Enter Password")},
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { validateAndLogin() }
                            ),
                            isError = showError,
                            singleLine = true,
                            modifier = Modifier.weight(1f) // 讓輸入框填滿剩餘空間
                        )
                        Button(
                            onClick = { validateAndLogin() },
                            enabled = !isLoading,
                            modifier = Modifier.width(100.dp)
                        ) {
                            Text("Login")
                        }
                    }

                }
            }
        }
    }
}

