/*
 * Hasher - Hashes and verifies entire directory trees.
 * Copyright (C) 2014  Oliver Konz <code@oliverkonz.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.konz.hasher;

import java.time.Duration;
import java.time.temporal.TemporalUnit;

/**
 * The statistics of the performed operation.
 */
public class Stats {

    public static Stats EMPTY = new Stats(Duration.ZERO, 0L, 0L, 0L, 0L);

    public static final long KI = 1024L;
    public static final long MI = KI * KI;
    public static final long GI = MI * KI;
    public static final long TI = GI * KI;

    private final Duration runtime;
    private final long bytesHashed;
    private final long filesHashed;
    private final long verificationErrors;
    private final long otherErrors;

    public Stats(final Duration runtime, final long bytesHashed, final long filesHashed, final long verificationErrors, final long otherErrors) {
        this.runtime = runtime;
        this.bytesHashed = bytesHashed;
        this.filesHashed = filesHashed;
        this.verificationErrors = verificationErrors;
        this.otherErrors = otherErrors;
    }

    public Duration getRuntime() {
        return runtime;
    }

    public long getBytesHashed() {
        return bytesHashed;
    }

    public long getFilesHashed() {
        return filesHashed;
    }

    public long getVerificationErrors() {
        return verificationErrors;
    }

    public long getOtherErrors() {
        return otherErrors;
    }

    public double getRate() {
        if (runtime.equals(Duration.ZERO)) {
            return 0.0d;
        }
        if (runtime.getSeconds() < 1000) {
            long nanoTime = runtime.getSeconds() * 1000000000 + runtime.getNano();
            return bytesHashed * 1000000000.0d / nanoTime;
        }
        return bytesHashed * 1.0d / runtime.getSeconds();
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Files hashed:        ").append(filesHashed).append('\n');
        sb.append("Verification errors: ").append(verificationErrors).append('\n');
        sb.append("Other errors:        ").append(otherErrors).append('\n');
        sb.append("Size of files (MiB): ").append(bytesHashed * 1.0d / MI).append('\n');
        sb.append("Runtime:             ").append(runtime.toString()).append('\n');
        sb.append("Rate (MiB/s):        ").append(getRate() / MI).append('\n');
        return sb.toString();
    }

    /**
     * Creates a new stats object by adding this to the other.
     *
     * @param other the other stats
     * @return the sum
     */
    public Stats add(Stats other) {
        return new Stats(
                runtime.plus(other.runtime),
                bytesHashed + other.bytesHashed,
                filesHashed + other.filesHashed,
                verificationErrors + other.verificationErrors,
                otherErrors + other.otherErrors);
    }
}
