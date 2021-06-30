// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.model;

import com.google.errorprone.annotations.Immutable;

@Immutable
public class InvariantStoreEntryLocation {
  public final String fileName;
  public final String fileHash;
  public final int line;
  public final int column;
  public final String function;

  public InvariantStoreEntryLocation(
      String fileName, String fileHash, int line, int column, String function) {
    this.fileName = fileName;
    this.fileHash = fileHash;
    this.line = line;
    this.column = column;
    this.function = function;
  }

  public String getFileName() {
    return fileName;
  }

  public String getFileHash() {
    return fileHash;
  }

  public int getLine() {
    return line;
  }

  public int getColumn() {
    return column;
  }

  public String getFunction() {
    return function;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof InvariantStoreEntryLocation)) {
      return false;
    }
    InvariantStoreEntryLocation other = (InvariantStoreEntryLocation) o;
    return fileName.equals(other.fileName)
        && fileHash.equals(other.fileHash)
        && line == other.line
        && column == other.column
        && function.equals(other.function);
  }

  @Override
  public int hashCode() {
    int hashCode = fileName.hashCode();
    hashCode = 31 * hashCode + fileHash.hashCode();
    hashCode = 31 * hashCode + Integer.hashCode(line);
    hashCode = 31 * hashCode + Integer.hashCode(column);
    hashCode = 31 * hashCode + function.hashCode();
    return hashCode;
  }
}
