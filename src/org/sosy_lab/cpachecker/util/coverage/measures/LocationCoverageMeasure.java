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
import java.util.Set;
import org.sosy_lab.cpachecker.util.coverage.util.CoverageColorUtil;

public class LocationCoverageMeasure implements CoverageMeasure {

  private final Multiset<Integer> coveredLocations;
  private final double maxCount;
  private String coverageColor;

  public LocationCoverageMeasure(
      Multiset<Integer> pCoveredLocations, double pMaxCount, String pCoverageColor) {
    this(pCoveredLocations, pMaxCount);
    coverageColor = pCoverageColor;
  }

  public LocationCoverageMeasure(
      Set<Integer> pCoveredLocations, double pMaxCount, String pCoverageColor) {
    this(pCoveredLocations, pMaxCount);
    coverageColor = pCoverageColor;
  }

  public LocationCoverageMeasure(Multiset<Integer> pCoveredLocations, double pMaxCount) {
    coveredLocations = pCoveredLocations;
    coverageColor = CoverageColorUtil.DEFAULT_COVERAGE_COLOR;
    if (pMaxCount <= 0) {
      maxCount = 1.0;
    } else {
      maxCount = pMaxCount;
    }
  }

  public LocationCoverageMeasure(Set<Integer> pCoveredLocations, double pMaxCount) {
    this(LinkedHashMultiset.create(pCoveredLocations), pMaxCount);
  }

  public LocationCoverageMeasure() {
    coveredLocations = LinkedHashMultiset.create();
    maxCount = 1;
  }

  public Set<Integer> getCoveredSet() {
    return coveredLocations.elementSet();
  }

  public Multiset<Integer> getCoveredLocations() {
    return coveredLocations;
  }

  public String getColor(Integer location) {
    if (getCoveredSet().contains(location)) {
      return coverageColor;
    } else {
      return CoverageColorUtil.DEFAULT_ELEMENT_COLOR;
    }
  }

  @Override
  public double getCoverage() {
    return coveredLocations.elementSet().size() / maxCount;
  }

  @Override
  public double getCount() {
    return coveredLocations.elementSet().size();
  }

  @Override
  public double getMaxCount() {
    return maxCount;
  }
}
