// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness;

import com.google.errorprone.annotations.Immutable;

@Immutable
public class InvariantWitnessLocation {
  private final String fileName;
  private final String fileHash;
  private final String functionName;
  private final int line;
  private final int column;

  InvariantWitnessLocation(
      String fileName, String fileHash, String functionName, int line, int column) {
    this.fileName = fileName;
    this.fileHash = fileHash;
    this.functionName = functionName;
    this.line = line;
    this.column = column;
  }

  public String getFileName() {
    return fileName;
  }

  public String getFileHash() {
    return fileHash;
  }

  public String getFunctionName() {
    return functionName;
  }

  public int getLine() {
    return line;
  }

  public int getColumn() {
    return column;
  }

  @Override
  public int hashCode() {
    int hashCode = fileName.hashCode();
    hashCode = 31 * hashCode + fileHash.hashCode();
    hashCode = 31 * hashCode + functionName.hashCode();
    hashCode = 31 * hashCode + Integer.hashCode(line);
    hashCode = 31 * hashCode + Integer.hashCode(column);
    return hashCode;
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) {
      return true;
    }

    if (!(pObj instanceof InvariantWitnessLocation)) {
      return false;
    }
    InvariantWitnessLocation other = (InvariantWitnessLocation) pObj;

    return fileName.equals(other.fileName)
        && fileHash.equals(other.fileHash)
        && functionName.equals(other.functionName)
        && line == other.line
        && column == other.column;
  }

  @Override
  public String toString() {
    return "Line " + line + ":" + column + " (" + functionName + ")";
  }
}
