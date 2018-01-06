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

package com.fondesa.kpermissions.request.manifest

import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import com.fondesa.kpermissions.controller.Delivering
import com.fondesa.kpermissions.controller.PermissionLifecycleController
import com.fondesa.kpermissions.extensions.isPermissionGranted
import com.fondesa.kpermissions.request.BasePermissionRequest

/**
 * Created by antoniolig on 05/01/18.
 */
class ManifestPermissionRequest(private val context: Context,
                                private val permissions: Array<out String>,
                                private val lifecycleController: PermissionLifecycleController) :
        BasePermissionRequest() {

    override fun send() {
        val acceptedList = mutableListOf<String>()
        val deniedList = mutableListOf<String>()

        permissions.forEach {
            if (context.isPermissionGranted(it)) {
                acceptedList.add(it)
            } else {
                deniedList.add(it)
            }
        }

        val acceptedPermissions = acceptedList.toTypedArray()
        val deniedPermissions = deniedList.toTypedArray()

        val acceptedDelivering = lifecycleController.acceptedDelivering()
        val deniedDelivering = lifecycleController.permanentlyDeniedDelivering()

        val notifyAccepted = (acceptedDelivering == Delivering.ALL && deniedPermissions.isEmpty()) ||
                (acceptedDelivering == Delivering.AT_LEAST_ONE && acceptedPermissions.isNotEmpty())

        if (notifyAccepted) {
            acceptedListener?.onPermissionsAccepted(acceptedPermissions)
        }

        val notifyDenied = (deniedDelivering == Delivering.ALL && acceptedPermissions.isEmpty()) ||
                (deniedDelivering == Delivering.AT_LEAST_ONE && deniedPermissions.isNotEmpty())

        if (notifyDenied) {
            deniedListener?.onPermissionsPermanentlyDenied(deniedPermissions)
        }
    }
}