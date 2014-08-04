Hasher 1.0.1 - Hashes and verifies entire directory trees.
Copyright (C) 2014  Oliver Konz <code@oliverkonz.de>

Hashes the files in the given directories and puts the hash sums in the file '.hashes' in each subdirectory.
At a later point you can run this tool again and see if hashes have changed that should not have. An error is only
reported if the file modification date and file size stayed the same but the hash changed. If you use a file system that
already stores hash sums of it's files, you do not need this tool. I use it to ensure the integrity of my backups.

Run 'hashes --help' for a list of all command line options.

This utility is written in Java 8, so you need a Java 8 Runtime Environment (JRE) on your machine. It was developed and
tested with Linux. Other OSes should also work but have not been tested.

Hasher allows you to use any message digest that is available in your JRE to hash your files - at least MD5, SHA-1 and
SHA-256. You can extend this with libraries like Bounce Castle.
By default it uses MD5, which should be OK since we just want to know if the files were damaged. If you think someone
might actively tamper with your files SHA-256 would be a better alternative.

The latest version of hasher is available on GitHub: https://github.com/Oliver-Konz/hasher

License:
--------
Hasher is licensed under GPLv3. For details see the file LICENSE.md.

Compile:
--------
Install Java 8 and Apache Maven on your system. Then change to the hasher folder and run 'mvn clean install'. This will
compile hasher in the subdirectory 'target'.

Install:
--------
You can then run ./install.sh with root privileges, which will copy hasher to /usr/local/share/hasher and create the
launch script /usr/local/bin/hasher.

Ideas for future versions:
--------------------------
* Functionality to compare hash files from two or more directory trees
* Multi-Threading - Currently hasher runs single threaded: For HDDs IO is the limiting factor on my systems. I think
  my SSDs could read a bit faster than the CPU creates MD5 hashes. So for fast IO or other hashing algorithms, multi-
  threading could be beneficial.
* Nicer logging / output / help
* I18n - But is this really necessary for a simple CLI util?
