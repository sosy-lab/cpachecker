// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;

@Immutable
public class LocationRecord {
  @JsonProperty("file_name")
  private final String fileName;

  @JsonProperty("file_hash")
  private final String fileHash;

  @JsonProperty("line")
  private final int line;

  @JsonProperty("column")
  private final int column;

  @JsonProperty("function")
  private final String function;

  public LocationRecord(
      @JsonProperty("file_name") String fileName,
      @JsonProperty("file_hash") String fileHash,
      @JsonProperty("line") int line,
      @JsonProperty("column") int column,
      @JsonProperty("function") String function) {
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
    if (!(o instanceof LocationRecord)) {
      return false;
    }
    LocationRecord other = (LocationRecord) o;
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

  @Override
  public String toString() {
    return "LocationRecord{"
        + " fileName='"
        + getFileName()
        + "'"
        + ", fileHash='"
        + getFileHash()
        + "'"
        + ", line='"
        + getLine()
        + "'"
        + ", column='"
        + getColumn()
        + "'"
        + ", function='"
        + getFunction()
        + "'"
        + "}";
  }
}
