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

import org.sosy_lab.cpachecker.util.presence.PresenceConditions;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceCondition;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;

public class AutomatonPrecision implements Precision, AutomatonPrecisionView {

  private static AutomatonPrecision GLOBAL_PRECISION = AutomatonPrecision.emptyBlacklist();

  public static synchronized void updateGlobalPrecision(AutomatonPrecision pAutomatonPrecision) {
    Preconditions.checkNotNull(pAutomatonPrecision);
    GLOBAL_PRECISION = pAutomatonPrecision;
  }

  public static synchronized AutomatonPrecision getGlobalPrecision() {
    return GLOBAL_PRECISION;
  }

  private ImmutableMap<SafetyProperty, Optional<PresenceCondition>> blacklist = ImmutableMap.of();

  private final int cachedHashCode;

  private AutomatonPrecision(ImmutableMap<SafetyProperty, Optional<PresenceCondition>> pBlacklist) {
    blacklist = pBlacklist;
    cachedHashCode = blacklist.hashCode();
  }

  public static AutomatonPrecision emptyBlacklist() {
    return new AutomatonPrecision(ImmutableMap.<SafetyProperty, Optional<PresenceCondition>>of());
  }

  public static AutomatonPrecision initBlacklist(Map<SafetyProperty, Optional<PresenceCondition>> pBlacklist) {
    return new AutomatonPrecision(ImmutableMap.copyOf(pBlacklist));
  }

  /**
   * Create a copy of this precision and
   * add blacklist the given properties for the corresponding region.
   *
   * @param pProperties
   * @return
   */
  public AutomatonPrecision cloneAndAddBlacklisted(Map<? extends SafetyProperty,
      Optional<PresenceCondition>> pProperties) {

    Builder<SafetyProperty, Optional<PresenceCondition>> builder = ImmutableMap.<SafetyProperty, Optional<PresenceCondition>>builder();

    for (Entry<SafetyProperty, Optional<PresenceCondition>> e: this.blacklist.entrySet()) {
      SafetyProperty prop = e.getKey();
      Optional<PresenceCondition> thisPresenceCond = e.getValue();

      Optional<PresenceCondition> otherPresenceCond = pProperties.get(prop);
      if (otherPresenceCond == null) {
        otherPresenceCond = Optional.absent();
      }

      final Optional<PresenceCondition> presenceCondUnion =
          makeDisjunctionOfConditions(thisPresenceCond, otherPresenceCond);

      builder.put(prop, presenceCondUnion);
    }

    for (Entry<? extends SafetyProperty, Optional<PresenceCondition>> e: pProperties.entrySet()) {
      if (!this.blacklist.containsKey(e.getKey())) {
        builder.put(e.getKey(), e.getValue());
      }
    }

    return new AutomatonPrecision(builder.build());
  }

  private Optional<PresenceCondition> makeDisjunctionOfConditions(Optional<PresenceCondition> thisPresenceCond,
      Optional<PresenceCondition> otherPresenceCond) {

    Preconditions.checkNotNull(thisPresenceCond);
    Preconditions.checkNotNull(otherPresenceCond);

    if (!otherPresenceCond.isPresent()) {
      return thisPresenceCond;
    } else if (!thisPresenceCond.isPresent()) {
      return otherPresenceCond;
    } else {
      return Optional.of(PresenceConditions.manager().makeOr(
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

    Builder<SafetyProperty, Optional<PresenceCondition>> builder = ImmutableMap.<SafetyProperty, Optional<PresenceCondition>>builder();
    Set<SafetyProperty> commonProperties = Sets.intersection(this.blacklist.keySet(), other.blacklist.keySet());

    for (SafetyProperty p: commonProperties) {
      Optional<PresenceCondition> thisPresenceCond = this.blacklist.get(p);
      Optional<PresenceCondition> otherPresenceCond = other.blacklist.get(p);

      final Optional<PresenceCondition> joinedPresenceCondition = makeDisjunctionOfConditions(thisPresenceCond, otherPresenceCond);

      builder.put(p, joinedPresenceCondition);
    }

    return new AutomatonPrecision(builder.build());
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
  @Override
  public boolean areBlackListed(
      Set<? extends SafetyProperty> pViolatedProperties,
      PresenceCondition pPresenceCondition)
      throws InterruptedException, CPAException {

    final PresenceCondition presenceOfProperties = PresenceConditions.orTrue(pPresenceCondition);
    int intersection = 0;

    for (SafetyProperty p : pViolatedProperties) {
      if (!blacklist.containsKey(p)) {
        return false;
      }

      PresenceCondition blacklistedFor = PresenceConditions.orTrue(blacklist.get(p));
      // blacklistedFor = false
      // presenceOfProperties = true
      // ---> covered = false
      boolean covered = PresenceConditions.manager().checkEntails(presenceOfProperties, blacklistedFor);

      if (covered) {
        intersection++;
      }
    }

    // All given properties are blacklisted
    if (intersection == pViolatedProperties.size()) {
      return true;
    }

    return false;
  }

  public Map<SafetyProperty, Optional<PresenceCondition>> getBlacklist() {
    return blacklist;
  }

  @Override
  public PresenceCondition getBlacklistedFor(SafetyProperty pSafetyProperty) {
    Optional<PresenceCondition> forCond = blacklist.get(pSafetyProperty);
    return PresenceConditions.orFalse(forCond);
  }

  @Override
  public int hashCode() {
    return cachedHashCode;
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

  public static class PairedAutomatonPrecision implements AutomatonPrecisionView {

    @Nonnull private final AutomatonPrecision precision1;
    @Nonnull private final AutomatonPrecision precision2;

    public static AutomatonPrecisionView of(@Nonnull AutomatonPrecision pPrecision1,
      @Nonnull AutomatonPrecision pPrecision2) {
      return new PairedAutomatonPrecision(pPrecision1, pPrecision2);
    }

    private PairedAutomatonPrecision(
        @Nonnull AutomatonPrecision pPrecision1,
        @Nonnull AutomatonPrecision pPrecision2) {
      precision1 = Preconditions.checkNotNull(pPrecision1);
      precision2 = Preconditions.checkNotNull(pPrecision2);
    }

    @Override
    public boolean areBlackListed(
        Set<? extends SafetyProperty> pViolatedProperties, PresenceCondition pPresenceCondition)
      throws InterruptedException, CPAException {

      return precision1.areBlackListed(pViolatedProperties, pPresenceCondition)
          || precision2.areBlackListed(pViolatedProperties, pPresenceCondition);
    }

    @Override
    public PresenceCondition getBlacklistedFor(SafetyProperty pSafetyProperty) {
      PresenceCondition pc1 = precision1.getBlacklistedFor(pSafetyProperty);
      PresenceCondition pc2 = precision1.getBlacklistedFor(pSafetyProperty);

      return PresenceConditions.manager().makeOr(pc1, pc2);
    }
  }
}
