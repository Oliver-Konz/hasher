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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scanner's file visitor.
 */
class HashVisitor implements FileVisitor<Path> {

    public static final int BLOCK_SIZE = 32768;

    private static final Logger logger = Logger.getLogger(HashVisitor.class.getName());

    private Scanner scanner;
    private final Map<Path, Map<String, HashEntry>> hashFiles = new HashMap<>();
    private final Map<String, Optional<MessageDigest>> otherAlgorithms = new HashMap<>();
    private long verificationErrors = 0L;
    private long otherErrors = 0L;
    private long fileCount = 0L;
    private long fileBytes = 0L;

    public HashVisitor(final Scanner scanner) {
        this.scanner = scanner;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        Path hashFilePath = dir.resolve(scanner.getHashFileName());
        File hashFile = hashFilePath.toFile();
        Map<String, HashEntry> hashEntries = new HashMap<>();

        if (hashFile.exists()) {
            otherErrors += HashEntry.parseHashesFile(hashFilePath, hashEntries);
        } else {
            if (scanner.isVerify()) {
                logger.info("Unhashed directory: " + dir.toString());
            }
        }
        hashFiles.put(dir, hashEntries);

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (exc == null) {
            Map<String, HashEntry> hashEntries = hashFiles.get(dir);

            if (scanner.isUpdate() && HashEntry.hashEntriesChanged(hashEntries.values())) {
                File hashFile = dir.resolve(scanner.getHashFileName()).toFile();

                try (BufferedWriter bw = new BufferedWriter(new FileWriter(hashFile))) {
                    for (HashEntry entry : new TreeSet<>(hashEntries.values())) {
                        if (entry.stillExists()) {
                            bw.write(entry.toString());
                            bw.newLine();
                        }
                    }
                }
            }
        }

        hashFiles.remove(dir);

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        String name = file.getFileName().toString();

        // Don't hash the hashes file
        if (!attrs.isRegularFile() || scanner.getHashFileName().equals(name)) {
            return FileVisitResult.CONTINUE;
        }

        // Get the correct entry
        Map<String, HashEntry> hashEntries = hashFiles.get(file.getParent());
        Optional<HashEntry> entry = Optional.ofNullable(hashEntries.get(name));

        // Verify
        boolean verified = scanner.isVerify() && verify(entry, file, attrs);

        // Update
        if (scanner.isUpdate()) hashEntries.put(name, update(file, attrs, entry, verified));

        return FileVisitResult.CONTINUE;
    }

    /**
     * Verify the file against the entry.
     *
     * @param entry the entry, if it exists
     * @param file the file
     * @param attrs the file attributes
     * @return the success of the verification
     * @throws IOException if reading the file fails
     */
    private boolean verify(Optional<HashEntry> entry, Path file, BasicFileAttributes attrs) throws IOException {
        FileTime time = attrs.lastModifiedTime();
        long size = attrs.size();
        boolean verified = false;

        if (entry.isPresent()) {
            // If modification time and size match we can compare the hashes
            if (time.equals(entry.get().getTime()) && entry.get().getSize() == size) {
                Optional<byte[]> hash = doHash(file, entry.get().getAlgorithm(), attrs);
                if (hash.isPresent()) {
                    if (Arrays.equals(entry.get().getHash(), hash.get())) {
                        verified = true;
                        if (logger.isLoggable(Level.FINE)) logger.fine("Verifed: " + file.toString());
                    } else {
                        logger.severe("Verification failed for " + file.toString());
                        verificationErrors++;
                    }
                } else {
                    logger.warning("No verification algorithm for " + file.toString());
                    otherErrors++;
                }
            } else {
                logger.info(String.format("Modified file (%s): %s", time.toString(), file.toString()));
            }
        } else {
            logger.info("Unhashed file: " + file.toString());
        }

        return verified;
    }

    /**
     * Creates or updates the hash for the file if necessary.
     *
     * @param file the file
     * @param attrs the file attributes
     * @param entry the entry, if it exists
     * @param verified was the hash already verified
     * @return the new/updated/unchanged entry
     * @throws IOException if reading the file fails
     */
    private HashEntry update(Path file, BasicFileAttributes attrs, Optional<HashEntry> entry, boolean verified) throws IOException {
        String name = file.getFileName().toString();
        FileTime time = attrs.lastModifiedTime();
        long size = attrs.size();
        HashEntry updatedEntry;

        if (!entry.isPresent()) {
            updatedEntry = new HashEntry(name, time, size, scanner.getAlgorithm(), doHash(file, scanner.getAlgorithm(), attrs).get());
            if (logger.isLoggable(Level.FINE)) logger.fine("Hashed: " + file.toString());
        } else {
            updatedEntry = entry.get();
            updatedEntry.setStillExists();
            // If the hash was verified we do not need to compute a new one
            if (!verified) {
                updatedEntry.update(time, size, scanner.getAlgorithm(), doHash(file, scanner.getAlgorithm(), attrs).get());
                if (logger.isLoggable(Level.FINE)) logger.fine("Hashed: " + file.toString());
            }
        }

        return updatedEntry;
    }

    /**
     * Perform the hashing.
     *
     * @param file the file to hash
     * @param requiredAlgorithm the algorithm to use
     * @param attrs file attributes - for the statistics only.
     * @return The hash result - if the algorithm was available
     * @throws java.io.IOException If the file cannot be read.
     */
    private Optional<byte[]> doHash(Path file, String requiredAlgorithm, BasicFileAttributes attrs) throws IOException {

        // Get the required message digest
        MessageDigest requiredDigest;
        if (scanner.getAlgorithm().equals(requiredAlgorithm)) {
            requiredDigest = scanner.getDigest();
        } else {
            Optional<MessageDigest> maybeDigest = otherAlgorithms.get(requiredAlgorithm);
            if (maybeDigest == null) {
                try {
                    maybeDigest = Optional.of(MessageDigest.getInstance(requiredAlgorithm));
                } catch (NoSuchAlgorithmException e) {
                    logger.warning(String.format("Algorithm %s is not available.", requiredAlgorithm));
                    maybeDigest = Optional.empty();
                }
                otherAlgorithms.put(requiredAlgorithm, maybeDigest);
            }
            if (maybeDigest.isPresent()) {
                requiredDigest = maybeDigest.get();
            } else {
                return Optional.empty();
            }
        }

        // read the file
        try (FileInputStream inputStream = new FileInputStream(file.toFile())) {
            FileChannel channel = inputStream.getChannel();
            ByteBuffer buff = ByteBuffer.allocate(BLOCK_SIZE);
            while(channel.read(buff) != -1)
            {
                buff.flip();
                requiredDigest.update(buff);
                buff.clear();
            }
        }
        fileCount++;
        fileBytes += attrs.size();

        // compute the hash
        return Optional.of(requiredDigest.digest());
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        logger.warning("Could not hash " + file);
        return FileVisitResult.CONTINUE;
    }

    public long getVerificationErrors() {
        return verificationErrors;
    }

    public long getOtherErrors() {
        return otherErrors;
    }

    public long getFileCount() {
        return fileCount;
    }

    public long getFileBytes() {
        return fileBytes;
    }
}
