/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class CSourceOriginMapping {

  // Each RangeMap in this map contains the mapping for one input file,
  // from its lines to the tuple of (originalFile, lineDelta).
  // The full mapping is a map with those RangeMaps as values,
  // one for each input file.
  private final Map<String, RangeMap<Integer, CodePosition>> mapping = new HashMap<>();

  void mapInputLineRangeToDelta(
      String pAnalysisFileName,
      String pOriginFileName,
      int pFromAnalysisCodeLineNumber,
      int pToAnalysisCodeLineNumber,
      int pLineDeltaToOrigin) {
    RangeMap<Integer, CodePosition> fileMapping = mapping.get(pAnalysisFileName);
    if (fileMapping == null) {
      fileMapping = TreeRangeMap.create();
      mapping.put(pAnalysisFileName, fileMapping);
    }

    Range<Integer> lineRange =
        Range.closedOpen(pFromAnalysisCodeLineNumber, pToAnalysisCodeLineNumber);
    fileMapping.put(lineRange, CodePosition.of(pOriginFileName, pLineDeltaToOrigin));
  }

  /**
   * Given a line number and file name for the analyzed code, retrieve the corresponding file name
   * and line number in the original code (e.g., before preprocessing).
   *
   * @param pAnalysisFileName the name of the analyzed file.
   * @param pAnalysisCodeLine the line number in the analyzed file.
   * @return the corresponding file name and line number in the original code (e.g., before
   *     preprocessing).
   */
  public CodePosition getOriginLineFromAnalysisCodeLine(
      String pAnalysisFileName, int pAnalysisCodeLine) {
    RangeMap<Integer, CodePosition> fileMapping = mapping.get(pAnalysisFileName);

    if (fileMapping != null) {
      CodePosition originFileAndLineDelta = fileMapping.get(pAnalysisCodeLine);

      if (originFileAndLineDelta != null) {
        return originFileAndLineDelta.addToLineNumber(pAnalysisCodeLine);
      }
    }
    return CodePosition.of(pAnalysisFileName, pAnalysisCodeLine);
  }

  /** Code position in terms of file name and absolute or relative line number. */
  public static class CodePosition {

    private final String fileName;

    private final int lineNumber;

    private CodePosition(String pFileName, int pLineNumber) {
      fileName = pFileName;
      lineNumber = pLineNumber;
    }

    public String getFileName() {
      return fileName;
    }

    public int getLineNumber() {
      return lineNumber;
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (pObj instanceof CodePosition) {
        CodePosition other = (CodePosition) pObj;
        return lineNumber == other.lineNumber && fileName.equals(other.fileName);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(fileName, lineNumber);
    }

    public CodePosition withFileName(String pFileName) {
      return of(pFileName, lineNumber);
    }

    public CodePosition addToLineNumber(int pDelta) {
      return of(fileName, lineNumber + pDelta);
    }

    public static CodePosition of(String pFileName, int pLineNumber) {
      return new CodePosition(pFileName, pLineNumber);
    }

  }
}
