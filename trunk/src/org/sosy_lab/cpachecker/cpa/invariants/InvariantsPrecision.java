/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.invariants;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Queue;
import java.util.Set;


public class InvariantsPrecision implements Precision {

  public static InvariantsPrecision getEmptyPrecision(
      AbstractionStrategy pAbstractionStrategy) {
    return new InvariantsPrecision(
        Collections.<CFAEdge>emptySet(),
        Collections.<MemoryLocation>emptySet(),
        0,
        pAbstractionStrategy) {

      @Override
      public boolean isRelevant(CFAEdge pEdge) {
        return true;
      }

      @Override
      public String toString() {
        return "no precision";
      }

    };
  }

  private final ImmutableSet<CFAEdge> relevantEdges;

  private final ImmutableSet<MemoryLocation> interestingVariables;

  private final int maximumFormulaDepth;

  private final AbstractionStrategy abstractionStrategy;

  public InvariantsPrecision(Set<CFAEdge> pRelevantEdges,
      Set<MemoryLocation> pInterestingVariables, int pMaximumFormulaDepth,
      AbstractionStrategy pAbstractionStrategy) {
    this(asImmutableRelevantEdges(pRelevantEdges),
        ImmutableSet.<MemoryLocation>copyOf(pInterestingVariables),
        pMaximumFormulaDepth,
        pAbstractionStrategy);
  }

  public InvariantsPrecision(ImmutableSet<CFAEdge> pRelevantEdges,
      ImmutableSet<MemoryLocation> pInterestingVariables, int pMaximumFormulaDepth,
      AbstractionStrategy pAbstractionStrategy) {
    this.relevantEdges = pRelevantEdges;
    this.interestingVariables = pInterestingVariables;
    this.maximumFormulaDepth = pMaximumFormulaDepth;
    this.abstractionStrategy = pAbstractionStrategy;
  }

  public boolean isRelevant(CFAEdge pEdge) {
    return pEdge != null && (this.relevantEdges == null || this.relevantEdges.contains(pEdge));
  }

  public Set<MemoryLocation> getInterestingVariables() {
    return this.interestingVariables;
  }

  @Override
  public String toString() {
    return String.format("Number of relevant edges: %d; Interesting variables: %s;", this.relevantEdges.size(), this.interestingVariables);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof InvariantsPrecision) {
      InvariantsPrecision other = (InvariantsPrecision) pOther;
      return relevantEdges.equals(other.relevantEdges)
          && interestingVariables.equals(other.interestingVariables)
          && maximumFormulaDepth == other.maximumFormulaDepth;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.interestingVariables.hashCode();
  }

  public int getMaximumFormulaDepth() {
    return this.maximumFormulaDepth;
  }

  public AbstractionStrategy getAbstractionStrategy() {
    return this.abstractionStrategy;
  }

  private static ImmutableSet<CFAEdge> asImmutableRelevantEdges(Set<CFAEdge> pRelevantEdges) {
    if (pRelevantEdges == null) {
      return null;
    }
    ImmutableSet.Builder<CFAEdge> builder = ImmutableSet.builder();
    Queue<CFAEdge> waitlist = new ArrayDeque<>(pRelevantEdges);
    while (!waitlist.isEmpty()) {
      CFAEdge relevantEdge = waitlist.poll();
      builder.add(relevantEdge);
    }
    return builder.build();
  }

}
