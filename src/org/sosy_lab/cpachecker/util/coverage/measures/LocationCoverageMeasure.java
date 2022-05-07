// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.measures;

import java.util.HashSet;
import java.util.Set;

public class LocationCoverageMeasure implements CoverageMeasure {

  private final Set<Integer> coveredLocations;
  private final double maxCount;

  public LocationCoverageMeasure(Set<Integer> pCoveredLocations, double pMaxCount) {
    coveredLocations = pCoveredLocations;
    if (pMaxCount <= 0) {
      maxCount = 1.0;
    } else {
      maxCount = pMaxCount;
    }
  }

  public LocationCoverageMeasure() {
    coveredLocations = new HashSet<>();
    maxCount = 1;
  }

  public Set<Integer> getCoveredSet() {
    return coveredLocations;
  }

  @Override
  public double getCoverage() {
    return coveredLocations.size() / maxCount;
  }

  @Override
  public double getValue() {
    return coveredLocations.size();
  }

  @Override
  public double getMaxCount() {
    return maxCount;
  }
}
