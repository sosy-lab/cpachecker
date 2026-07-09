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

public class CSourceOriginMapping {

  // Each RangeMap in this map contains the mapping for one input file,
  // from its lines to the tuple of (originalFile, lineDelta).
  // The full mapping is a map with those RangeMaps as values,
  // one for each input file.
  // All file paths are always pre-fixed with a `./` in case they are not absolute paths.
  private final Map<Path, RangeMap<Integer, CodePosition>> mapping = new HashMap<>();

  // For each file (identified by its path, always pre-fixed with a `./` in case it is not an
  // absolute path), we store a list of starting offsets for each line. This allows us to compute
  // the column number for a given offset.
  private final ListMultimap<Path, Integer> lineNumberToStartingOffset = ArrayListMultimap.create();

  void mapInputLineRangeToDelta(
      Path pAnalysisFileName,
      Path pOriginFileName,
      int pFromAnalysisCodeLineNumber,
      int pToAnalysisCodeLineNumber,
      int pLineDeltaToOrigin) {
    Path normalizedAnalysisFileName = normalizePathForLookup(pAnalysisFileName);
    RangeMap<Integer, CodePosition> fileMapping = mapping.get(normalizedAnalysisFileName);
    if (fileMapping == null) {
      fileMapping = TreeRangeMap.create();
      mapping.put(normalizedAnalysisFileName, fileMapping);
    }

    Range<Integer> lineRange =
        Range.closedOpen(pFromAnalysisCodeLineNumber, pToAnalysisCodeLineNumber);
    fileMapping.put(lineRange, new CodePosition(pOriginFileName, pLineDeltaToOrigin));
  }

  /**
   * Convert paths like "file.c" to "./file.c", and return all other paths unchanged. We need some
   * type of normalization, since our Eclipse based parser for C programs returns relative paths
   * with "./", while other parts of CPAchecker do not use "./" for relative paths.
   *
   * <p>Each time a lookup in a data structure of this class is done with a path, this method should
   * be applied to the path first.
   */
  private static Path normalizePathForLookup(Path path) {
    if (!path.toString().isEmpty() && !path.isAbsolute() && path.getParent() == null) {
      return Path.of(".").resolve(path);
    }
    return path;
  }

  /**
   * Adds information about the relation between line numbers and offsets for the given path. This
   * is tracked by a mapping from paths to A list where for entry i the starting offset of line i in
   * the file is stored.
   *
   * @param pPath the path from where the program code stems
   * @param pProgramCode code for the file whose line starting offsets should be computed
   */
  public void addFileInformation(Path pPath, String pProgramCode) {
    ImmutableList.Builder<Integer> result = ImmutableList.builder();

    int currentOffset = 0;
    List<String> sourceLines = Splitter.onPattern("\\n").splitToList(pProgramCode);
    for (String sourceLine : sourceLines) {
      result.add(currentOffset);
      currentOffset += sourceLine.length() + 1;
    }

    lineNumberToStartingOffset.putAll(normalizePathForLookup(pPath), result.build());
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
    RangeMap<Integer, CodePosition> fileMapping =
        mapping.get(normalizePathForLookup(pAnalysisFileName));

    if (fileMapping != null) {
      CodePosition originFileAndLineDelta = fileMapping.get(pAnalysisCodeLine);

      if (originFileAndLineDelta != null) {
        return originFileAndLineDelta.addToLineNumber(pAnalysisCodeLine);
      }
    }
    return new CodePosition(pAnalysisFileName, pAnalysisCodeLine);
  }

  public int getStartColumn(Path pAnalysisFileName, int pAnalysisCodeLine, int pOffset) {
    Path normalizedPath = normalizePathForLookup(pAnalysisFileName);
    // This should only happen when parsing an automaton file. In those cases the file is called
    // 'fragment' since usually only a fragment of the automaton contains C-code.
    if (!lineNumberToStartingOffset.containsKey(normalizedPath)) {
      Verify.verify(
          // For automata files
          normalizedPath.toString().equals("./fragment")
              // For parsing expressions stemming from witnesses
              || normalizedPath.toString().equals("./#expr#"));
      // Till now, we only have fragments with one line of code.
      Verify.verify(pAnalysisCodeLine == 1);
      return pOffset;
    }

    Verify.verify(lineNumberToStartingOffset.get(normalizedPath).size() >= pAnalysisCodeLine);

    // Since the offsets start at 0 there is a one-off difference between the column and the
    // offset
    return pOffset - lineNumberToStartingOffset.get(normalizedPath).get(pAnalysisCodeLine - 1) + 1;
  }

  /**
   * If true, there exists no mapping from an analysis file to an origin file and hence each line
   * number will be mapped to its identical value.
   */
  public boolean isMappingToIdenticalLineNumbers() {
    return mapping.isEmpty();
  }

  /** Code position in terms of file name and absolute or relative line number. */
  public record CodePosition(Path fileName, int lineNumber) {

    public CodePosition withFileName(Path pFileName) {
      return new CodePosition(pFileName, lineNumber);
    }

    public CodePosition addToLineNumber(int pDelta) {
      return new CodePosition(fileName, lineNumber + pDelta);
    }
  }
}
