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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.coverage.util.CoverageColorUtil;

/**
 * A coverage measure which is based on working with the CFA locations. Therefore, the coverage
 * depends on the total relevant locations and all locations which are considered as covered
 * depending on concrete coverage criteria. The coverage criteria is applied during the data
 * gathering and not within this class. Data gathering is typically done after the analysis within
 * the CoverageCollector or during the analysis within a CoverageCPA.
 */
public class LocationCoverageMeasure implements CoverageMeasure {
  /* ##### Class Fields ##### */
  private final Multiset<CFANode> coveredLocations;
  private final double maxCount;
  private String coverageColor;

  /* ##### Constructors ##### */
  public LocationCoverageMeasure(
      Multiset<CFANode> pCoveredLocations, double pMaxCount, String pCoverageColor) {
    this(pCoveredLocations, pMaxCount);
    coverageColor = pCoverageColor;
  }

  public LocationCoverageMeasure(
      Set<CFANode> pCoveredLocations, double pMaxCount, String pCoverageColor) {
    this(pCoveredLocations, pMaxCount);
    coverageColor = pCoverageColor;
  }

  public LocationCoverageMeasure(Multiset<CFANode> pCoveredLocations, double pMaxCount) {
    coveredLocations = pCoveredLocations;
    coverageColor = CoverageColorUtil.DEFAULT_COVERAGE_COLOR;
    if (pMaxCount <= 0) {
      maxCount = 1.0;
    } else {
      maxCount = pMaxCount;
    }
  }

  public LocationCoverageMeasure(Set<CFANode> pCoveredLocations, double pMaxCount) {
    this(LinkedHashMultiset.create(pCoveredLocations), pMaxCount);
  }

  public LocationCoverageMeasure() {
    coveredLocations = LinkedHashMultiset.create();
    maxCount = 1;
  }

  /* ##### Getter Methods ##### */
  /**
   * Returns the color representing if a location is covered or not. This information is used for
   * later visualization in the report.html CFA Tab.
   *
   * @param location location identification number within the CFA
   * @return hex color code which represents the coverage status for the given location
   */
  public String getColor(CFANode location) {
    if (getCoveredSet().contains(location)) {
      return coverageColor;
    } else {
      return CoverageColorUtil.DEFAULT_ELEMENT_COLOR;
    }
  }

  public Set<CFANode> getCoveredSet() {
    return coveredLocations.elementSet();
  }

  public Multiset<CFANode> getCoveredLocations() {
    return coveredLocations;
  }

  /* ##### Interface Implementations ##### */
  @Override
  public double getCoverage() {
    return getCount() / getMaxCount();
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
