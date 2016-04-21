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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;
import org.sosy_lab.solver.SolverException;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

public class AutomatonPrecision implements Precision {

  private ImmutableMap<SafetyProperty, Optional<Region>> blacklist = ImmutableMap.of();
  @Nullable private final RegionManager regionManager;

  private AutomatonPrecision(ImmutableMap<SafetyProperty, Optional<Region>> pBlacklist,
      @Nullable RegionManager pRegionManager) {
    blacklist = pBlacklist;
    regionManager = pRegionManager;
  }

  public static AutomatonPrecision emptyBlacklist() {
    return new AutomatonPrecision(ImmutableMap.<SafetyProperty, Optional<Region>>of(), null);
  }

  public static AutomatonPrecision initBlacklist(Map<SafetyProperty, Optional<Region>> pBlacklist,
      @Nullable RegionManager pRegionManager) {
    return new AutomatonPrecision(ImmutableMap.copyOf(pBlacklist), pRegionManager);
  }

  /**
   * Create a copy of this precision and
   * add blacklist the given properties for the corresponding region.
   *
   * @param pProperties
   * @param pRegionManager
   * @return
   */
  public AutomatonPrecision cloneAndAddBlacklisted(Map<? extends SafetyProperty, Optional<Region>> pProperties,
      RegionManager pRegionManager) {

    Builder<SafetyProperty, Optional<Region>> builder = ImmutableMap.<SafetyProperty, Optional<Region>>builder();

    for (Entry<SafetyProperty, Optional<Region>> e: this.blacklist.entrySet()) {
      SafetyProperty prop = e.getKey();
      Optional<Region> thisPresenceCond = e.getValue();

      Optional<Region> otherPresenceCond = pProperties.get(prop);
      if (otherPresenceCond == null) {
        otherPresenceCond = Optional.absent();
      }

      final Optional<Region> presenceCondUnion =
          makeDisjunctionOfConditions(pRegionManager, thisPresenceCond, otherPresenceCond);

      builder.put(prop, presenceCondUnion);
    }

    for (Entry<? extends SafetyProperty, Optional<Region>> e: pProperties.entrySet()) {
      if (!this.blacklist.containsKey(e.getKey())) {
        builder.put(e.getKey(), e.getValue());
      }
    }

    return new AutomatonPrecision(builder.build(), pRegionManager);
  }

  private Optional<Region> makeDisjunctionOfConditions(RegionManager pRegionManager,
      Optional<Region> thisPresenceCond, Optional<Region> otherPresenceCond) {

    Preconditions.checkNotNull(thisPresenceCond);
    Preconditions.checkNotNull(otherPresenceCond);

    if (!otherPresenceCond.isPresent()) {
      return thisPresenceCond;
    } else if (!thisPresenceCond.isPresent()) {
      return otherPresenceCond;
    } else {
      Preconditions.checkNotNull(pRegionManager);

      return Optional.of(pRegionManager.makeOr(
          otherPresenceCond.get(),
          thisPresenceCond.get()
          ));
    }
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

      final Optional<Region> joinedPresenceCondition = makeDisjunctionOfConditions(rm, thisPresenceCond, otherPresenceCond);

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
   * @throws CPAException
   */
  public boolean areBlackListed(Set<? extends SafetyProperty> pViolatedProperties, Optional<Region> pPresenceCondition)
      throws InterruptedException, CPAException {

    int intersection = 0;

    for (SafetyProperty p : pViolatedProperties) {
      if (!blacklist.containsKey(p)) {
        return false;
      }

      Optional<Region> propBlacklistedFor = blacklist.get(p);
      if (propBlacklistedFor == null || !propBlacklistedFor.isPresent()) { // equals pc==TRUE, that is, blacklisted for all configurations
        intersection++;
      } else {
        Preconditions.checkState(regionManager != null);

        final Region pcRegion;
        if (pPresenceCondition.isPresent()) {
          pcRegion = pPresenceCondition.get();
        } else {
          pcRegion = regionManager.makeTrue();
        }

        boolean covered;
        try {
          covered = regionManager.entails(pcRegion, propBlacklistedFor.get());
        } catch (SolverException e) {
          throw new CPAException("Solving the presence condition entailment failed!", e);
        }

        if (covered) {
          intersection++;
        }
      }
    }

    // All given properties are blacklisted
    if (intersection == pViolatedProperties.size()) {
      return true;
    }

    return false;
  }

  public Map<SafetyProperty, Optional<Region>> getBlacklist() {
    return blacklist;
  }

  @Override
  public int hashCode() {
    return blacklist.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }

    if (!(obj instanceof AutomatonPrecision)) {
      return false;
    }

    AutomatonPrecision other = (AutomatonPrecision) obj;

    if (!blacklist.equals(other.blacklist)) {
      return false;
    }

    return true;
  }

  @Override
  public String toString() {
    return blacklist.toString();
  }

}
