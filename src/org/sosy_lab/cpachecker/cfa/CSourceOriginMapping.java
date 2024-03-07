// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import com.google.common.base.Splitter;
import com.google.common.base.Verify;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CSourceOriginMapping {

  // Each RangeMap in this map contains the mapping for one input file,
  // from its lines to the tuple of (originalFile, lineDelta).
  // The full mapping is a map with those RangeMaps as values,
  // one for each input file.
  private final Map<Path, RangeMap<Integer, CodePosition>> mapping = new HashMap<>();

  private final ListMultimap<Path, Integer> lineNumberToStartingColumn = ArrayListMultimap.create();

  void mapInputLineRangeToDelta(
      Path pAnalysisFileName,
      Path pOriginFileName,
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
   * Returns a list where for entry i the starting offset of line i in the file is stored.
   *
   * @param pProgramCode code for the file whose line starting offsets should be computed
   * @return Immutable List
   */
  private static ImmutableList<Integer> getLineOffsets(String pProgramCode) {
    ImmutableList.Builder<Integer> result = ImmutableList.builder();

    int currentOffset = 0;
    List<String> sourceLines = Splitter.onPattern("\\n").splitToList(pProgramCode);
    for (String sourceLine : sourceLines) {
      result.add(currentOffset);
      currentOffset += sourceLine.length() + 1;
    }
    return result.build();
  }

  public void addFileInformation(Path pPath, String pProgramCode) {
    lineNumberToStartingColumn.putAll(pPath, getLineOffsets(pProgramCode));
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
      Path pAnalysisFileName, int pAnalysisCodeLine) {
    RangeMap<Integer, CodePosition> fileMapping = mapping.get(pAnalysisFileName);

    if (fileMapping != null) {
      CodePosition originFileAndLineDelta = fileMapping.get(pAnalysisCodeLine);

      if (originFileAndLineDelta != null) {
        return originFileAndLineDelta.addToLineNumber(pAnalysisCodeLine);
      }
    }
    return CodePosition.of(pAnalysisFileName, pAnalysisCodeLine);
  }

  public int getStartColumn(Path pAnalysisFileName, int pAnalysisCodeLine, int pOffset) {
    // This should only happen when parsing an automaton file. In those cases the file is called
    // 'fragment' since usually only a fragment of the automaton contains C-code.
    if (!lineNumberToStartingColumn.containsKey(pAnalysisFileName)) {
      Verify.verify(pAnalysisFileName.toString().equals("fragment"));
      // Till now, we only have fragments with one line of code.
      Verify.verify(pAnalysisCodeLine == 1);
      return pOffset;
    }

    Verify.verify(lineNumberToStartingColumn.get(pAnalysisFileName).size() > pAnalysisCodeLine);

    // Since the offsets start at 0 there is a one-off difference between the column and the
    // offset
    return pOffset
        - lineNumberToStartingColumn.get(pAnalysisFileName).get(pAnalysisCodeLine - 1)
        + 1;
  }

  /**
   * If true, there exists no mapping from an analysis file to an origin file and hence each line
   * number will be mapped to its identical value.
   */
  public boolean isMappingToIdenticalLineNumbers() {
    return mapping.isEmpty();
  }

  /** Code position in terms of file name and absolute or relative line number. */
  public static class CodePosition {

    private final Path fileName;

    private final int lineNumber;

    private CodePosition(Path pFileName, int pLineNumber) {
      fileName = pFileName;
      lineNumber = pLineNumber;
    }

    public Path getFileName() {
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
      return pObj instanceof CodePosition other
          && lineNumber == other.lineNumber
          && fileName.equals(other.fileName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(fileName, lineNumber);
    }

    public CodePosition withFileName(Path pFileName) {
      return of(pFileName, lineNumber);
    }

    public CodePosition addToLineNumber(int pDelta) {
      return of(fileName, lineNumber + pDelta);
    }

    public static CodePosition of(Path pFileName, int pLineNumber) {
      return new CodePosition(pFileName, pLineNumber);
    }
  }
}
