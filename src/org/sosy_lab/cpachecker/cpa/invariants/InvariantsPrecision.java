/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import java.util.Collections;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;

import com.google.common.collect.ImmutableSet;


public class InvariantsPrecision implements Precision {

  public static final InvariantsPrecision NONE = new InvariantsPrecision(
      Collections.<CFAEdge>emptySet(),
      Collections.<InvariantsFormula<CompoundInterval>>emptySet(),
      Collections.<String>emptySet(), 0, false, false) {

    @Override
    public boolean isRelevant(CFAEdge pEdge) {
      return true;
    }

    @Override
    public String toString() {
      return "no precision";
    }

  };

  private final ImmutableSet<CFAEdge> relevantEdges;

  private final ImmutableSet<InvariantsFormula<CompoundInterval>> interestingAssumptions;

  private final ImmutableSet<String> interestingVariables;

  private final int maximumFormulaDepth;

  private final boolean useBinaryVariableInterrelations;

  private final boolean useAbstractEvaluation;

  public InvariantsPrecision(Set<CFAEdge> pRelevantEdges,
      Set<InvariantsFormula<CompoundInterval>> pInterestingAssumptions,
      Set<String> pInterestingVariables, int pMaximumFormulaDepth,
      boolean pUseBinaryVariableInterrelations,
      boolean pUseAbstractEvaluation) {
    this(pRelevantEdges == null ? null : ImmutableSet.<CFAEdge>copyOf(pRelevantEdges),
        ImmutableSet.<InvariantsFormula<CompoundInterval>>copyOf(pInterestingAssumptions),
        ImmutableSet.<String>copyOf(pInterestingVariables),
        pMaximumFormulaDepth,
        pUseBinaryVariableInterrelations,
        pUseAbstractEvaluation);
  }

  public InvariantsPrecision(ImmutableSet<CFAEdge> pRelevantEdges,
      ImmutableSet<InvariantsFormula<CompoundInterval>> pInterestingAssumptions,
      ImmutableSet<String> pInterestingVariables, int pMaximumFormulaDepth,
      boolean pUseBinaryVariableInterrelations,
      boolean pUseAbstractEvaluation) {
    this.relevantEdges = pRelevantEdges;
    this.interestingAssumptions = pInterestingAssumptions;
    this.interestingVariables = pInterestingVariables;
    this.maximumFormulaDepth = pMaximumFormulaDepth;
    this.useBinaryVariableInterrelations = pUseBinaryVariableInterrelations;
    this.useAbstractEvaluation = pUseAbstractEvaluation;
  }

  public boolean isRelevant(CFAEdge pEdge) {
    return this.relevantEdges == null || this.relevantEdges.contains(pEdge);
  }

  public Set<InvariantsFormula<CompoundInterval>> getInterestingAssumptions() {
    return this.interestingAssumptions;
  }

  public Set<String> getInterestingVariables() {
    return this.interestingVariables;
  }

  @Override
  public String toString() {
    return String.format("Number of relevant edges: %d; Interesting asumptions: %s; Interesting variables: %s;", this.relevantEdges.size(), this.interestingAssumptions, this.interestingVariables);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof InvariantsPrecision) {
      InvariantsPrecision other = (InvariantsPrecision) pOther;
      return relevantEdges.equals(other.relevantEdges)
          && interestingAssumptions.equals(other.interestingAssumptions)
          && interestingVariables.equals(other.interestingVariables)
          && maximumFormulaDepth == other.maximumFormulaDepth
          && useBinaryVariableInterrelations == other.useBinaryVariableInterrelations;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.interestingVariables.hashCode() * 43 + interestingAssumptions.hashCode();
  }

  public int getMaximumFormulaDepth() {
    return this.maximumFormulaDepth;
  }

  public boolean isUsingBinaryVariableInterrelations() {
    return this.useBinaryVariableInterrelations;
  }

  public boolean isUsingAbstractEvaluation() {
    return this.useAbstractEvaluation;
  }

}
