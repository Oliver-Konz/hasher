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

import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;

/**
 * Represents a line in the hashes file.
 */
public class HashEntry implements Comparable<HashEntry> {

    /**
     * The delimiter used to separate the values in the hashes file.
     */
    public static final String DELIMITER = "|";

    private static final String DELIMITER_REGEX = "\\|";

    private static final Base64.Encoder base64encoder = Base64.getEncoder();
    private static final Base64.Decoder base64decoder = Base64.getDecoder();

    private final String name;
    private FileTime time;
    private long size;
    private String algorithm;
    private byte[] hash;
    private boolean changed = true;
    private boolean stillExists = true;

    /**
     * Create a HashEntry instance from a line in the hashes file.
     *
     * @param line the line
     * @return the new HashEntry instance
     */
    public static HashEntry fromString(String line) {
        String[] parts = line.split(DELIMITER_REGEX);
        if (parts.length != 5) {
            throw new IllegalArgumentException(String.format("Incorrect hash entry format: %s", line));
        }
        HashEntry entry = new HashEntry(
                parts[0],
                FileTime.from(Instant.parse(parts[1])),
                Long.parseLong(parts[2]),
                parts[3],
                base64decoder.decode(parts[4]));
        entry.changed = false;
        entry.stillExists = false;
        return entry;
    }

    /**
     * Were there any changes in the given hash entries?
     *
     * @param hashEntries the entries
     * @return the result
     */
    public static boolean hashEntriesChanged(Collection<HashEntry> hashEntries) {
        for (HashEntry hashEntry : hashEntries) {
            if (hashEntry.wasChanged() || !hashEntry.stillExists()) {
                return true;
            }
        }
        return hashEntries.isEmpty();
    }

    /**
     * Constructor.
     *
     * @param name the file name
     * @param time the modification time
     * @param size the file size
     * @param algorithm the algorithm used to hash
     * @param hash the hash
     */
    public HashEntry(String name, FileTime time, long size, String algorithm, byte[] hash) {
        this.name = name;
        this.time = time;
        this.size = size;
        this.algorithm = algorithm;
        this.hash = hash;
    }

    /**
     * Updates an existing hash entry.
     *
     * @param time the modification time
     * @param size the file size
     * @param algorithm the algorithm used to hash
     * @param hash the hash
     */
    public void update(FileTime time, long size, String algorithm, byte[] hash) {
        this.time = time;
        this.size = size;
        this.algorithm = algorithm;
        this.hash = hash;
        changed = true;
        stillExists = true;
    }

    /**
     * Converts the entry in a line to write in the hashes file.
     *
     * @return the line
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append(DELIMITER);
        sb.append(time.toString());
        sb.append(DELIMITER);
        sb.append(size);
        sb.append(DELIMITER);
        sb.append(algorithm);
        sb.append(DELIMITER);
        sb.append(base64encoder.encodeToString(hash));
        return sb.toString();
    }

    /**
     * @return the file name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the modification time
     */
    public FileTime getTime() {
        return time;
    }

    /**
     * @return the file size (in bytes)
     */
    public long getSize() {
        return size;
    }

    /**
     * @return the hashing algorithm
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * @return the hash
     */
    public byte[] getHash() {
        return hash;
    }

    /**
     * @return Was the entry changed after being read from the hashes file?
     */
    public boolean wasChanged() {
        return changed;
    }

    /**
     * @return Does the file that corresponds to this entry still exist?
     */
    public boolean stillExists() {
        return stillExists;
    }

    /**
     * The file that corresponds to this entry does still exist.
     */
    public void setStillExists() {
        this.stillExists = true;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof HashEntry)) return false;

        final HashEntry hashEntry = (HashEntry) o;

        if (size != hashEntry.size) return false;
        if (!algorithm.equals(hashEntry.algorithm)) return false;
        if (!Arrays.equals(hash, hashEntry.hash)) return false;
        if (!name.equals(hashEntry.name)) return false;
        if (!time.equals(hashEntry.time)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + time.hashCode();
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + algorithm.hashCode();
        result = 31 * result + Arrays.hashCode(hash);
        return result;
    }

    @Override
    public int compareTo(final HashEntry o) {
        return name.compareTo(o.name);
    }
}
