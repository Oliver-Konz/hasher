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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Command line parameters.
 */
public class CommandLine {

    @Parameter(description = "The directories to update/verify", required = true)
    private List<String> directories = new ArrayList<>();

    @Parameter(names = {"--update", "-u"}, description = "Create or update hashes")
    private boolean update = false;

    @Parameter(names = {"--verify", "-v"}, description = "Verify hashes")
    private boolean verify = false;

    @Parameter(names = {"--help", "-h"}, description = "Show help", help = true)
    private boolean help = false;

    @Parameter(names = {"--loglevel", "-l"}, description = "How much to log (FINE|INFO|WARNING|SEVERE|OFF)")
    private String logLevel = "INFO";

    @Parameter(names = {"--algorithm", "-a"}, description = "The hashing algorithm (MD5, SHA-1, SHA-256, ...) depending on the JVM.")
    private String algorithm = "MD5";

    @Parameter(names = {"--hashfile", "-f"}, description = "The name of the file containing the hashes.")
    private String hashFile = ".hashes";

    public List<String> getDirectories() {
        return directories;
    }

    public boolean isUpdate() {
        return update;
    }

    public boolean isVerify() {
        return verify;
    }

    public boolean isHelp() {
        return help;
    }

    public String getLogLevelString() {
        return logLevel;
    }

    public Level getLogLevel() {
        return Level.parse(logLevel);
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getHashFile() {
        return hashFile;
    }

    public static CommandLine readCommandLine(String[] args) {
        CommandLine commandLine = new CommandLine();
        JCommander jc = new JCommander(commandLine);
        jc.setProgramName(Hasher.EXECUTABLE_NAME);

        try {
            jc.parse(args);
        } catch (ParameterException px) {
            System.out.println(px.getMessage() + '\n');
            jc.usage();
            System.exit(Hasher.STATUS_COMMAND_LINE_ERROR);
        }

        if (commandLine.isHelp()) {
            jc.usage();
            System.exit(Hasher.STATUS_OK);
        }

        if (!(commandLine.isUpdate() || !commandLine.isVerify())) {
            System.out.println("Use either --update or --verify\n");
            jc.usage();
            System.exit(Hasher.STATUS_COMMAND_LINE_ERROR);
        }

        return commandLine;
    }

}
