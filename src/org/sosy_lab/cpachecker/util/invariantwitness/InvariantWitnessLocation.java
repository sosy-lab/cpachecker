// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness;

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
}
