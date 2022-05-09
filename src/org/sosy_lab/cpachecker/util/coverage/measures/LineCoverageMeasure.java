// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.measures;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.util.coverage.report.FileCoverageStatistics;
import org.sosy_lab.cpachecker.util.coverage.util.CoverageColorUtil;

public class LineCoverageMeasure implements CoverageMeasure {
  private final Map<String, Multiset<Integer>> visitedLinesPerFile;
  private final double maxCount;

  public LineCoverageMeasure(Map<String, FileCoverageStatistics> infoPerFile, double pMaxCount) {
    visitedLinesPerFile = new LinkedHashMap<>();
    for (var info : infoPerFile.entrySet()) {
      visitedLinesPerFile.put(info.getKey(), info.getValue().visitedLines);
    }
    if (pMaxCount <= 0) {
      maxCount = 1.0;
    } else {
      maxCount = pMaxCount;
    }
  }

  public LineCoverageMeasure() {
    visitedLinesPerFile = new LinkedHashMap<>();
    maxCount = 1;
  }

  public String getColor(String file, int line) {
    return CoverageColorUtil.getFrequencyColorMap(visitedLinesPerFile.get(file)).get(line);
  }

  @Override
  public double getCoverage() {
    return getValue() / maxCount;
  }

  @Override
  public double getValue() {
    int visitedLinesCount = 0;
    for (var visitedLines : visitedLinesPerFile.values()) {
      visitedLinesCount += visitedLines.entrySet().size();
    }
    return visitedLinesCount;
  }

  @Override
  public double getMaxCount() {
    return maxCount;
  }
}
