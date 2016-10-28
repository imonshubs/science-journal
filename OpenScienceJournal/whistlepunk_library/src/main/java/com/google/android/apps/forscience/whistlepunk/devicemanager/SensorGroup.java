/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.google.android.apps.forscience.whistlepunk.devicemanager;

import com.google.android.apps.forscience.whistlepunk.api.scalarinput.InputDeviceSpec;

import java.util.List;

public interface SensorGroup {
    boolean hasSensorKey(String sensorKey);

    /**
     * Add {@code sensor} with key {@code sensorKey}.
     */
    void addSensor(String sensorKey, ConnectableSensor sensor);

    /**
     * @return true if the given sensorKey existed and was removed.
     */
    boolean removeSensor(String sensorKey);

    /**
     * Replace the sensor (if any) currently at {@code sensorKey} with {@code sensor}.
     *
     * Note that there are situations in which multiple asynchronous processes mean that sensorKey
     * is already gone before this executes, in which case this method should do the same thing as
     * addSensor.
     */
    void replaceSensor(String sensorKey, ConnectableSensor sensor);

    int getSensorCount();
}