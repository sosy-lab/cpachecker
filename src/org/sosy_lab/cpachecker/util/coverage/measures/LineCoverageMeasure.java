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
import org.sosy_lab.cpachecker.util.coverage.report.FileCoverageStatistics;
import org.sosy_lab.cpachecker.util.coverage.util.CoverageColorUtil;

public class LineCoverageMeasure implements CoverageMeasure {
  private final Map<String, Multiset<Integer>> visitedLinesPerFile;
  private final Map<String, Set<Integer>> exisitingLinesPerFile;

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

  public String getColor(String file, int line) {
    if (visitedLinesPerFile.get(file).contains(line)) {
      return CoverageColorUtil.getFrequencyColorMap(visitedLinesPerFile.get(file)).get(line);
    } else if (exisitingLinesPerFile.get(file).contains(line)) {
      return CoverageColorUtil.DEFAULT_CONSIDERED_COLOR;
    } else {
      return CoverageColorUtil.DEFAULT_ELEMENT_COLOR;
    }
  }

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
