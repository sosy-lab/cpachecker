// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

class InvariantsPrecision implements Precision {

  public static InvariantsPrecision getEmptyPrecision(
      AbstractionStrategy pAbstractionStrategy) {
    return new InvariantsPrecision(
        ImmutableSet.of(), ImmutableSet.of(), 0, pAbstractionStrategy, false) {

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

  private final boolean useMod2Template;

  public InvariantsPrecision(
      Set<CFAEdge> pRelevantEdges,
      Set<MemoryLocation> pInterestingVariables,
      int pMaximumFormulaDepth,
      AbstractionStrategy pAbstractionStrategy,
      boolean pUseMod2Template) {
    this(
        pRelevantEdges == null ? null : ImmutableSet.copyOf(pRelevantEdges),
        ImmutableSet.<MemoryLocation>copyOf(pInterestingVariables),
        pMaximumFormulaDepth,
        pAbstractionStrategy,
        pUseMod2Template);
  }

  public InvariantsPrecision(
      ImmutableSet<CFAEdge> pRelevantEdges,
      ImmutableSet<MemoryLocation> pInterestingVariables,
      int pMaximumFormulaDepth,
      AbstractionStrategy pAbstractionStrategy,
      boolean pUseMod2Template) {
    this.relevantEdges = pRelevantEdges;
    this.interestingVariables = pInterestingVariables;
    this.maximumFormulaDepth = pMaximumFormulaDepth;
    this.abstractionStrategy = pAbstractionStrategy;
    this.useMod2Template = pUseMod2Template;
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

  public boolean shouldUseMod2Template() {
    return useMod2Template;
  }
}
