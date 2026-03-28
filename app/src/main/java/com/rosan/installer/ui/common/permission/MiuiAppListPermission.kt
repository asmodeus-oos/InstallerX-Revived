// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.common.permission

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import org.koin.compose.koinInject

private const val MIUI_APP_LIST_PERMISSION = "com.android.permission.GET_INSTALLED_APPS"
private const val MIUI_SECURITY_CENTER = "com.lbe.security.miui"

// Cache the specific capability check result to prevent repeated IPC calls.
private var isMiuiPermissionCapabilitySupportedCache: Boolean? = null

/**
 * Check if the current MIUI system supports dynamic request for GET_INSTALLED_APPS.
 * Integrates fast-path checks for non-Xiaomi devices and caches the result.
 */
private fun isMiuiGetInstalledAppsSupported(
    context: Context,
    deviceCapability: DeviceCapabilityProvider
): Boolean {
    // Fast-path: Skip expensive checks if it's not a Xiaomi device.
    if (!deviceCapability.isMIUI && !deviceCapability.isHyperOS) {
        return false
    }

    // Return the cached result immediately if available.
    isMiuiPermissionCapabilitySupportedCache?.let { return it }

    // Execute the actual check only once using application context to prevent memory leaks.
    isMiuiPermissionCapabilitySupportedCache = try {
        val permissionInfo = context.applicationContext.packageManager.getPermissionInfo(MIUI_APP_LIST_PERMISSION, 0)
        permissionInfo.packageName == MIUI_SECURITY_CENTER
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    return isMiuiPermissionCapabilitySupportedCache!!
}

/**
 * Check if the GET_INSTALLED_APPS permission is already granted.
 */
private fun hasMiuiGetInstalledAppsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        MIUI_APP_LIST_PERMISSION
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * A reusable Compose hook to handle MIUI specific app list permissions.
 * Integrates with DeviceCapabilityProvider to optimize performance on non-Xiaomi devices.
 *
 * @param onGranted Callback executed when permission is granted or not needed.
 * @param onDenied Callback executed when permission is explicitly denied by the user.
 * @return A lambda function that triggers the permission check and request flow.
 */
@Composable
fun rememberMiuiAppListPermission(
    onGranted: () -> Unit,
    onDenied: () -> Unit = {}
): () -> Unit {
    val context = LocalContext.current
    // Inject the DeviceCapabilityProvider using Koin.
    val deviceCapability = koinInject<DeviceCapabilityProvider>()

    // Register the permission launcher once for this composable lifecycle.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onGranted()
        } else {
            onDenied()
        }
    }

    // Return the execution lambda. Note that deviceCapability is added to the keys.
    return remember(context, deviceCapability, onGranted, onDenied) {
        {
            if (isMiuiGetInstalledAppsSupported(context, deviceCapability) && !hasMiuiGetInstalledAppsPermission(context)) {
                // Request the MIUI specific permission.
                permissionLauncher.launch(MIUI_APP_LIST_PERMISSION)
            } else {
                // Not MIUI, old MIUI without the capability, or already granted. Execute directly.
                onGranted()
            }
        }
    }
}
