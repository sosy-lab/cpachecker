// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.location;

import com.google.common.collect.FluentIterable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.GhostCFA;
import org.sosy_lab.cpachecker.core.interfaces.AdjustablePrecision;

public class LocationPrecision implements AdjustablePrecision {

  private Set<GhostCFA> forbiddenStrategies = new HashSet<>();

  private List<GhostCFA> beginningStrategies = new ArrayList<>();

  private Optional<GhostCFA> currentStrategy = Optional.empty();

  public LocationPrecision() {}

  public LocationPrecision(List<GhostCFA> pBeginningStrategies) {
    beginningStrategies = pBeginningStrategies;
  }

  public void addForbiddenStrategy(GhostCFA ghostCFA) {
    forbiddenStrategies.add(ghostCFA);
  }

  public Set<GhostCFA> getForbiddenStrategies() {
    return this.forbiddenStrategies;
  }

  public Optional<GhostCFA> getCurrentStrategy() {
    return currentStrategy;
  }

  public void setCurrentStrategy(Optional<GhostCFA> pCurrentStrategy) {
    currentStrategy = pCurrentStrategy;
  }

  /**
   *
   * @param pNode the node to be checked
   * @return whether the given CFANode is the starting node of any of the available strategies
   */
  public boolean isStartOfSomeStrategy(CFANode pNode) {
    return FluentIterable.from(beginningStrategies)
        .transform(g -> g.getStartGhostCfaNode())
        .toSet()
        .contains(pNode);
  }

  @Override
  public String toString() {
    return "LocationPrecision " + this.forbiddenStrategies;
  }

  @Override
  public AdjustablePrecision add(AdjustablePrecision pOtherPrecision) {
    if (pOtherPrecision instanceof LocationPrecision) {
      LocationPrecision newLocationPrecission = new LocationPrecision();
      Set<GhostCFA> newUnallowedStrategies = new HashSet<>();
      newUnallowedStrategies.addAll(forbiddenStrategies);
      newUnallowedStrategies.addAll(((LocationPrecision) pOtherPrecision).getForbiddenStrategies());
      for (GhostCFA s : newUnallowedStrategies) {
        newLocationPrecission.addForbiddenStrategy(s);
      }

      return newLocationPrecission;
    }
    return null;

  }

  @Override
  public AdjustablePrecision subtract(AdjustablePrecision pOtherPrecision) {
    if (pOtherPrecision instanceof LocationPrecision) {
      LocationPrecision newLocationPrecission = new LocationPrecision();
      Set<GhostCFA> newUnallowedStrategies = new HashSet<>();
      newUnallowedStrategies.addAll(forbiddenStrategies);
      newUnallowedStrategies.removeAll(
          ((LocationPrecision) pOtherPrecision).getForbiddenStrategies());
      for (GhostCFA s : newUnallowedStrategies) {
        newLocationPrecission.addForbiddenStrategy(s);
      }

      return newLocationPrecission;
    }
    return null;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }


}
