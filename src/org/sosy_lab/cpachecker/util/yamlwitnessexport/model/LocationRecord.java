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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;

@Immutable
public class LocationRecord {
  @JsonProperty("file_name")
  private final String fileName;

  @JsonProperty("line")
  private final int line;

  @JsonProperty("column")
  private final int column;

  @JsonProperty("function")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final String function;

  public LocationRecord(
      @JsonProperty("file_name") String pFileName,
      @JsonProperty("line") int pLine,
      @JsonProperty("column") int pColumn,
      @JsonProperty("function") String pFunction) {
    fileName = pFileName;
    line = pLine;
    column = pColumn;
    function = pFunction;
  }

  public static LocationRecord createLocationRecordAtStart(
      FileLocation location, String functionName) {
    return createLocationRecordAtStart(location, location.getFileName().toString(), functionName);
  }

  public static LocationRecord createLocationRecordAtStart(
      FileLocation location, String fileName, String functionName) {
    final int lineNumber = location.getStartingLineInOrigin();

    return new LocationRecord(fileName, lineNumber, location.getStartColumnInLine(), functionName);
  }

  public static LocationRecord createLocationRecordAtEnd(
      FileLocation pLocation, String pFunctionName) {
    return createLocationRecordAtEnd(pLocation, pLocation.getFileName().toString(), pFunctionName);
  }

  public static LocationRecord createLocationRecordAtEnd(
      FileLocation pLocation, String pFileName, String pFunctionName) {
    final int lineNumber = pLocation.getEndingLineNumber();
    return new LocationRecord(pFileName, lineNumber, pLocation.getEndColumnInLine(), pFunctionName);
  }

  public static LocationRecord createLocationRecordAtStartOfNextLocation(
      FileLocation fLoc, String functionName, AstCfaRelation pAstCfaRelation) {
    final String fileName = fLoc.getFileName().toString();
    FileLocation nextStatementFileLocation =
        pAstCfaRelation.nextStartStatementLocation(fLoc.getNodeOffset() + fLoc.getNodeLength());

    return LocationRecord.createLocationRecordAtStart(
        nextStatementFileLocation, fileName, functionName);
  }

  public String getFileName() {
    return fileName;
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
        && line == other.line
        && column == other.column
        && Objects.equals(function, other.function);
  }

  @Override
  public int hashCode() {
    int hashCode = fileName.hashCode();
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
