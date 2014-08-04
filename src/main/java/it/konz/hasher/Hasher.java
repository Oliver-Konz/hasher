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

import java.io.File;
import java.nio.file.FileSystems;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;
import java.util.List;
import java.util.logging.*;

/**
 * Hasher main class.
 *
 * Uses JCommander for arguments parsing.
 */
public class Hasher {

    public static final String EXECUTABLE_NAME = "hasher";

    public static final int STATUS_OK = 0;
    public static final int STATUS_HASH_ERROR = 1;
    public static final int STATUS_COMMAND_LINE_ERROR = 2;
    public static final int STATUS_MISSING_DIRECTORY = 3;
    public static final int STATUS_ALGORITHM_NOT_AVAILABLE = 4;
    public static final int STATUS_IO_ERROR = 5;

    private static final Logger logger = Logger.getLogger(Hasher.class.getName());

    private final BitSet mode;
    private final List<String> directories;
    private final Level logLevel;
    private final String algorithm;
    private final String hashFile;

    public static void main(String[] args) {
        Hasher hasher = new Hasher(CommandLine.readCommandLine(args));

        hasher.setupLogging();

        if (!hasher.checkDirectoriesExist()) {
            System.err.println("Could not find one of the directories. See log for details.");
            System.exit(STATUS_MISSING_DIRECTORY);
        }

        if (!hasher.checkAlgorithmExists()) {
            System.err.println(String.format("Hashing algorithm %s is not available. See log for details.", hasher.algorithm));
            System.exit(STATUS_ALGORITHM_NOT_AVAILABLE);
        }

        Stats stats = hasher.run();
        System.err.println(stats.toString());
        System.err.println();

        if (stats.getVerificationErrors() != 0) {
            System.err.println("There were verification errors. See log for details.");
            System.exit(STATUS_HASH_ERROR);
        }
        if (stats.getOtherErrors() != 0) {
            System.err.println("There were other errors (probably I/O errors) performing the operation. See log for details.");
            System.exit(STATUS_IO_ERROR);
        }

        System.err.println("Operation successful.");
        System.exit(STATUS_OK);
    }

    private Hasher(CommandLine commandLine) {
        mode = new BitSet();
        mode.set(Scanner.MODE_UPDATE, commandLine.isUpdate());
        mode.set(Scanner.MODE_VERIFY, commandLine.isVerify());
        directories = commandLine.getDirectories();
        logLevel = commandLine.getLogLevel();
        algorithm = commandLine.getAlgorithm();
        hashFile = commandLine.getHashFile();
    }

    private void setupLogging() {
        Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        logger.setLevel(logLevel);
    }

    private boolean checkDirectoriesExist() {
        for (String directoryString : directories) {
            File directoryFile = FileSystems.getDefault().getPath(directoryString).toFile();
            if (!directoryFile.exists()) {
                logger.severe(String.format("Directory %s does not exist!", directoryString));
                return false;
            }
            if (!directoryFile.isDirectory()) {
                logger.severe(String.format("%s is not a directory!", directoryString));
                return false;
            }
        }
        return true;
    }

    private boolean checkAlgorithmExists() {
        try {
            MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            logger.severe(e.getMessage());
            return false;
        }
        return true;
    }

    private Stats run() {
        Scanner scanner;
        try {
            scanner = new Scanner(mode, algorithm, hashFile);
        } catch (NoSuchAlgorithmException e) {
            // Cannot happen - we have checked this beforehand.
            throw new RuntimeException(e);
        }

        Stats stats = Stats.EMPTY;
        for (String directory : directories) {
            logger.info(String.format("%s %s...", scanner.getOperationName(), directory));
            stats = stats.add(scanner.scan(FileSystems.getDefault().getPath(directory)));
        }

        return stats;
    }

}
