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

package android.healthconnect.aidl;

import static android.healthconnect.Constants.DEFAULT_INT;
import static android.healthconnect.Constants.DEFAULT_LONG;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.healthconnect.AggregateRecordsGroupedByDurationResponse;
import android.healthconnect.AggregateRecordsGroupedByPeriodResponse;
import android.healthconnect.AggregateRecordsResponse;
import android.healthconnect.AggregateResult;
import android.healthconnect.LocalTimeRangeFilter;
import android.healthconnect.TimeInstantRangeFilter;
import android.healthconnect.TimeRangeFilter;
import android.healthconnect.TimeRangeFilterHelper;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.internal.datatypes.utils.AggregationTypeIdMapper;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArrayMap;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** @hide */
public class AggregateDataResponseParcel implements Parcelable {
    public static final Creator<AggregateDataResponseParcel> CREATOR =
            new Creator<>() {
                @Override
                public AggregateDataResponseParcel createFromParcel(Parcel in) {
                    return new AggregateDataResponseParcel(in);
                }

                @Override
                public AggregateDataResponseParcel[] newArray(int size) {
                    return new AggregateDataResponseParcel[size];
                }
            };
    private final List<AggregateRecordsResponse<?>> mAggregateRecordsResponses;
    private Duration mDuration;
    private Period mPeriod;
    private TimeRangeFilter mTimeRangeFilter;

    public AggregateDataResponseParcel(List<AggregateRecordsResponse<?>> aggregateRecordsResponse) {
        mAggregateRecordsResponses = aggregateRecordsResponse;
    }

    protected AggregateDataResponseParcel(Parcel in) {
        final int size = in.readInt();
        mAggregateRecordsResponses = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            final int mapSize = in.readInt();
            Map<Integer, AggregateResult<?>> result = new ArrayMap<>(mapSize);

            for (int mapI = 0; mapI < mapSize; mapI++) {
                int id = in.readInt();
                boolean hasValue = in.readBoolean();
                if (hasValue) {
                    result.put(
                            id,
                            AggregationTypeIdMapper.getInstance()
                                    .getAggregateResultFor(id, in)
                                    .setZoneOffset(parseZoneOffset(in))
                                    .setDataOrigins(in.createStringArrayList()));
                } else {
                    result.put(id, null);
                }
            }

            mAggregateRecordsResponses.add(new AggregateRecordsResponse<>(result));
        }

        int period = in.readInt();
        if (period != DEFAULT_INT) {
            mPeriod = Period.ofDays(period);
        }

        long duration = in.readLong();
        if (duration != DEFAULT_LONG) {
            mDuration = Duration.ofMillis(duration);
        }

        long startTime = in.readLong();
        long endTime = in.readLong();
        if (startTime != DEFAULT_LONG && endTime != DEFAULT_LONG) {
            mTimeRangeFilter =
                    new TimeInstantRangeFilter.Builder()
                            .setStartTime(Instant.ofEpochMilli(startTime))
                            .setEndTime(Instant.ofEpochMilli(endTime))
                            .build();
        }
    }

    public AggregateDataResponseParcel setDuration(
            @Nullable Duration duration, @Nullable TimeRangeFilter timeRangeFilter) {
        mDuration = duration;
        mTimeRangeFilter = timeRangeFilter;

        return this;
    }

    public AggregateDataResponseParcel setPeriod(
            @Nullable Period period, @Nullable TimeRangeFilter timeRangeFilter) {
        mPeriod = period;
        mTimeRangeFilter = timeRangeFilter;

        return this;
    }

    /**
     * @return the first response from {@code mAggregateRecordsResponses}
     */
    public AggregateRecordsResponse<?> getAggregateDataResponse() {
        return mAggregateRecordsResponses.get(0);
    }

    /**
     * @return responses from {@code mAggregateRecordsResponses} grouped as per the {@code
     *     mDuration}
     */
    public List<AggregateRecordsGroupedByDurationResponse<?>>
            getAggregateDataResponseGroupedByDuration() {
        Objects.requireNonNull(mDuration);

        List<AggregateRecordsGroupedByDurationResponse<?>>
                aggregateRecordsGroupedByDurationResponse = new ArrayList<>();
        long mStartTime = TimeRangeFilterHelper.getDurationStart(mTimeRangeFilter);
        long mDelta = getDurationDelta(mDuration);
        for (AggregateRecordsResponse<?> aggregateRecordsResponse : mAggregateRecordsResponses) {
            aggregateRecordsGroupedByDurationResponse.add(
                    new AggregateRecordsGroupedByDurationResponse<>(
                            getDurationInstant(mStartTime),
                            getDurationInstant(mStartTime + mDelta),
                            aggregateRecordsResponse.getAggregateResults()));
            mStartTime += mDelta;
        }

        return aggregateRecordsGroupedByDurationResponse;
    }

    /**
     * @return responses from {@code mAggregateRecordsResponses} grouped as per the {@code mPeriod}
     */
    public List<AggregateRecordsGroupedByPeriodResponse<?>>
            getAggregateDataResponseGroupedByPeriod() {
        Objects.requireNonNull(mPeriod);

        List<AggregateRecordsGroupedByPeriodResponse<?>> aggregateRecordsGroupedByPeriodResponses =
                new ArrayList<>();
        long mStartTime = TimeRangeFilterHelper.getPeriodStart(mTimeRangeFilter);
        long mDelta = getPeriodDelta(mPeriod);
        for (AggregateRecordsResponse<?> aggregateRecordsResponse : mAggregateRecordsResponses) {
            aggregateRecordsGroupedByPeriodResponses.add(
                    new AggregateRecordsGroupedByPeriodResponse<>(
                            getPeriodLocalDateTime(mStartTime),
                            getPeriodLocalDateTime(mStartTime + mDelta),
                            aggregateRecordsResponse.getAggregateResults()));
            mStartTime += mDelta;
        }

        if (!aggregateRecordsGroupedByPeriodResponses.isEmpty()) {
            aggregateRecordsGroupedByPeriodResponses
                    .get(0)
                    .setStartTime(getPeriodStartLocalDateTime(mTimeRangeFilter));
            aggregateRecordsGroupedByPeriodResponses
                    .get(aggregateRecordsGroupedByPeriodResponses.size() - 1)
                    .setStartTime(getPeriodEndLocalDateTime(mTimeRangeFilter));
        }

        return aggregateRecordsGroupedByPeriodResponses;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mAggregateRecordsResponses.size());
        for (AggregateRecordsResponse<?> aggregateRecordsResponse : mAggregateRecordsResponses) {
            dest.writeInt(aggregateRecordsResponse.getAggregateResults().size());
            aggregateRecordsResponse
                    .getAggregateResults()
                    .forEach(
                            (key, val) -> {
                                dest.writeInt(key);
                                // to represent if the value is present or not
                                dest.writeBoolean(val != null);
                                if (val != null) {
                                    val.putToParcel(dest);
                                    ZoneOffset zoneOffset = val.getZoneOffset();
                                    if (zoneOffset != null) {
                                        dest.writeInt(val.getZoneOffset().getTotalSeconds());
                                    } else {
                                        dest.writeInt(DEFAULT_INT);
                                    }
                                    Set<DataOrigin> dataOrigins = val.getDataOrigins();
                                    List<String> packageNames = new ArrayList<>();
                                    for (DataOrigin dataOrigin : dataOrigins) {
                                        packageNames.add(dataOrigin.getPackageName());
                                    }
                                    dest.writeStringList(packageNames);
                                }
                            });
        }

        if (mPeriod != null) {
            dest.writeInt(mPeriod.getDays());
        } else {
            dest.writeInt(DEFAULT_INT);
        }

        if (mDuration != null) {
            dest.writeLong(mDuration.toMillis());
        } else {
            dest.writeLong(DEFAULT_LONG);
        }

        if (mTimeRangeFilter != null) {
            dest.writeLong(TimeRangeFilterHelper.getDurationStart(mTimeRangeFilter));
            dest.writeLong(TimeRangeFilterHelper.getDurationEnd(mTimeRangeFilter));
        } else {
            dest.writeLong(DEFAULT_LONG);
            dest.writeLong(DEFAULT_LONG);
        }
    }

    private ZoneOffset parseZoneOffset(Parcel in) {
        int zoneOffsetInSecs = in.readInt();
        ZoneOffset zoneOffset = null;
        if (zoneOffsetInSecs != DEFAULT_INT) {
            zoneOffset = ZoneOffset.ofTotalSeconds(zoneOffsetInSecs);
        }

        return zoneOffset;
    }

    private LocalDateTime getPeriodStartLocalDateTime(TimeRangeFilter timeRangeFilter) {
        if (timeRangeFilter instanceof TimeInstantRangeFilter) {
            return LocalDateTime.ofInstant(
                    ((TimeInstantRangeFilter) timeRangeFilter).getStartTime(),
                    ZoneOffset.systemDefault());
        } else if (timeRangeFilter instanceof LocalTimeRangeFilter) {
            return ((LocalTimeRangeFilter) timeRangeFilter).getStartTime();
        } else {
            throw new IllegalArgumentException(
                    "Invalid time filter object. Object should be either "
                            + "TimeInstantRangeFilter or LocalTimeRangeFilter.");
        }
    }

    private LocalDateTime getPeriodEndLocalDateTime(TimeRangeFilter timeRangeFilter) {
        if (timeRangeFilter instanceof TimeInstantRangeFilter) {
            return LocalDateTime.ofInstant(
                    ((TimeInstantRangeFilter) timeRangeFilter).getEndTime(),
                    ZoneOffset.systemDefault());
        } else if (timeRangeFilter instanceof LocalTimeRangeFilter) {
            return ((LocalTimeRangeFilter) timeRangeFilter).getEndTime();
        } else {
            throw new IllegalArgumentException(
                    "Invalid time filter object. Object should be either "
                            + "TimeInstantRangeFilter or LocalTimeRangeFilter.");
        }
    }

    private long getPeriodDelta(Period period) {
        return period.getDays();
    }

    private LocalDateTime getPeriodLocalDateTime(long period) {
        return LocalDateTime.of(LocalDate.ofEpochDay(period), LocalTime.MIN);
    }

    private Instant getDurationInstant(long duration) {
        return Instant.ofEpochMilli(duration);
    }

    private long getDurationDelta(Duration duration) {
        return duration.toMillis();
    }
}
