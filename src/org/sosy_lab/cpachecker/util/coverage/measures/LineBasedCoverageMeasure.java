// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.measures;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multiset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
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
public class LineBasedCoverageMeasure implements CoverageMeasure {
  private final ImmutableMap<String, Multiset<Integer>> visitedLinesPerFile;
  private final ImmutableMap<String, Set<Integer>> exisitingLinesPerFile;

  public LineBasedCoverageMeasure(Map<String, FileCoverageStatistics> infoPerFile) {
    Map<String, Multiset<Integer>> lVisitedLinesPerFile = new LinkedHashMap<>();
    Map<String, Set<Integer>> lExistingLinesPerFile = new LinkedHashMap<>();
    for (var entry : infoPerFile.entrySet()) {
      lVisitedLinesPerFile.put(entry.getKey(), entry.getValue().visitedLines);
      lExistingLinesPerFile.put(entry.getKey(), entry.getValue().allLines);
    }
    visitedLinesPerFile = ImmutableSortedMap.copyOf(lVisitedLinesPerFile);
    exisitingLinesPerFile = ImmutableSortedMap.copyOf(lExistingLinesPerFile);
  }

  /**
   * Returns the color representing if a line is covered or not. This information is used for later
   * visualization in the report.html Source Tab.
   *
   * @param file file location which also is used as identification to determine the right file
   * @param line line number of the corresponding file
   * @return hex color code which represents the coverage status for the given line
   */
  public String getColor(String file, int line) {
    if (Objects.requireNonNull(visitedLinesPerFile.get(file)).contains(line)) {
      return CoverageColorUtil.getFrequencyColorMapForLines(visitedLinesPerFile.get(file))
          .get(line);
    } else if (Objects.requireNonNull(exisitingLinesPerFile.get(file)).contains(line)) {
      return CoverageColorUtil.DEFAULT_CONSIDERED_COLOR;
    } else {
      return CoverageColorUtil.DEFAULT_ELEMENT_COLOR;
    }
  }

  @Override
  public double getNormalizedValue() {
    return getValue() / getMaxValue();
  }

  @Override
  public double getValue() {
    return visitedLinesPerFile.values().stream()
        .mapToInt(visitedLines -> visitedLines.entrySet().size())
        .sum();
  }

  @Override
  public double getMaxValue() {
    return exisitingLinesPerFile.values().stream()
        .mapToInt(existingLines -> existingLines.size())
        .sum();
  }
}
