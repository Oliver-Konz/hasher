#!/bin/sh

 # Hasher - Hashes and verifies entire directory trees.
 # Copyright (C) 2014  Oliver Konz <code@oliverkonz.de>
 #
 # This program is free software: you can redistribute it and/or modify
 # it under the terms of the GNU General Public License as published by
 # the Free Software Foundation, either version 3 of the License, or
 # (at your option) any later version.
 #
 # This program is distributed in the hope that it will be useful,
 # but WITHOUT ANY WARRANTY; without even the implied warranty of
 # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 # GNU General Public License for more details.
 #
 # You should have received a copy of the GNU General Public License
 # along with this program.  If not, see <http://www.gnu.org/licenses/>.

JAR_NAME=hasher-1.0-SNAPSHOT-jar-with-dependencies.jar
JAR_DIR=/usr/local/share/hasher
BIN_DIR=/usr/local/bin

JAR=$JAR_DIR/$JAR_NAME
BIN=$BIN_DIR/hasher

mkdir -p $JAR_DIR
cp $JAR_NAME README.txt gpl-3.0.txt $JAR_DIR

echo "#!/bin/sh" > $BIN
echo "java -jar "$JAR >> $BIN
chmod 775 $BIN
