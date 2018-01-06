/*
 * Copyright (c) 2018 Fondesa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fondesa.kpermissions.request.runtime

import android.app.Fragment
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.content.ContextCompat
import android.util.Log
import com.fondesa.kpermissions.controller.*
import com.fondesa.kpermissions.extensions.flatString
import com.fondesa.kpermissions.extensions.isPermissionGranted

/**
 * Created by antoniolig on 05/01/18.
 */
@RequiresApi(Build.VERSION_CODES.M)
class FragmentRuntimePermissionHandler : Fragment(), RuntimePermissionHandler {

    private val listeners = mutableMapOf<String, RuntimePermissionHandler.Listener>()
    private val controllers = mutableMapOf<String, PermissionLifecycleController>()

    private var isProcessingPermissions = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retain the instance of the Fragment.
        retainInstance = true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQ_CODE_PERMISSIONS || permissions.isEmpty()) {
            // Ignore the result if the request code doesn't match or
            // avoid the computation if there aren't processed permissions.
            return
        }

        val context = activity ?: throw NullPointerException("The activity mustn't be null.")

        // Now the Fragment is not processing the permissions anymore.
        isProcessingPermissions = false

        // Get the listener for this group of permissions.
        val listener = listenerOf(permissions)
        // Get the controller for this group of permissions.
        val controller = controllerOf(permissions)

        val acceptedList = mutableListOf<String>()
        val permDeniedList = mutableListOf<String>()
        val rationaleList = mutableListOf<String>()

        permissions.forEach {
            when {
                context.isPermissionGranted(it) -> acceptedList.add(it)
                shouldShowRequestPermissionRationale(it) -> rationaleList.add(it)
                else -> permDeniedList.add(it)
            }
        }

        val acceptedPermissions = acceptedList.toTypedArray()
        val deniedPermissions = permDeniedList.toTypedArray()
        val rationalePermissions = rationaleList.toTypedArray()

        val acceptedDelivering = controller.acceptedDelivering()
        val deniedDelivering = controller.permanentlyDeniedDelivering()
        val rationaleDelivering = controller.rationaleDelivering()

        val notifyAccepted = (acceptedDelivering == Delivering.AT_LEAST_ONE && acceptedPermissions.isNotEmpty()) ||
                (acceptedDelivering == Delivering.ALL && acceptedPermissions.size == permissions.size)

        if (notifyAccepted) {
            // Some permissions are accepted.
            listener.permissionsAccepted(acceptedPermissions)
        }

        val notifyDenied = (deniedDelivering == Delivering.AT_LEAST_ONE && deniedPermissions.isNotEmpty()) ||
                (deniedDelivering == Delivering.ALL && deniedPermissions.size == permissions.size)

        val notifyRationale = controller.rationaleCheck() != RationaleCheck.BEFORE &&
                (rationaleDelivering == Delivering.AT_LEAST_ONE && rationalePermissions.isNotEmpty()) ||
                (rationaleDelivering == Delivering.ALL && rationalePermissions.size == permissions.size)

        val execDenied = {
            // Some permissions are permanently denied by the user.
            listener.permissionsPermanentlyDenied(deniedPermissions)
        }
        val execRationale = {
            // Show rationale of permissions.
            listener.permissionsShouldShowRationale(rationalePermissions)
        }

        if (notifyDenied && notifyRationale) {
            val execAlways = controller.notAcceptedSecondaryExecution() == Execution.ALWAYS
            val priority = controller.notAcceptedPriority()
            if (priority == Priority.RATIONALE) {
                if (!execRationale() || execAlways) {
                    execDenied()
                }
            } else if (priority == Priority.DENIED) {
                if (!execDenied() || execAlways) {
                    execRationale()
                }
            }
        } else if (notifyDenied) {
            execDenied()
        } else if (notifyRationale) {
            execRationale()
        }
    }

    override fun attachListener(permissions: Array<out String>, listener: RuntimePermissionHandler.Listener) {
        val key = keyOf(permissions)
        listeners[key] = listener
    }

    override fun attachLifecycleController(permissions: Array<out String>, controller: PermissionLifecycleController) {
        val key = keyOf(permissions)
        controllers[key] = controller
    }

    override fun handleRuntimePermissions(permissions: Array<out String>) {
        val context = activity ?: throw NullPointerException("The activity mustn't be null.")

        val areAllGranted = permissions.indexOfFirst { !context.isPermissionGranted(it) } == -1
        if (!areAllGranted) {
            if (isProcessingPermissions) {
                // The Fragment can process only one request at the same time.
                return
            }

            // Get the lifecycle controller.
            val controller = controllerOf(permissions)

            val rationaleCheck = controller.rationaleCheck()
            if (rationaleCheck == RationaleCheck.AFTER) {
                // Request the permissions.
                requestRuntimePermissions(permissions)
                return
            }

            val rationaleDelivering = controller.rationaleDelivering()
            // Get the permissions that must show the rationale.
            val permissionsWithRationale = permissionsThatShouldShowRationale(permissions)
            val notifyRationale = (rationaleDelivering == Delivering.AT_LEAST_ONE && permissionsWithRationale.isNotEmpty()) ||
                    (rationaleDelivering == Delivering.ALL && permissionsWithRationale.size == permissions.size)

            val rationaleHandled = if (notifyRationale) {
                val listener = listenerOf(permissions)
                // Show rationale of permissions.
                listener.permissionsShouldShowRationale(permissionsWithRationale)
            } else false

            if (!rationaleHandled) {
                // Request the permissions.
                requestRuntimePermissions(permissions)
            }
        } else {
            val listener = listenerOf(permissions)
            // All permissions are accepted.
            listener.permissionsAccepted(permissions)
        }
    }

    override fun requestRuntimePermissions(permissions: Array<out String>) {
        // The Fragment is now processing some permissions.
        isProcessingPermissions = true
        Log.d(TAG, "requesting permissions: ${permissions.flatString()}")
        requestPermissions(permissions, REQ_CODE_PERMISSIONS)
    }

    private fun permissionsThatShouldShowRationale(permissions: Array<out String>): Array<out String> =
            permissions.filter {
                shouldShowRequestPermissionRationale(it)
            }.toTypedArray()

    private fun controllerOf(permissions: Array<out String>): PermissionLifecycleController {
        val key = keyOf(permissions)
        return controllers.getOrElse(key) {
            throw IllegalArgumentException("You need a controller for the key $key.")
        }
    }

    private fun listenerOf(permissions: Array<out String>): RuntimePermissionHandler.Listener {
        val key = keyOf(permissions)
        return listeners.getOrElse(key) {
            throw IllegalArgumentException("You need a listener for the key $key.")
        }
    }

    private fun keyOf(permissions: Array<out String>) = permissions.flatString()

    companion object {
        private val TAG = FragmentRuntimePermissionHandler::class.java.simpleName
        private const val REQ_CODE_PERMISSIONS = 986
    }
}