/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * This class provides a RegionManager which additionally keeps track of a name
 * for each predicate, and can provide a nice String representation of a BDD.
 */
public class NamedRegionManager implements RegionManager {

  private final RegionManager delegate;

  private final BiMap<String, Region> regionMap = HashBiMap.create();

  private static final String ANONYMOUS_PREDICATE = "__anon_pred";
  private int anonymousPredicateCounter = 0;

  public NamedRegionManager(RegionManager pDelegate) {
    delegate = checkNotNull(pDelegate);
  }

  /**
   * Create a predicate with a name associated to it.
   * If the same name is passed again to this method, the old predicate will be
   * returned (guaranteeing uniqueness of predicate<->name mapping).
   * @param pName An arbitary name for a predicate.
   * @return A region representing a predicate
   */
  public Region createPredicate(String pName) {
    Region result = regionMap.get(pName);
    if (result == null) {
      result = delegate.createPredicate();
      regionMap.put(pName, result);
    }
    return result;
  }

  @Override
  public Region createPredicate() {
    return createPredicate(ANONYMOUS_PREDICATE + anonymousPredicateCounter++);
  }

  /**
   * Returns a String representation of a region.
   */
  public String dumpRegion(Region r) {
    if (regionMap.containsValue(r)) {
      return regionMap.inverse().get(r);

    } else if (r.isFalse()) {
      return "FALSE";

    } else if (r.isTrue()) {
      return "TRUE";

    } else {
      Triple<Region, Region, Region> triple = delegate.getIfThenElse(r);
      String predName = regionMap.inverse().get(triple.getFirst());

      Region trueBranch = triple.getSecond();
      String ifTrue = "";
      if (trueBranch.isFalse()) {
        // omit
      } else if (trueBranch.isTrue()) {
        ifTrue = predName;
      } else {
        ifTrue = predName + " & " + dumpRegion(trueBranch);
      }

      Region falseBranch = triple.getThird();
      String ifFalse = "";
      if (falseBranch.isFalse()) {
        // omit
      } else if (falseBranch.isTrue()) {
        ifFalse = "!" + predName;
      } else {
        ifFalse = "!" + predName + " & " + dumpRegion(falseBranch);
      }

      if (!ifTrue.isEmpty() && !ifFalse.isEmpty()) {
        return "((" + ifTrue + ") | (" + ifFalse + "))";
      } else if (ifTrue.isEmpty()) {
        return ifFalse;
      } else if (ifFalse.isEmpty()) {
        return ifTrue;

      } else {
        throw new AssertionError("Both BDD Branches are empty!?");
      }
    }
  }

  @Override
  public boolean entails(Region pF1, Region pF2) {
    return delegate.entails(pF1, pF2);
  }

  @Override
  public Region makeTrue() {
    return delegate.makeTrue();
  }

  @Override
  public Region makeFalse() {
    return delegate.makeFalse();
  }

  @Override
  public Region makeNot(Region pF) {
    return delegate.makeNot(pF);
  }

  @Override
  public Region makeAnd(Region pF1, Region pF2) {
    return delegate.makeAnd(pF1, pF2);
  }

  @Override
  public Region makeOr(Region pF1, Region pF2) {
    return delegate.makeOr(pF1, pF2);
  }

  @Override
  public Region makeExists(Region pF1, Region pF2) {
    return delegate.makeExists(pF1, pF2);
  }

  @Override
  public Triple<Region, Region, Region> getIfThenElse(Region pF) {
    return delegate.getIfThenElse(pF);
  }
}