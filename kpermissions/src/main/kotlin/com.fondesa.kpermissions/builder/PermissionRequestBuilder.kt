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

package com.fondesa.kpermissions.builder

import com.fondesa.kpermissions.request.PermissionRequest
import com.fondesa.kpermissions.request.runtime.RuntimePermissionHandler
import com.fondesa.kpermissions.request.runtime.RuntimePermissionHandlerProvider
import com.fondesa.kpermissions.request.runtime.nonce.PermissionNonce
import com.fondesa.kpermissions.request.runtime.nonce.PermissionNonceGenerator

/**
 * Builder used to create a [PermissionRequest].
 *
 * This builder allows also to customize a [PermissionRequest] before it is sent.
 * It's recommended to avoid to hold the reference to a [PermissionRequestBuilder]
 * to create [PermissionRequest]s with different configurations because the values
 * assigned from the previous configurations could override the default values.
 * The correct behavior is to use the same instance of a [PermissionRequestBuilder] only
 * for the same set of configurations.
 */
interface PermissionRequestBuilder {

    /**
     * Sets the permissions that must be requested.
     *
     * @param permissions set of permissions that must be requested.
     * @return the builder itself.
     */
    fun permissions(vararg permissions: String): PermissionRequestBuilder

    /**
     * Sets the [PermissionNonceGenerator] used to generate a [PermissionNonce].
     *
     * @param nonceGenerator instance of [PermissionNonceGenerator] that must be added.
     * @return the builder itself.
     */
    fun nonceGenerator(nonceGenerator: PermissionNonceGenerator): PermissionRequestBuilder

    /**
     * Sets the [RuntimePermissionHandlerProvider] used to provide a [RuntimePermissionHandler].
     *
     * @param runtimeHandlerProvider instance of [RuntimePermissionHandlerProvider] that must be added.
     * @return the builder itself.
     */
    fun runtimeHandlerProvider(runtimeHandlerProvider: RuntimePermissionHandlerProvider): PermissionRequestBuilder

    /**
     * Build the [PermissionRequest] with all the configurations set.
     * It will also assigns the default values and throw some exceptions if the configuration
     * of this builder isn't a valid one.
     *
     * @return instance of [PermissionRequest] built from these configurations.
     * @throws IllegalArgumentException if the permissions or the [RuntimePermissionHandlerProvider]
     * are not specified.
     */
    fun build(): PermissionRequest
}