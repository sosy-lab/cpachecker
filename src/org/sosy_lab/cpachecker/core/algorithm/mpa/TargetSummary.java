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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithPresenceCondition;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.SafetyProperty;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceCondition;
import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceConditionManager;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

public class TargetSummary {

  static class TargetStateInfo {
    private Set<Property> violatedProperties = Sets.newLinkedHashSet();
    private Optional<PresenceCondition> presenceCondition = Optional.absent();
  }

  private Collection<TargetStateInfo> targetStateSummaries = Lists.newArrayList();

  public static TargetSummary of(LogManager pLogger,
      Map<SafetyProperty, Optional<PresenceCondition>> pCovered) {

    TargetSummary result = new TargetSummary();
    for (Entry<SafetyProperty, Optional<PresenceCondition>> e: pCovered.entrySet()) {
      TargetStateInfo targetInfo = new TargetStateInfo();
      targetInfo.violatedProperties = ImmutableSet.of(e.getKey());
      targetInfo.presenceCondition = e.getValue();
      result.targetStateSummaries.add(targetInfo);
    }

    return result;
  }

  public static TargetSummary identifyViolationsInRun(LogManager pLogger, AbstractState pState) {
    TargetSummary result = new TargetSummary();

    // ASSUMPTION: no "global refinement" is used! (not yet implemented for this algorithm!)

    if (isTargetState(pState)) {
      TargetStateInfo stateSummary = new TargetStateInfo();
      stateSummary.violatedProperties = AbstractStates.extractViolatedProperties(pState, Property.class);
      stateSummary.presenceCondition = getPresenceCondition(pState);

      final String presenceConditionText;

      if (stateSummary.presenceCondition.isPresent()) {
        PresenceConditionManager pcMgr = GlobalInfo.getInstance().getPresenceConditionManager();
        presenceConditionText = pcMgr.dump(stateSummary.presenceCondition.get()).toString();
      } else {
        presenceConditionText = "ANY";
      }

      result.targetStateSummaries.add(stateSummary);

      pLogger.logf(Level.INFO, "Target for %s at %s in the configuration %s",
          stateSummary.violatedProperties.toString(),
          AbstractStates.extractLocation(pState).describeFileLocation(), presenceConditionText);
    }

    return result;
  }

  public static TargetSummary identifyViolationsInRun(LogManager pLogger, ReachedSet pReachedSet) {
    final AbstractState e = pReachedSet.getLastState();
    return identifyViolationsInRun(pLogger, e);
  }

  private static Optional<PresenceCondition> getPresenceCondition(AbstractState pTargetState) {
    final AbstractStateWithPresenceCondition targetStateWithPc = AbstractStates.extractStateByType(pTargetState,
        AbstractStateWithPresenceCondition.class);

    if (targetStateWithPc != null) {
      return targetStateWithPc.getPresenceCondition();
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

  public Map<Property, Optional<PresenceCondition>> getViolationConditions() {
    Map<Property, Optional<PresenceCondition>> result = Maps.newLinkedHashMap();
    for (TargetStateInfo s: targetStateSummaries) {
      for (Property p: s.violatedProperties) {
        Preconditions.checkState(result.get(p) == null);
        result.put(p, s.presenceCondition);
      }
    }
    return result;
  }

  private static Optional<PresenceCondition> unionOfConditions(
      @Nullable Optional<PresenceCondition> pCondition1,
      @Nullable Optional<PresenceCondition> pCondition2) {

    PresenceConditionManager pcMgr = GlobalInfo.getInstance().getPresenceConditionManager();

    boolean noCond = ((pCondition1 == null || !pCondition1.isPresent())
        && (pCondition2 == null || !pCondition2.isPresent()));

    if (noCond) {
      return Optional.absent();
    }

    PresenceCondition pRegion1;
    PresenceCondition pRegion2;

    if (pCondition1 == null || !pCondition1.isPresent()) {
      pRegion1 = pcMgr.makeTrue();
    } else {
      pRegion1 = pCondition1.get();
    }

    if (pCondition2 == null || !pCondition2.isPresent()) {
      pRegion2 = pcMgr.makeTrue();
    } else {
      pRegion2 = pCondition2.get();
    }

    return Optional.of(pcMgr.makeOr(pRegion1, pRegion2));
  }

  public static TargetSummary union (TargetSummary pSummary1, TargetSummary pSummary2) {
    Set<Property> properties = Sets.newLinkedHashSet();
    properties.addAll(pSummary1.getViolatedProperties());
    properties.addAll(pSummary2.getViolatedProperties());

    TargetSummary result = new TargetSummary();

    Map<Property, Optional<PresenceCondition>> conditions1 = pSummary1.getViolationConditions();
    Map<Property, Optional<PresenceCondition>> conditions2 = pSummary2.getViolationConditions();

    for (Property p: properties) {
      Optional<PresenceCondition> cond1 = conditions1.get(p);
      Optional<PresenceCondition> cond2 = conditions2.get(p);

      Optional<PresenceCondition> condUnion;
      if (cond1 == null) {
        condUnion = cond2;
      } else if (cond2 == null) {
        condUnion = cond1;
      } else {
        condUnion = unionOfConditions(cond1, cond2);
      }

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
