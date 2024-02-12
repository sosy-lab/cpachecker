// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Objects;

@Immutable
public class LocationRecord {
  @JsonProperty("file_name")
  private final String fileName;

  @JsonProperty("file_hash")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final String fileHash;

  @JsonProperty("line")
  private final int line;

  @JsonProperty("column")
  private final int column;

  @JsonProperty("function")
  @JsonInclude(JsonInclude.Include.NON_NULL)
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
    if (this == o) {
      return true;
    }
    return o instanceof LocationRecord other
        && fileName.equals(other.fileName)
        && Objects.equals(fileHash, other.fileHash)
        && line == other.line
        && column == other.column
        && Objects.equals(function, other.function);
  }

  @Override
  public int hashCode() {
    int hashCode = fileName.hashCode();
    hashCode = 31 * hashCode + (fileHash != null ? fileHash.hashCode() : 0);
    hashCode = 31 * hashCode + Integer.hashCode(line);
    hashCode = 31 * hashCode + Integer.hashCode(column);
    hashCode = 31 * hashCode + (function != null ? function.hashCode() : 0);
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
