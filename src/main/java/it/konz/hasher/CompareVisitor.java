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
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Compares the hash files in directory structures.
 */
public class CompareVisitor implements FileVisitor<Path> {

    private static final Logger logger = Logger.getLogger(CompareVisitor.class.getName());

    private Scanner scanner;
    private final Map<Path, Map<String, HashEntry>> hashFiles = new HashMap<>();
    private long compareErrors = 0L;
    private long otherErrors = 0L;

    public CompareVisitor(final Scanner scanner) {
        this.scanner = scanner;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
        Path hashFilePath = dir.resolve(scanner.getHashFileName());
        File hashFile = hashFilePath.toFile();
        Map<String, HashEntry> hashEntries = new HashMap<>();

        if (hashFile.exists()) {
            otherErrors += HashEntry.parseHashesFile(hashFilePath, hashEntries);
        } else {
            logger.info("Unhashed directory: " + dir.toString());
        }
        hashFiles.put(dir, hashEntries);

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }
}
