// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.measures;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multiset;
import java.util.Map;
import org.sosy_lab.cpachecker.util.coverage.util.CoverageColorUtil;

/**
 * A coverage measure which is based on working with the source code lines. Therefore, the coverage
 * depends on the total relevant lines and all lines which are considered as covered depending on
 * concrete coverage criteria. The coverage criteria is applied during the data gathering and not
 * within this class. Data gathering is typically done after the analysis within the
 * CoverageCollector or during the analysis within a CoverageCPA.
 */
public class LineBasedCoverageMeasure implements CoverageMeasure {
  private final ImmutableMap<String, ImmutableMultiset<Integer>> visitedLinesPerFile;
  private final ImmutableMap<String, ImmutableSet<Integer>> existingLinesPerFile;

  public LineBasedCoverageMeasure(
      Map<String, ImmutableMultiset<Integer>> pVisitedLinesPerFile,
      Map<String, ImmutableSet<Integer>> pExistingLinesPerFile) {
    visitedLinesPerFile = ImmutableSortedMap.copyOf(pVisitedLinesPerFile);
    existingLinesPerFile = ImmutableSortedMap.copyOf(pExistingLinesPerFile);
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
    Multiset<Integer> visitedLines = visitedLinesPerFile.get(file);
    ImmutableSet<Integer> existingLines = existingLinesPerFile.get(file);
    if (visitedLines != null && visitedLines.contains(line)) {
      return CoverageColorUtil.getFrequencyColorMapForLines(visitedLinesPerFile.get(file))
          .get(line);
    } else if (existingLines != null && existingLines.contains(line)) {
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
    return existingLinesPerFile.values().stream()
        .mapToInt(existingLines -> existingLines.size())
        .sum();
  }
}
