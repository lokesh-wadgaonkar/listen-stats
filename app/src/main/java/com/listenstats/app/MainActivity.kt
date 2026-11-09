package com.listenstats.app

import android.content.ComponentName
import android.os.Bundle
import android.service.notification.NotificationListenerService
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.listenstats.app.recorder.ListenerService
import com.listenstats.app.recorder.LiveState
import com.listenstats.app.ui.AppRoot
import com.listenstats.app.ui.ListenStatsTheme
import com.listenstats.app.ui.MainViewModel

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ListenStatsTheme(amoled = vm.amoled, dynamicColor = vm.dynamicColor) {
                AppRoot(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshAccess()
        // If access is granted but the service is not bound (killed by the system), ask for a rebind.
        if (vm.accessGranted && !LiveState.connected.value) {
            try {
                NotificationListenerService.requestRebind(
                    ComponentName(this, ListenerService::class.java)
                )
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
