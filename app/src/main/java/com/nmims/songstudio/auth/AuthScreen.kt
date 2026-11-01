package com.nmims.songstudio.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowInsetsControllerCompat
import android.app.Activity
import com.nmims.songstudio.ui.LoginBackdrop
import com.nmims.songstudio.ui.LoginHeader
import com.nmims.songstudio.ui.LoginGlassPanel
import com.nmims.songstudio.ui.LoginCollectionHint
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AuthScreen(onLogin: (Student) -> Unit) {
    val context = LocalContext.current
    val store = remember { AccountStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var signup by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf(store.rememberedEmail()) }
    // Passwords intentionally never enter saved instance state.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    DisposableEffect(store) { onDispose { store.close() } }
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (context as? Activity)?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, view) }
        val oldStatus = controller?.isAppearanceLightStatusBars ?: true
        val oldNavigation = controller?.isAppearanceLightNavigationBars ?: true
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            controller?.isAppearanceLightStatusBars = oldStatus
            controller?.isAppearanceLightNavigationBars = oldNavigation
        }
    }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White.copy(alpha = .35f),
        unfocusedContainerColor = Color.White.copy(alpha = .18f)
    )
    LoginBackdrop {
        Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LoginHeader(signup)
            LoginGlassPanel {
                Text(if (signup) "Create your account" else "Welcome back", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(selected = !signup, enabled = !busy, onClick = { signup = false; error = ""; password = ""; confirmation = "" }, label = { Text("Log in") })
                    FilterChip(selected = signup, enabled = !busy, onClick = { signup = true; error = ""; message = ""; password = ""; confirmation = "" }, label = { Text("Sign up") })
                }
                if (signup) OutlinedTextField(name, { name = it }, label = { Text("Full name") }, singleLine = true, enabled = !busy, shape = RoundedCornerShape(15.dp), colors = fieldColors, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(email, { email = it }, label = { Text("Email address") }, singleLine = true, enabled = !busy, shape = RoundedCornerShape(15.dp), colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, enabled = !busy, shape = RoundedCornerShape(15.dp), colors = fieldColors,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false, capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None),
                    trailingIcon = { TextButton(onClick = { visible = !visible }) { Text(if (visible) "Hide" else "Show") } }, modifier = Modifier.fillMaxWidth())
                if (signup) OutlinedTextField(confirmation, { confirmation = it }, label = { Text("Confirm password") }, singleLine = true, enabled = !busy, shape = RoundedCornerShape(15.dp), colors = fieldColors,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false, capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None), modifier = Modifier.fillMaxWidth())
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
                if (message.isNotEmpty()) Text(message)
                Button(enabled = !busy && email.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), onClick = {
                    error = ""; message = ""
                    if (signup && password != confirmation) { error = "Passwords do not match." }
                    else {
                        busy = true
                        scope.launch {
                            try {
                                if (signup) {
                                    withContext(Dispatchers.IO) { store.signUp(name, email, password) }
                                    signup = false; email = store.rememberedEmail(); password = ""; confirmation = ""
                                    message = "Account created. Log in to continue."
                                } else {
                                    val student = withContext(Dispatchers.IO) { store.login(email, password) }
                                    password = ""; onLogin(student)
                                }
                            } catch (e: IllegalArgumentException) { error = e.message ?: "Please check your details." }
                            catch (_: Exception) { error = "Could not access your account. Please try again." }
                            finally { busy = false }
                        }
                    }
                }) { Text(if (busy) "Please wait…" else if (signup) "Create account" else "Log in →") }
            }
            LoginCollectionHint()
            Text("Made for NMIMS students · Calculus & Chemistry", color = Color(0xFFD1DFCD),
                style = MaterialTheme.typography.bodySmall)
            Text("Your account stays on this device. Sign in with your password when you return. College portal login is not connected.",
                color = Color(0xFFB9CBB6), style = MaterialTheme.typography.bodySmall)

        }
    }
}
