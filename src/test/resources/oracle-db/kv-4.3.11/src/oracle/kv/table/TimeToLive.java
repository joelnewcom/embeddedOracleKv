/*-
 * Copyright (C) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This file was distributed by Oracle as part of a version of Oracle NoSQL
 * Database made available at:
 *
 * http://www.oracle.com/technetwork/database/database-technologies/nosqldb/downloads/index.html
 *
 * Please see the LICENSE file included in the top-level directory of the
 * appropriate version of Oracle NoSQL Database for a copy of the license and
 * additional information.
 */

package oracle.kv.table;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import com.sleepycat.persist.model.Persistent;

/**
 * TimeToLive is a utility class that represents a period of time, similar to
 * Java 8's java.time.Duration, but specialized to the needs of Oracle NoSQL
 * Database.
 * <p>
 * This class is restricted to durations of days and hours. It is only
 * used as input related to time to live (TTL) for {@link Row} instances,
 * set by using {@link Row#setTTL}. Construction allows only day and hour
 * durations for efficiency reasons. Durations of days are recommended as they
 * result in the least amount of storage consumed in a store.
 * <p>
 * Only positive durations are allowed on input, although negative durations
 * can be returned from {@link #fromExpirationTime} if the expirationTime is
 * in the past relative to the referenceTime.
 *
 * @since 4.0
 */
@Persistent
public final class TimeToLive implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long value;
    private final TimeUnit unit;

    /**
     * A convenience constant that can be used as an argument to
     * {@link Row#setTTL} to indicate that the row should not expire.
     */
    public static final TimeToLive DO_NOT_EXPIRE = ofDays(0);

    /**
     * Creates a duration using a period of hours.
     *
     * @param hours the number of hours in the duration, must be
     * a non-negative number
     *
     * @return the duration
     *
     * @throws IllegalArgumentException if a negative value is provided
     */
    public static TimeToLive ofHours(long hours) {
        if (hours < 0) {
            throw new IllegalArgumentException(
                "TimeToLive does not support negative time periods");
        }
        return new TimeToLive(hours, TimeUnit.HOURS);
    }

    /**
     * Creates a duration using a period of 24 hour days.
     *
     * @param days the number of days in the duration, must be
     * a non-negative number
     *
     * @return the duration
     *
     * @throws IllegalArgumentException if a negative value is provided
     */
    public static TimeToLive ofDays(long days) {
        if (days < 0) {
            throw new IllegalArgumentException(
                "TimeToLive does not support negative time periods");
        }
        return new TimeToLive(days, TimeUnit.DAYS);
    }

    /**
     * Returns the number of days in this duration, which may be negative.
     *
     * @return the number of days
     */
    public long toDays() {
        return TimeUnit.DAYS.convert(value, unit);
    }

    /**
     * Returns the number of hours in this duration, which may be negative.
     *
     * @return the number of hours
     */
    public long toHours() {
        return TimeUnit.HOURS.convert(value, unit);
    }

    /**
     * Returns an absolute time representing the duration plus the absolute
     * time reference parameter. If an expiration time from the current time is
     * desired the parameter should be {@link System#currentTimeMillis}. If the
     * duration of this object is 0 ({@link #DO_NOT_EXPIRE}), indicating no
     * expiration time, this method will return 0, regardless of the reference
     * time.
     *
     * @param referenceTime an absolute time in milliseconds since January
     * 1, 1970.
     *
     * @return time in milliseconds, 0 if this object's duration is 0
     */
    public long toExpirationTime(long referenceTime) {
        if (value == 0) {
            return 0;
        }
        return referenceTime + toMillis();
    }

    /**
     * Returns an instance of TimeToLive based on an absolute expiration
     * time and a reference time. If a duration relative to the current time
     * is desired the referenceTime should be {@link System#currentTimeMillis}.
     * If the expirationTime is 0, the referenceTime is ignored and a
     * TimeToLive of duration 0 is created, indicating no expiration.
     * <p>
     * Days will be use as the primary unit of duration if the expiration time
     * is evenly divisible into days, otherwise hours are used.
     *
     * @param expirationTime an absolute time in milliseconds since January
     * 1, 1970
     *
     * @param referenceTime an absolute time in milliseconds since January
     * 1, 1970.
     *
     * @return a new TimeToLive instance
     */
    public static TimeToLive fromExpirationTime(long expirationTime,
                                                long referenceTime) {
        final long MILLIS_PER_HOUR = 1000L * 60 * 60;

        if (expirationTime == 0) {
            return DO_NOT_EXPIRE;
        }

        /*
         * Calculate whether the given time in millis, when converted to hours,
         * rounding up, is not an even multiple of 24.
         */
        final long hours =
            (expirationTime + MILLIS_PER_HOUR - 1) / MILLIS_PER_HOUR;
        boolean timeInHours = hours % 24 != 0;

        /*
         * This may result in a negative duration. This is ok and is documented.
         * If somehow the duration is 0, set it to -1 hours because 0 means
         * no expiration.
         */
        long duration = expirationTime - referenceTime;

        if (duration == 0) {
            /* very unlikely, but possible; set to -1 hours to avoid 0 */
            duration = -MILLIS_PER_HOUR;
            timeInHours = true;
        }
        if (timeInHours) {
            return new TimeToLive(
                TimeUnit.HOURS.convert(duration, TimeUnit.MILLISECONDS),
                TimeUnit.HOURS);
        }
        return new TimeToLive(
                TimeUnit.DAYS.convert(duration, TimeUnit.MILLISECONDS),
                TimeUnit.DAYS);
    }

    /**
     * Equality compares the duration only if the units used for construction
     * are the same. If the units (ofHours vs ofDays) are different two
     * instances will not be equal even if their absolute durations are the
     * same unless both values are 0, which means no expiration.
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TimeToLive)) {
            return false;
        }
        TimeToLive otherTTL = (TimeToLive) other;
        if (value == 0 && otherTTL.value == 0) {
            return true;
        }
        return (unit == otherTTL.unit && value == otherTTL.value);
    }

    @Override
    public int hashCode() {
        /* include the high order bits */
        return (int) ((value >>> 32) ^ value) + unit.hashCode();
    }

    @Override
    public String toString() {
        return value + " " + unit.toString();
    }

    /**
     * Internal use only
     * @hidden
     */
    public static TimeToLive createTimeToLive(long value, TimeUnit unit) {
        return new TimeToLive(value, unit);
    }

    /**
     * Internal use only
     * @hidden
     *
     * Returns the numeric duration value
     */
    public long getValue() {
        return value;
    }

    /**
     * Internal use only
     * @hidden
     *
     * Returns the TimeUnit used for the duration
     */
    public TimeUnit getUnit() {
        return unit;
    }

    /**
     * Private constructor. All construction is done via this constructor, which
     * validates the arguments.
     *
     * @param value value of time
     * @param unit unit of time, cannot be null
     */
    private TimeToLive(long value, TimeUnit unit) {

        if (unit != TimeUnit.DAYS && unit != TimeUnit.HOURS) {
            throw new IllegalArgumentException(
                "Invalid TimeUnit (" + unit + ") in TimeToLive construction." +
                "Must be DAYS or HOURS.");
        }

        this.value = value;
        this.unit = unit;
    }

    /**
     * For DPL. DPL/@Persistent is *only* required because TimeToLive was
     * (mistakenly) included in the 4.0 EvolveTable task, which is persistent.
     * As of 4.0 DPL is not used for plans/tasks but it's needed to upgrade
     * plans from 3.0-based stores. Eventually, when the pre-req version
     * increases beyond the DPL-based version, this can be removed.
     */
    private TimeToLive() {
        this(0, TimeUnit.DAYS);
    }

    private long toMillis() {
        return TimeUnit.MILLISECONDS.convert(value, unit);
    }
}
