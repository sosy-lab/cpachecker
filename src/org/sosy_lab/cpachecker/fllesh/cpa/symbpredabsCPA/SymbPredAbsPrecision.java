/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA;

import java.util.Collection;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.interfaces.Predicate;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;

public class SymbPredAbsPrecision implements Precision {

  // TODO change
  private static final boolean absOnFunction = true;
  private static final boolean absOnLoop = true;
  private static final boolean absOnlyIfBoth = false;
  
  private final SetMultimap<CFANode, Predicate> predicateMap;
  private final Set<Predicate> globalPredicates;

  public SymbPredAbsPrecision(ImmutableSetMultimap<CFANode, Predicate> predicateMap, Collection<Predicate> globalPredicates) {
    assert predicateMap != null;
    this.predicateMap = predicateMap;
    this.globalPredicates = ImmutableSet.copyOf(globalPredicates);
  }

  public SymbPredAbsPrecision(Collection<Predicate> globalPredicates) {
    predicateMap = ImmutableSetMultimap.of();
    this.globalPredicates = (globalPredicates == null ? ImmutableSet.<Predicate>of() : ImmutableSet.copyOf(globalPredicates));
  }

  public SetMultimap<CFANode, Predicate> getPredicateMap() {
    return predicateMap;
  }

  public Set<Predicate> getGlobalPredicates() {
    return globalPredicates;
  }
  
  public Set<Predicate> getPredicates(CFANode loc) {
    Set<Predicate> result = predicateMap.get(loc);
    if (result == null) {
      result = globalPredicates;
    }
    return result;
  }

  @Override
  public int hashCode() {
    return predicateMap.hashCode();
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) {
      return true;
    } else if (!(pObj instanceof SymbPredAbsPrecision)) {
      return false;
    } else {
      return predicateMap.equals(((SymbPredAbsPrecision)pObj).predicateMap);
    }
  }

  @Override
  public String toString() {
    return predicateMap.toString();
  }
  
  @Override
  public boolean isBreak() {
    return false;
  }
  
  /**
   * @param succLoc successor CFA location.
   * @param thresholdReached if the maximum block size has been reached
   * @return true if succLoc is an abstraction location. For now a location is 
   * an abstraction location if it has an incoming loop-back edge, if it is
   * the start node of a function or if it is the call site from a function call.
   */
  public boolean isAbstractionLocation(CFANode succLoc, boolean thresholdReached) {
    boolean result = false;
    
    if (absOnLoop) {
      result = succLoc.isLoopStart();
    }
    if (absOnFunction) {
      result = result
            || (succLoc instanceof CFAFunctionDefinitionNode) // function call edge
            || (succLoc.getEnteringSummaryEdge() != null); // function return edge
    }
    
    if (absOnlyIfBoth) {
      result = result && thresholdReached;
    } else {
      result = result || thresholdReached;
    }

    return result;
  }
  
}


