package com.example

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import com.example.bluetooth.BluetoothPermissions
import com.example.ui.MainViewModel
import com.example.ui.navigation.ResilientNavHost
import com.example.ui.theme.ResilientConnectTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ResilientConnectTheme {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    viewModel.refreshPermissionsAndRadio()
                }

                val enableBluetoothLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    viewModel.refreshPermissionsAndRadio()
                }

                val discoverableLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    viewModel.refreshPermissionsAndRadio()
                }

                LaunchedEffect(Unit) {
                    viewModel.refreshPermissionsAndRadio()
                }

                ResilientNavHost(
                    viewModel = viewModel,
                    onRequestPermissions = {
                        permissionLauncher.launch(BluetoothPermissions.requiredPermissions())
                    },
                    onEnableBluetooth = {
                        if (!BluetoothPermissions.hasConnectPermission(this)) {
                            permissionLauncher.launch(BluetoothPermissions.requiredPermissions())
                        } else {
                            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            enableBluetoothLauncher.launch(intent)
                        }
                    },
                    onMakeDiscoverable = {
                        if (!BluetoothPermissions.hasAllRequiredPermissions(this)) {
                            permissionLauncher.launch(BluetoothPermissions.requiredPermissions())
                        } else {
                            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                            }
                            discoverableLauncher.launch(intent)
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissionsAndRadio()
    }
}
