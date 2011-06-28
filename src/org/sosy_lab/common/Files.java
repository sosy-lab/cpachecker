/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Provides helper functions for file access.
 */
public final class Files {

  private Files() { /* utility class */ }

  /**
   * Creates a temporary file with an optional content. The file is marked for
   * deletion when the Java VM exits.
   * @param  prefix     The prefix string to be used in generating the file's
   *                    name; must be at least three characters long
   * @param  suffix     The suffix string to be used in generating the file's
   *                    name; may be <code>null</code>, in which case the
   *                    suffix <code>".tmp"</code> will be used
   * @param content The content to write (may be null).
   *
   * @throws  IllegalArgumentException
   *          If the <code>prefix</code> argument contains fewer than three
   *          characters
   * @throws  IOException  If a file could not be created
   */
  public static File createTempFile(String prefix, String suffix, String content) throws IOException {
    File file = File.createTempFile(prefix, suffix);
    file.deleteOnExit();

    if (!Strings.isNullOrEmpty(content)) {
      try {
        com.google.common.io.Files.write(content, file, Charset.defaultCharset());
      } catch (IOException e) {
        file.delete();

        throw e;
      }
    }
    return file;
  }

  /**
   * Writes content to a file.
   * @param file The file.
   * @param content The content which shall be written.
   * @throws IOException
   */
  public static void writeFile(File file, Object content) throws IOException {
    com.google.common.io.Files.createParentDirs(file);
    com.google.common.io.Files.write(content.toString(), file, Charset.defaultCharset());
  }

  /**
   * Writes content to a file.
   * @param file The file.
   * @param content The content which will be written to the end of the file.
   * @throws IOException
   */
  public static void appendToFile(File file, Object content) throws IOException {
    com.google.common.io.Files.append(content.toString(), file, Charset.defaultCharset());
  }

  /**
   * Verifies if a file exists, is a normal file and is readable. If this is not
   * the case, a FileNotFoundException with a nice message is thrown.
   *
   * @param file The file to check.
   * @throws FileNotFoundException If one of the conditions is not true.
   */
  public static void checkReadableFile(File file) throws FileNotFoundException {
    Preconditions.checkNotNull(file);

    if (!file.exists()) {
      throw new FileNotFoundException("File " + file.getAbsolutePath() + " does not exist!");
    }

    if (!file.isFile()) {
      throw new FileNotFoundException("File " + file.getAbsolutePath() + " is not a normal file!");
    }

    if (!file.canRead()) {
      throw new FileNotFoundException("File " + file.getAbsolutePath() + " is not readable!");
    }
  }
}