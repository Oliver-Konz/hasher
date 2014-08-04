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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

/**
 * The scanner utilizes a FileVisitor to walk the directories und hash the files.
 *
 */
public class Scanner {

    public static final int MODE_UPDATE = 0;
    public static final int MODE_VERIFY = 1;

    private static final Logger logger = Logger.getLogger(Scanner.class.getName());

    private final boolean update;

    private final boolean verify;

    private final String algorithm;

    private final String hashFileName;

    private final MessageDigest digest;

    /**
     * Constructor.
     *
     * @param mode update and/or verify
     * @param algorithm the hash algorithm
     * @param hashFileName the hashes file name
     * @throws NoSuchAlgorithmException If the hashing algorithm is not available.
     */
    public Scanner(BitSet mode, String algorithm, String hashFileName) throws NoSuchAlgorithmException {
        if (mode.isEmpty()) {
            throw new RuntimeException("At least one mode must be set!");
        }
        if (mode.length() > 2) {
            throw new RuntimeException("Invalid mode.");
        }
        this.update = mode.get(MODE_UPDATE);
        this.verify = mode.get(MODE_VERIFY);
        this.algorithm = algorithm;
        digest = MessageDigest.getInstance(algorithm);
        this.hashFileName = hashFileName;
    }

    /**
     * For output-purposes
     * @return updating / verifying
     */
    public String getOperationName() {
        if (update && !verify) {
            return "Updating";
        }
        if (!update && verify) {
            return "Verifying";
        }
        return "Updating and verifying";
    }

    /**
     * Start hashing.
     *
     * @param path the directory to hash
     * @return true if no errors occurred
     */
    public Stats scan(Path path) {
        Instant startTime = Instant.now();
        HashVisitor visitor = new HashVisitor(this);
        try {
            Files.walkFileTree(path, visitor);
        } catch (IOException e) {
            // Should never happen
            logger.severe(e.toString());
            throw new RuntimeException(e);
        }
        return new Stats(Duration.between(startTime, Instant.now()), visitor.getFileBytes(), visitor.getFileCount(),
                visitor.getVerificationErrors(), visitor.getOtherErrors());
    }

    public boolean isUpdate() {
        return update;
    }

    public boolean isVerify() {
        return verify;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getHashFileName() {
        return hashFileName;
    }

    MessageDigest getDigest() {
        return digest;
    }
}
