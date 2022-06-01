// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.measures;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.coverage.util.CoverageColorUtil;

/**
 * A coverage measure which is based on working with the CFA locations. Therefore, the coverage
 * depends on the total relevant locations and all locations which are considered as covered
 * depending on concrete coverage criteria. The coverage criteria is applied during the data
 * gathering and not within this class. Data gathering is typically done after the analysis within
 * the CoverageCollector or during the analysis within a CoverageCPA.
 */
public class LocationBasedCoverageMeasure implements CoverageMeasure {
  private final ImmutableMultiset<CFANode> coveredLocations;
  private final double maxCount;

  /**
   * Creates a LocationsBasedCoverageMeasure instance.
   *
   * @param pCoveredLocations a multiset containing all covered locations.
   * @param pMaxCount the highest theoretically possible location count. This number should be
   *     always equal or greater than pCoveredLocations.elementSet().size() and greater than 0,
   *     since it used as divisor and otherwise could lead to division by zero.
   */
  public LocationBasedCoverageMeasure(Multiset<CFANode> pCoveredLocations, double pMaxCount) {
    checkArgument(pMaxCount > 0);
    checkArgument(pMaxCount >= pCoveredLocations.elementSet().size());
    maxCount = pMaxCount;
    coveredLocations = ImmutableMultiset.copyOf(pCoveredLocations);
  }

  public LocationBasedCoverageMeasure(Set<CFANode> pCoveredLocations, double pMaxCount) {
    this(LinkedHashMultiset.create(pCoveredLocations), pMaxCount);
  }

  /**
   * Returns the color representing if a location is covered or not. This information is used for
   * later visualization in the report.html CFA Tab.
   *
   * @param location location identification number within the CFA
   * @return hex color code which represents the coverage status for the given location
   */
  public String getColor(CFANode location) {
    if (getCoveredSet().contains(location)) {
      return CoverageColorUtil.DEFAULT_COVERAGE_COLOR;
    } else {
      return CoverageColorUtil.DEFAULT_ELEMENT_COLOR;
    }
  }

  Multiset<CFANode> getCoveredLocations() {
    return coveredLocations;
  }

  private Set<CFANode> getCoveredSet() {
    return coveredLocations.elementSet();
  }

  @Override
  public double getNormalizedValue() {
    return getValue() / getMaxValue();
  }

  @Override
  public double getValue() {
    return coveredLocations.elementSet().size();
  }

  @Override
  public double getMaxValue() {
    return maxCount;
  }
}
