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
import org.sosy_lab.cpachecker.util.coverage.util.CoverageColorUtil;

public class LineCoverageMeasure implements CoverageMeasure {
  private final Multiset<Integer> visitedLines;
  private final double maxCount;

  public LineCoverageMeasure(Multiset<Integer> pVisitedLines, double pMaxCount) {
    visitedLines = pVisitedLines;
    if (pMaxCount <= 0) {
      maxCount = 1.0;
    } else {
      maxCount = pMaxCount;
    }
  }

  public LineCoverageMeasure() {
    visitedLines = LinkedHashMultiset.create();
    maxCount = 1;
  }

  public Multiset<Integer> getVisitedMultiSet() {
    return visitedLines;
  }

  public String getColor(String file, int line) {
    // TODO: separate for each file
    if (file.equals("")) return CoverageColorUtil.DEFAULT_ELEMENT_COLOR;
    return CoverageColorUtil.getFrequencyColorMap(visitedLines).get(line);
  }

  @Override
  public double getCoverage() {
    return visitedLines.entrySet().size() / maxCount;
  }

  @Override
  public double getValue() {
    return visitedLines.entrySet().size();
  }

  @Override
  public double getMaxCount() {
    return maxCount;
  }
}
