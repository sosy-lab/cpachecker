/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.mpa;

import static org.sosy_lab.cpachecker.util.AbstractStates.isTargetState;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithPresenceCondition;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.bdd.BDDCPA;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.regions.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

public class TargetSummary {

  static class TargetStateInfo {
    private Set<Property> violatedProperties = Sets.newLinkedHashSet();
    private Optional<Region> presenceCondition = Optional.absent();
  }

  private Collection<TargetStateInfo> targetStateSummaries = Lists.newArrayList();

  public static TargetSummary identifyViolationsInRun(LogManager pLogger, ReachedSet pReachedSet) {
    TargetSummary result = new TargetSummary();

    // ASSUMPTION: no "global refinement" is used! (not yet implemented for this algorithm!)

    final AbstractState e = pReachedSet.getLastState();
    if (isTargetState(e)) {
      TargetStateInfo stateSummary = new TargetStateInfo();
      stateSummary.violatedProperties = AbstractStates.extractViolatedProperties(e, Property.class);
      stateSummary.presenceCondition = getPresenceCondition(e);

      String presenceConditionText = stateSummary.presenceCondition.toString();

      BDDCPA bddCpa = CPAs.retrieveCPA(GlobalInfo.getInstance().getCPA().get(), BDDCPA.class);
      if (bddCpa != null) {
        NamedRegionManager rm = bddCpa.getManager();
        if (rm != null) {
          if (stateSummary.presenceCondition.isPresent()) {
            presenceConditionText = rm.dumpRegion(stateSummary.presenceCondition.get()).toString();
          }
        }
      }

      result.targetStateSummaries.add(stateSummary);

      pLogger.logf(Level.INFO, "Violation of %s at %s in the configuration %s",
          stateSummary.violatedProperties.toString(), AbstractStates.extractLocation(e).describeFileLocation(),
          presenceConditionText);
    }

    return result;
  }


  private static Optional<Region> getPresenceCondition(AbstractState pTargetState) {
    final AbstractStateWithPresenceCondition targetStateWithPc = AbstractStates.extractStateByType(pTargetState,
        AbstractStateWithPresenceCondition.class);

    if (targetStateWithPc != null) {
      final Region targetCondition = targetStateWithPc.getPresenceCondition();
      return Optional.of(targetCondition);
    }

    return Optional.absent();
  }


  boolean hasTargetStates() {
    return !targetStateSummaries.isEmpty();
  }

  public Set<Property> getViolatedProperties() {
    LinkedHashSet<Property> result = Sets.newLinkedHashSet();
    for (TargetStateInfo s: targetStateSummaries) {
      result.addAll(s.violatedProperties);
    }
    return result;
  }

  public Map<Property, Optional<Region>> getViolationConditions() {
    Map<Property, Optional<Region>> result = Maps.newLinkedHashMap();
    for (TargetStateInfo s: targetStateSummaries) {
      for (Property p: s.violatedProperties) {
        Preconditions.checkState(result.get(p) == null);
        result.put(p, s.presenceCondition);
      }
    }
    return result;
  }

  private static Optional<Region> unionOfConditions(
      RegionManager pRm,
      @Nullable Optional<Region> pCondition1,
      @Nullable Optional<Region> pCondition2) {

    boolean noCond = ((pCondition1 == null || !pCondition1.isPresent())
        && (pCondition2 == null || !pCondition2.isPresent()));

    if (noCond) {
      return Optional.absent();
    }

    Preconditions.checkNotNull(pRm);

    Region pRegion1;
    Region pRegion2;

    if (pCondition1 == null || !pCondition1.isPresent()) {
      pRegion1 = pRm.makeTrue();
    } else {
      pRegion1 = pCondition1.get();
    }

    if (pCondition2 == null || !pCondition2.isPresent()) {
      pRegion2 = pRm.makeTrue();
    } else {
      pRegion2 = pCondition2.get();
    }

    return Optional.of(pRm.makeOr(pRegion1, pRegion2));
  }

  public static TargetSummary union (RegionManager pRm, TargetSummary pSummary1, TargetSummary pSummary2) {
    Set<Property> properties = Sets.newLinkedHashSet();
    properties.addAll(pSummary1.getViolatedProperties());
    properties.addAll(pSummary2.getViolatedProperties());

    TargetSummary result = new TargetSummary();

    Map<Property, Optional<Region>> conditions1 = pSummary1.getViolationConditions();
    Map<Property, Optional<Region>> conditions2 = pSummary2.getViolationConditions();

    for (Property p: properties) {
      Optional<Region> cond1 = conditions1.get(p);
      Optional<Region> cond2 = conditions2.get(p);

      Optional<Region> condUnion = unionOfConditions(pRm, cond1, cond2);

      TargetStateInfo sum = new TargetStateInfo();
      sum.violatedProperties.add(p);
      sum.presenceCondition = condUnion;

      result.targetStateSummaries.add(sum);
    }

    return result;
  }

  public static TargetSummary none() {
    return new TargetSummary();
  }

}
