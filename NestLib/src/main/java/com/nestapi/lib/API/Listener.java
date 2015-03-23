/**
 * Copyright 2014 Nest Labs Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software * distributed under
 * the License is distributed on an "AS IS" BASIS, * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nestapi.lib.API;

import android.support.annotation.NonNull;

/**
 * This class is used to build a collection of data listeners for updates
 * to user data (devices and structure details).
 */
public final class Listener {
    private final ThermostatListener mThermostatListener;
    private final StructureListener mStructureListener;
    private final SmokeCOAlarmListener mSmokeCOAlarmListener;
    private final MetaDataListener mMetaDataListener;

    private Listener(Builder builder) {
        mThermostatListener = builder.mThermostatListener;
        mStructureListener = builder.mStructureListener;
        mSmokeCOAlarmListener = builder.mSmokeCOAlarmListener;
        mMetaDataListener = builder.mMetaDataListener;
    }

    ThermostatListener getThermostatListener() {
        return mThermostatListener;
    }

    StructureListener getStructureListener() {
        return mStructureListener;
    }

    SmokeCOAlarmListener getSmokeCOAlarmListener() {
        return mSmokeCOAlarmListener;
    }

    MetaDataListener getMetaDataListener() {
        return mMetaDataListener;
    }

    public static class Builder {
        private ThermostatListener mThermostatListener = null;
        private StructureListener mStructureListener = null;
        private SmokeCOAlarmListener mSmokeCOAlarmListener = null;
        private MetaDataListener mMetaDataListener = null;

        public Builder setThermostatListener(ThermostatListener listener) {
            mThermostatListener = listener;
            return this;
        }

        public Builder setStructureListener(StructureListener listener) {
            mStructureListener = listener;
            return this;
        }

        public Builder setSmokeCOAlarmListener(SmokeCOAlarmListener listener) {
            mSmokeCOAlarmListener = listener;
            return this;
        }

        public Builder setMetaDataListener(MetaDataListener listener) {
            mMetaDataListener = listener;
            return this;
        }

        public Listener build() {
            return new Listener(this);
        }
    }

    public interface ThermostatListener {
        /**
         * Called when updated data is retrieved for a Thermostat.
         * @param thermostat the new data for the thermostat (guaranteed
         *                   to be non-null)
         */
        void onThermostatUpdated(@NonNull Thermostat thermostat);
    }

    public interface StructureListener {
        /**
         * Called when updated data is retrieved for a user's structure.
         * @param structure the new data for the structure (guaranteed
         *                  to be non-null)
         */
        void onStructureUpdated(@NonNull Structure structure);
    }

    public interface SmokeCOAlarmListener {
        /**
         * Called when updated data is retrieved for a Nest Protect.
         * @param smokeCOAlarm the new data for the Nest Protect (guaranteed
         *                     to be non-null)
         */
        void onSmokeCOAlarmUpdated(@NonNull SmokeCOAlarm smokeCOAlarm);
    }

    public interface MetaDataListener {
        /**
         * Called when updated data is retrieved for the Metadata for a client
         * @param metaData the new data for the meta data for the client (guaranteed
         *                 to be non-null)
         */
        void onMetaDataUpdated(@NonNull MetaData metaData);
    }
}