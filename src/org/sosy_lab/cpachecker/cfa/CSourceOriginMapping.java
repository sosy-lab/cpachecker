// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
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
   * Returns a map that contains an entry for each given file, where an entry is a list that maps
   * each line to its starting offset in the file. The lines are indexed starting with 0. The method
   * reads the given files from disk. So be aware of IO operations and potential failure when the
   * files can not be accessed.
   *
   * <p>For example, the first line has offset 0. If the length of the first line is 5 symbols, then
   * the second line has offset 5.
   *
   * @param filePaths Paths of the file to process
   * @return Immutable map
   * @throws IOException if the files can not be accessed.
   */
  private static ListMultimap<Path, Integer> getLineOffsetsByFile(Collection<Path> filePaths)
      throws IOException {
    ImmutableListMultimap.Builder<Path, Integer> result = ImmutableListMultimap.builder();

    for (Path filePath : filePaths) {
      if (Files.isRegularFile(filePath)) {
        String fileContent = Files.readString(filePath);

        int currentOffset = 0;
        List<String> sourceLines = Splitter.onPattern("\\n").splitToList(fileContent);
        for (String sourceLine : sourceLines) {
          result.put(filePath, currentOffset);
          currentOffset += sourceLine.length() + 1;
        }
      }
    }
    return result.build();
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

  public int getPositionStartingColumnStartLine(Path pAnalysisFileName, int pAnalysisCodeLine) {
    if (!lineNumberToStartingColumn.containsKey(pAnalysisFileName)) {
      try {
        lineNumberToStartingColumn.putAll(
            pAnalysisFileName,
            getLineOffsetsByFile(ImmutableList.of(pAnalysisFileName)).get(pAnalysisFileName));
      } catch (IOException e) {
        return -1;
      }
    }
    if (lineNumberToStartingColumn.get(pAnalysisFileName).size() <= pAnalysisCodeLine) {
      return -2;
    }

    return lineNumberToStartingColumn.get(pAnalysisFileName).get(pAnalysisCodeLine - 1);
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
