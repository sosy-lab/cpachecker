// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.measures;

import com.google.common.collect.Multiset;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.util.coverage.util.CoverageColorUtil;

public class MultiLocationCoverageMeasure extends LocationCoverageMeasure {
  private final Multiset<Integer> alternativeCoveredLocations;

  public MultiLocationCoverageMeasure(
      Multiset<Integer> pCoveredLocations,
      Multiset<Integer> pAlternativeCoveredLocations,
      double pMaxCount) {
    super(pCoveredLocations, pMaxCount);
    alternativeCoveredLocations = pAlternativeCoveredLocations;
  }

  public Set<Integer> getIntersectionLocations() {
    Set<Integer> intersect = new HashSet<>(getCoveredLocations().elementSet());
    intersect.removeAll(alternativeCoveredLocations.elementSet());
    return intersect;
  }

  public Set<Integer> getAlternativeCoveredSet() {
    return alternativeCoveredLocations.elementSet();
  }

  @Override
  public String getColor(Integer location) {
    if (getIntersectionLocations().contains(location)) {
      return CoverageColorUtil.DEFAULT_CONSIDERED_COLOR;
    } else if (getAlternativeCoveredSet().contains(location)) {
      return CoverageColorUtil.getFrequencyColorMap(alternativeCoveredLocations).get(location);
    } else {
      return CoverageColorUtil.DEFAULT_ELEMENT_COLOR;
    }
  }
}
