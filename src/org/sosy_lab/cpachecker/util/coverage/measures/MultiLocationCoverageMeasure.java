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

/**
 * Behaves like LocationCoverageMeasure but with the difference that it considers a second coverage
 * criteria. For example when looking at all reached locations and all visited locations, we can use
 * this MultiLocationCoverageMeasure to consider both coverage criteria and i.e. calculate the
 * intersection and use this as combined coverage criteria.
 */
public class MultiLocationCoverageMeasure extends LocationCoverageMeasure {
  /* ##### Class fields ##### */
  private final Multiset<Integer> alternativeCoveredLocations;

  /* ##### Constructors ##### */
  public MultiLocationCoverageMeasure(
      Multiset<Integer> pCoveredLocations,
      Multiset<Integer> pAlternativeCoveredLocations,
      double pMaxCount) {
    super(pCoveredLocations, pMaxCount);
    alternativeCoveredLocations = pAlternativeCoveredLocations;
  }

  /* ##### Getter Methods ##### */
  public Set<Integer> getIntersectionLocations() {
    Set<Integer> intersect = new HashSet<>(getCoveredLocations().elementSet());
    intersect.removeAll(alternativeCoveredLocations.elementSet());
    return intersect;
  }

  public Set<Integer> getAlternativeCoveredSet() {
    return alternativeCoveredLocations.elementSet();
  }

  /* ##### Inherited Methods ##### */
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
