// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.tdcg;

import org.sosy_lab.cpachecker.util.coverage.util.CoverageColorUtil;

public enum TimeDependentCoverageType {
  /* ##### Enum Fields ##### */
  VisitedLines("Visited Lines", CoverageColorUtil.GREEN_TDCG_COLOR),
  PredicatesGenerated("Predicates Generated", CoverageColorUtil.BLUE_TDCG_COLOR, false),
  PredicateConsideredLocations("Predicate-Considered Locations", CoverageColorUtil.RED_TDCG_COLOR),
  PredicateRelevantVariables("Predicate-Relevant-Variables", CoverageColorUtil.YELLOW_TDCG_COLOR),
  AbstractStateCoveredNodes("Abstract-State Covered Nodes", CoverageColorUtil.PURPLE_TDCG_COLOR);

  private final String name;
  private final String color;
  private final boolean isPercentage;

  /* ##### Constructors ##### */
  TimeDependentCoverageType(String pName, String pColor, boolean pIsPercentage) {
    name = pName;
    color = pColor;
    isPercentage = pIsPercentage;
  }

  TimeDependentCoverageType(String pName, String pColor) {
    this(pName, pColor, true);
  }

  /* ##### Getter and Setter ##### */
  public String getName() {
    return name;
  }

  public String getColor() {
    return color;
  }

  public boolean isPercentage() {
    return isPercentage;
  }
}
