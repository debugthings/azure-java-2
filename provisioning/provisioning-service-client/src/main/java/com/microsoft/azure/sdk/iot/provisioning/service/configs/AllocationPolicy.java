// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.sdk.iot.provisioning.service.configs;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * The Device Provisioning Service enrollment level allocation policies.
 *
 * @see <a href="https://docs.microsoft.com/en-us/azure/iot-dps/tutorial-provision-multiple-hubs#set-the-device-provisioning-service-allocation-policy">
 *     Provision devices across load-balanced IoT hubs</a>
 */
public enum AllocationPolicy implements Serializable
{
    @SerializedName("hashed")
    HASHED,

    @SerializedName("geoLatency")
    GEOLATENCY,

    @SerializedName("static")
    STATIC,

    @SerializedName("custom")
    CUSTOM
}
