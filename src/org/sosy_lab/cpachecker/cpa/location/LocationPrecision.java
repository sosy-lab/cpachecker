// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.location;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.core.interfaces.AdjustablePrecision;

public class LocationPrecision implements AdjustablePrecision {

  private Set<StrategiesEnum> unallowedStrategies = new HashSet<>();

  public LocationPrecision() {}

  public void addUnallowedStrategy(StrategiesEnum strategy) {
    unallowedStrategies.add(strategy);
  }

  public Set<StrategiesEnum> getUnallowedStrategies() {
    return this.unallowedStrategies;
  }

  @Override
  public String toString() {
    return "LocationPrecision " + this.unallowedStrategies;
  }

  @Override
  public AdjustablePrecision add(AdjustablePrecision pOtherPrecision) {
    if (pOtherPrecision instanceof LocationPrecision) {
      LocationPrecision newLocationPrecission = new LocationPrecision();
      Set<StrategiesEnum> newUnallowedStrategies = new HashSet<>();
      newUnallowedStrategies.addAll(unallowedStrategies);
      newUnallowedStrategies.addAll(((LocationPrecision) pOtherPrecision).getUnallowedStrategies());
      for (StrategiesEnum s : newUnallowedStrategies) {
        newLocationPrecission.addUnallowedStrategy(s);
      }

      return newLocationPrecission;
    }
    return null;

  }

  @Override
  public AdjustablePrecision subtract(AdjustablePrecision pOtherPrecision) {
    if (pOtherPrecision instanceof LocationPrecision) {
      LocationPrecision newLocationPrecission = new LocationPrecision();
      Set<StrategiesEnum> newUnallowedStrategies = new HashSet<>();
      newUnallowedStrategies.addAll(unallowedStrategies);
      newUnallowedStrategies.removeAll(
          ((LocationPrecision) pOtherPrecision).getUnallowedStrategies());
      for (StrategiesEnum s : newUnallowedStrategies) {
        newLocationPrecission.addUnallowedStrategy(s);
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
