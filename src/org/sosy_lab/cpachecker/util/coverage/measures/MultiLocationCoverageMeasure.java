// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.measures;

import com.google.common.collect.Multiset;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.coverage.util.CoverageColorUtil;

/**
 * Behaves like LocationCoverageMeasure but with the difference that it considers a second coverage
 * criteria. For example when looking at all reached locations and all visited locations, we can use
 * this MultiLocationCoverageMeasure to consider both coverage criteria and i.e. calculate the
 * intersection and use this as combined coverage criteria.
 */
public class MultiLocationCoverageMeasure extends LocationCoverageMeasure {
  /* ##### Class fields ##### */
  private final Multiset<CFANode> alternativeCoveredLocations;

  /* ##### Constructors ##### */
  public MultiLocationCoverageMeasure(
      Multiset<CFANode> pCoveredLocations,
      Multiset<CFANode> pAlternativeCoveredLocations,
      double pMaxCount) {
    super(pCoveredLocations, pMaxCount);
    alternativeCoveredLocations = pAlternativeCoveredLocations;
  }

  /* ##### Getter Methods ##### */
  public Set<CFANode> getAlternativeCoveredSet() {
    return alternativeCoveredLocations.elementSet();
  }

  /* ##### Inherited Methods ##### */
  @Override
  public String getColor(CFANode location) {
    if (getAlternativeCoveredSet().contains(location)) {
      return CoverageColorUtil.getFrequencyColorMapForLocations(alternativeCoveredLocations)
          .get(location);
    } else if (getCoveredLocations().contains(location)) {
      return CoverageColorUtil.DEFAULT_CONSIDERED_COLOR;
    } else {
      return CoverageColorUtil.DEFAULT_ELEMENT_COLOR;
    }
  }
}
