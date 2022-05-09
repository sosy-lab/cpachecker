// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.tdcg;

public enum TimeDependentCoverageType {
  VisitedLines("Visited Lines", "#3cc220"),
  PredicatesGenerated("Predicates Generated", "#1a81d5", false),
  PredicateConsideredLocations("Predicate-Considered Locations", "#e33636"),
  PredicateRelevantVariables("Predicate-Relevant-Variables", "#d9ae19"),
  AbstractStateCoveredNodes("Abstract-State Covered Nodes", "#7c0eb4");

  private final String name;
  private final String color;
  private final boolean isPercentage;

  TimeDependentCoverageType(String pName, String pColor, boolean pIsPercentage) {
    name = pName;
    color = pColor;
    isPercentage = pIsPercentage;
  }

  TimeDependentCoverageType(String pName, String pColor) {
    this(pName, pColor, true);
  }

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
