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
package org.sosy_lab.cpachecker.util.presence.region;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.sosy_lab.common.Appender;
import org.sosy_lab.cpachecker.util.predicates.regions.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;
import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceCondition;
import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceConditionManager;
import org.sosy_lab.solver.SolverException;

import java.util.List;
import java.util.Set;

public class RegionPresenceConditionManager implements PresenceConditionManager {

  private final RegionManager mgr;

  public RegionPresenceConditionManager(RegionManager pMgr) {
    mgr = pMgr;
  }

  @Override
  public PresenceCondition makeTrue() {
    return new RegionPresenceCondition(mgr.makeTrue());
  }

  @Override
  public PresenceCondition makeFalse() {
    return new RegionPresenceCondition(mgr.makeFalse());
  }

  @Override
  public boolean checkEntails(PresenceCondition pCond1, PresenceCondition pCond2)
      throws InterruptedException {
    Preconditions.checkArgument(pCond1 instanceof RegionPresenceCondition);
    Preconditions.checkArgument(pCond2 instanceof RegionPresenceCondition);

    RegionPresenceCondition cond1 = (RegionPresenceCondition) pCond1;
    RegionPresenceCondition cond2 = (RegionPresenceCondition) pCond2;

    try {
      return mgr.entails(cond1.getRegion(), cond2.getRegion());
    } catch (SolverException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public PresenceCondition makeNegation(PresenceCondition pNegationOf) {
    Preconditions.checkNotNull(pNegationOf);
    Preconditions.checkArgument(pNegationOf instanceof RegionPresenceCondition);

    RegionPresenceCondition negationOf = (RegionPresenceCondition) pNegationOf;

    return new RegionPresenceCondition(mgr.makeNot(negationOf.getRegion()));
  }

  @Override
  public PresenceCondition makeOr(PresenceCondition pCond1, PresenceCondition pCond2) {
    Preconditions.checkArgument(pCond1 instanceof RegionPresenceCondition);
    Preconditions.checkArgument(pCond2 instanceof RegionPresenceCondition);

    RegionPresenceCondition cond1 = (RegionPresenceCondition) pCond1;
    RegionPresenceCondition cond2 = (RegionPresenceCondition) pCond2;

    return new RegionPresenceCondition(mgr.makeOr(cond1.getRegion(), cond2.getRegion()));
  }

  @Override
  public boolean checkConjunction(PresenceCondition pCond1, PresenceCondition pCond2)
      throws InterruptedException {
    Preconditions.checkArgument(pCond1 instanceof RegionPresenceCondition);
    Preconditions.checkArgument(pCond2 instanceof RegionPresenceCondition);

    RegionPresenceCondition cond1 = (RegionPresenceCondition) pCond1;
    RegionPresenceCondition cond2 = (RegionPresenceCondition) pCond2;

    return !mgr.makeAnd(cond1.getRegion(), cond2.getRegion()).isFalse();
  }

  @Override
  public boolean checkSat(PresenceCondition pCond) {
    Preconditions.checkArgument(mgr instanceof NamedRegionManager);
    Preconditions.checkArgument(pCond instanceof RegionPresenceCondition);

    RegionPresenceCondition cond = (RegionPresenceCondition) pCond;

    return !cond.getRegion().isFalse();
  }

  @Override
  public PresenceCondition makeAnd(PresenceCondition pCond1, PresenceCondition pCond2) {
    Preconditions.checkArgument(pCond1 instanceof RegionPresenceCondition);
    Preconditions.checkArgument(pCond2 instanceof RegionPresenceCondition);

    RegionPresenceCondition cond1 = (RegionPresenceCondition) pCond1;
    RegionPresenceCondition cond2 = (RegionPresenceCondition) pCond2;

    return new RegionPresenceCondition(mgr.makeAnd(cond1.getRegion(), cond2.getRegion()));
  }

  @Override
  public Appender dump(PresenceCondition pCond) {
    Preconditions.checkArgument(mgr instanceof NamedRegionManager);
    Preconditions.checkArgument(pCond instanceof RegionPresenceCondition);

    RegionPresenceCondition cond = (RegionPresenceCondition) pCond;
    NamedRegionManager nmgr = (NamedRegionManager) mgr;

    return nmgr.dumpRegion(cond.getRegion());
  }

  @Override
  public boolean checkEqualsTrue(PresenceCondition pCond) {
    Preconditions.checkArgument(mgr instanceof NamedRegionManager);
    Preconditions.checkArgument(pCond instanceof RegionPresenceCondition);

    RegionPresenceCondition cond = (RegionPresenceCondition) pCond;

    return cond.getRegion().isTrue();
  }

  @Override
  public boolean checkEqualsFalse(PresenceCondition pCond) {
    Preconditions.checkArgument(mgr instanceof NamedRegionManager);
    Preconditions.checkArgument(pCond instanceof RegionPresenceCondition);

    RegionPresenceCondition cond = (RegionPresenceCondition) pCond;

    return cond.getRegion().isFalse();
  }

  public RegionManager getMgr() {
    return mgr;
  }

  private Set<PresenceCondition> extractPredicates(PresenceCondition pCond) {
    Preconditions.checkArgument(pCond instanceof RegionPresenceCondition);

    RegionPresenceCondition cond = (RegionPresenceCondition) pCond;
    Set<PresenceCondition> predicates = Sets.newLinkedHashSet();

    /*for (Region predicate : mgr.extractPredicates(cond.getRegion())) {
      RegionPresenceCondition pc = new RegionPresenceCondition(predicate);
      predicates.add(pc);
    }*/

    return predicates;
  }

  private PresenceCondition makeExists(PresenceCondition pF1, List<PresenceCondition> pF2) {
    Preconditions.checkArgument(pF1 instanceof RegionPresenceCondition);
    for (PresenceCondition pc : pF2) {
      Preconditions.checkArgument(pc instanceof RegionPresenceCondition);
    }

    RegionPresenceCondition cond = (RegionPresenceCondition) pF1;
    Region pF1Region = cond.getRegion();


    Region[] pF2Regions = new Region[pF2.size()];
    int i = 0;
    for (RegionPresenceCondition cond2: Iterables.filter(pF2, RegionPresenceCondition.class)) {
      pF2Regions[i++] = cond2.getRegion();
    }

    Region exists = mgr.makeExists(pF1Region, pF2Regions);

    return new RegionPresenceCondition(exists);
  }

  @Override
  public PresenceCondition removeMarkerVariables(PresenceCondition pPc) {
    Preconditions.checkNotNull(pPc);

    Set<PresenceCondition> predicates = extractPredicates(pPc);

    List<PresenceCondition> markers = Lists.newArrayList();

    for (PresenceCondition region : predicates) {
      String regionStr = dump(region).toString();
      if (regionStr.contains("_MARKER_")) {
        markers.add(region);
      }
    }

    return makeExists(pPc, markers);
  }

}
