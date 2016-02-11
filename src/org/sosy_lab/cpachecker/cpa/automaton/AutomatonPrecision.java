/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.automaton;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;
import org.sosy_lab.solver.SolverException;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Sets;

public class AutomatonPrecision implements Precision {

  private ImmutableMap<SafetyProperty, Optional<Region>> blacklist = ImmutableMap.of();
  @Nullable private final RegionManager regionManager;

  private AutomatonPrecision(ImmutableMap<SafetyProperty, Optional<Region>> pBlacklist, RegionManager pRegionManager) {
    blacklist = pBlacklist;
    regionManager = pRegionManager;
  }

  public static AutomatonPrecision emptyBlacklist() {
    return new AutomatonPrecision(ImmutableMap.<SafetyProperty, Optional<Region>>of(), null);
  }

  public AutomatonPrecision cloneAndAddBlacklisted(Map<SafetyProperty, Optional<Region>> pProperties,
      RegionManager pRegionManager) {

    Builder<SafetyProperty, Optional<Region>> builder = ImmutableMap.<SafetyProperty, Optional<Region>>builder();

    for (Entry<SafetyProperty, Optional<Region>> e: this.blacklist.entrySet()) {
      SafetyProperty prop = e.getKey();
      Optional<Region> thisPresenceCond = e.getValue();
      Optional<Region> otherPresenceCond = pProperties.get(prop);

      final Optional<Region> presenceCondUnion =
          makeAndOfConditions(pRegionManager, thisPresenceCond, otherPresenceCond);

      builder.put(prop, presenceCondUnion);
    }

    for (Entry<SafetyProperty, Optional<Region>> e: pProperties.entrySet()) {
      if (!this.blacklist.containsKey(e.getKey())) {
        builder.put(e.getKey(), e.getValue());
      }
    }

    return new AutomatonPrecision(builder.build(), pRegionManager);
  }

  private Optional<Region> makeAndOfConditions(RegionManager pRegionManager,
      Optional<Region> thisPresenceCond, Optional<Region> otherPresenceCond) {

    if (!otherPresenceCond.isPresent()) {
      return thisPresenceCond;
    } else if (!thisPresenceCond.isPresent()) {
      return otherPresenceCond;
    } else {
      return Optional.of(pRegionManager.makeOr(thisPresenceCond.get(), otherPresenceCond.get()));
    }
  }

  public ImmutableMap<SafetyProperty, Optional<Region>> getBlacklist() {
    return blacklist;
  }

  @Override
  public String toString() {
    return blacklist.toString();
  }

  /**
   * The join of two automata precisions with property
   *  blacklists is special.
   *
   * The resulting precision blacklists the intersection of the input blacklists.
   *  (which is the union of the whitelists).
   */
  @Override
  public Precision join(Precision pOther) {
    Preconditions.checkArgument(pOther instanceof AutomatonPrecision);
    AutomatonPrecision other = (AutomatonPrecision) pOther;

    RegionManager rm = this.regionManager;
    if (rm == null) {
      rm = other.regionManager;
    }

    Builder<SafetyProperty, Optional<Region>> builder = ImmutableMap.<SafetyProperty, Optional<Region>>builder();
    Set<SafetyProperty> commonProperties = Sets.intersection(this.blacklist.keySet(), other.blacklist.keySet());

    for (SafetyProperty p: commonProperties) {
      Optional<Region> thisPresenceCond = this.blacklist.get(p);
      Optional<Region> otherPresenceCond = other.blacklist.get(p);

      final Optional<Region> joinedPresenceCondition = makeAndOfConditions(rm, thisPresenceCond, otherPresenceCond);

      builder.put(p, joinedPresenceCondition);
    }

    return new AutomatonPrecision(builder.build(), rm);
  }

  /**
   * Are all properties blacklisted under the given presence conditions?
   *
   * @param pViolatedProperties
   * @param pPresenceCondition
   * @return
   * @throws InterruptedException
   * @throws SolverException
   */
  public boolean isBlackListed(Set<SafetyProperty> pViolatedProperties, Region pPresenceCondition)
      throws SolverException, InterruptedException {

    int intersection = 0;

    for (SafetyProperty p : pViolatedProperties) {
      if (!blacklist.containsKey(p)) {
        return false;
      }
      Optional<Region> propBlacklistedFor = blacklist.get(p);
      if (propBlacklistedFor == null || !propBlacklistedFor.isPresent()) { // equals pc==TRUE, i.e., blacklisted for all configurations
        intersection++;
      } else {
        Preconditions.checkState(regionManager != null);
        boolean covered = regionManager.entails(pPresenceCondition, propBlacklistedFor.get());
        if (covered) {
          intersection++;
        }
      }
    }

    if (intersection == pViolatedProperties.size()) {
      return true;
    }

    return false;
  }


}
