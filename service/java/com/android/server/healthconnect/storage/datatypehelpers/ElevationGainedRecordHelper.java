/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.server.healthconnect.storage.datatypehelpers;

import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.ELEVATION_RECORD_ELEVATION_GAINED_TOTAL;

import static com.android.server.healthconnect.storage.utils.StorageUtils.REAL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorDouble;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.database.Cursor;
import android.healthconnect.AggregateResult;
import android.healthconnect.datatypes.AggregationType;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.internal.datatypes.ElevationGainedRecordInternal;
import android.util.Pair;

import java.util.Collections;
import java.util.List;

/**
 * Helper class for ElevationGainedRecord.
 *
 * @hide
 */
@HelperFor(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_ELEVATION_GAINED)
public final class ElevationGainedRecordHelper
        extends IntervalRecordHelper<ElevationGainedRecordInternal> {
    private static final String ELEVATION_GAINED_RECORD_TABLE_NAME =
            "elevation_gained_record_table";
    private static final String ELEVATION_COLUMN_NAME = "elevation";

    @Override
    public AggregateResult<?> getAggregateResult(
            Cursor results, AggregationType<?> aggregationType) {
        switch (aggregationType.getAggregationTypeIdentifier()) {
            case ELEVATION_RECORD_ELEVATION_GAINED_TOTAL:
                return new AggregateResult<>(
                                results.getDouble(results.getColumnIndex(ELEVATION_COLUMN_NAME)))
                        .setZoneOffset(getZoneOffset(results));

            default:
                return null;
        }
    }

    @Override
    @NonNull
    public String getMainTableName() {
        return ELEVATION_GAINED_RECORD_TABLE_NAME;
    }

    @Override
    AggregateParams getAggregateParams(AggregationType<?> aggregateRequest) {
        switch (aggregateRequest.getAggregationTypeIdentifier()) {
            case ELEVATION_RECORD_ELEVATION_GAINED_TOTAL:
                return new AggregateParams(
                        ELEVATION_GAINED_RECORD_TABLE_NAME,
                        Collections.singletonList(ELEVATION_COLUMN_NAME),
                        START_TIME_COLUMN_NAME);
            default:
                return null;
        }
    }

    @Override
    void populateSpecificRecordValue(
            @NonNull Cursor cursor, @NonNull ElevationGainedRecordInternal elevationGainedRecord) {
        elevationGainedRecord.setElevation(getCursorDouble(cursor, ELEVATION_COLUMN_NAME));
    }

    @Override
    void populateSpecificContentValues(
            @NonNull ContentValues contentValues,
            @NonNull ElevationGainedRecordInternal elevationGainedRecord) {
        contentValues.put(ELEVATION_COLUMN_NAME, elevationGainedRecord.getElevation());
    }

    @Override
    @NonNull
    protected List<Pair<String, String>> getIntervalRecordColumnInfo() {
        return Collections.singletonList(new Pair<>(ELEVATION_COLUMN_NAME, REAL));
    }
}
