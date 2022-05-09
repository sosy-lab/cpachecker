// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.measures;

import com.google.common.collect.Multiset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.util.coverage.data.FileCoverageStatistics;
import org.sosy_lab.cpachecker.util.coverage.util.CoverageColorUtil;

/**
 * A coverage measure which is based on working with the source code lines. Therefore, the coverage
 * depends on the total relevant lines and all lines which are considered as covered depending on
 * concrete coverage criteria. The coverage criteria is applied during the data gathering and not
 * within this class. Data gathering is typically done after the analysis within the
 * CoverageCollector or during the analysis within a CoverageCPA.
 */
public class LineCoverageMeasure implements CoverageMeasure {
  /* ##### Class Fields ##### */
  private final Map<String, Multiset<Integer>> visitedLinesPerFile;
  private final Map<String, Set<Integer>> exisitingLinesPerFile;

  /* ##### Constructors ##### */
  public LineCoverageMeasure(Map<String, FileCoverageStatistics> infoPerFile) {
    visitedLinesPerFile = new LinkedHashMap<>();
    exisitingLinesPerFile = new LinkedHashMap<>();
    for (var info : infoPerFile.entrySet()) {
      visitedLinesPerFile.put(info.getKey(), info.getValue().visitedLines);
      exisitingLinesPerFile.put(info.getKey(), info.getValue().allLines);
    }
  }

  public LineCoverageMeasure() {
    visitedLinesPerFile = new LinkedHashMap<>();
    exisitingLinesPerFile = new LinkedHashMap<>();
  }

  /* ##### Getter and Setter ##### */
  /**
   * Returns the color representing if a line is covered or not. This information is used for later
   * visualization in the report.html Source Tab.
   *
   * @param file file location which also is used as identification to determine the right file
   * @param line line number of the corresponding file
   * @return hex color code which represents the coverage status for the given line
   */
  public String getColor(String file, int line) {
    if (visitedLinesPerFile.get(file).contains(line)) {
      return CoverageColorUtil.getFrequencyColorMap(visitedLinesPerFile.get(file)).get(line);
    } else if (exisitingLinesPerFile.get(file).contains(line)) {
      return CoverageColorUtil.DEFAULT_CONSIDERED_COLOR;
    } else {
      return CoverageColorUtil.DEFAULT_ELEMENT_COLOR;
    }
  }

  /* ##### Interface Implementations ##### */
  @Override
  public double getCoverage() {
    return getCount() / getMaxCount();
  }

  @Override
  public double getCount() {
    int visitedLinesCount = 0;
    for (var visitedLines : visitedLinesPerFile.values()) {
      visitedLinesCount += visitedLines.entrySet().size();
    }
    return visitedLinesCount;
  }

  @Override
  public double getMaxCount() {
    int existingLinesCount = 0;
    for (var existingLines : exisitingLinesPerFile.values()) {
      existingLinesCount += existingLines.size();
    }
    return existingLinesCount;
  }
}
