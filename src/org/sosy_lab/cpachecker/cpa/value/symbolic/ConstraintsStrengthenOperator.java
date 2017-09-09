/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.value.symbolic;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.IdentifierAssignment;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.interval.NumberInterface;
import org.sosy_lab.cpachecker.cpa.interval.UnifyAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicValues;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Strengthener for ValueAnalysis with {@link ConstraintsCPA}.
 */
@Options(prefix = "cpa.value.symbolic")
public class ConstraintsStrengthenOperator implements Statistics {

  @Option(description = "Whether to simplify symbolic expressions, if possible.")
  private boolean simplifySymbolics = true;

  @Option(description = "Whether to adopt definite assignments computed by the ConstraintsCPA")
  private boolean adoptDefinites = true;

  // statistics
  private final Timer totalTime = new Timer();
  private int replacedSymbolicExpressions = 0;


  public ConstraintsStrengthenOperator(final Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  /**
   * Strengthen the given {@link org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState} with the given {@link org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState}.
   *
   * <p>The returned <code>Collection</code> contains all reachable states after strengthening.
   * A returned empty <code>Collection</code> represents 'bottom', a returned <code>null</code>
   * represents that no changes were made to the given <code>ValueAnalysisState</code>.</p>
   *
   *
   * @param pStateToStrengthen the state to strengthen
   * @param pStrengtheningState the state to strengthen the first state with
   * @return <code>null</code> if no changes were made to the given <code>ValueAnalysisState</code>,
   *    an empty <code>Collection</code>, if the resulting state is not reachable and
   *    a <code>Collection</code> containing all reachable states, otherwise
   */
  public Collection<UnifyAnalysisState> strengthen(
      final UnifyAnalysisState pStateToStrengthen,
      final ConstraintsState pStrengtheningState,
      final CFAEdge pEdge
  ) {
    totalTime.start();
    try {
        UnifyAnalysisState newState;

      if (adoptDefinites) {
        newState =
            evaluateAssignment(pStrengtheningState.getDefiniteAssignment(), pStateToStrengthen);

      } else {
        newState = pStateToStrengthen;
      }

      if (simplifySymbolics) {
        newState = simplifySymbolicValues(newState, pStrengtheningState, pEdge);
      }

      if (!newState.equals(pStateToStrengthen)) {
        return Collections.singleton(newState);

      } else {
        return null;
      }
    } finally {
      totalTime.stop();
    }

  }

  private UnifyAnalysisState evaluateAssignment(
      final IdentifierAssignment pAssignment,
      final UnifyAnalysisState pValueState
  ) {

      UnifyAnalysisState newElement = UnifyAnalysisState.copyOf(pValueState);

    for (Map.Entry<? extends SymbolicIdentifier, NumberInterface> onlyValidAssignment : pAssignment.entrySet()) {
      final SymbolicIdentifier identifierToReplace = onlyValidAssignment.getKey();
      final NumberInterface newIdentifierValue = onlyValidAssignment.getValue();

      newElement.assignConstant(identifierToReplace, newIdentifierValue);
    }

    return newElement;
  }

  // replaces symbolic expressions that are not used anywhere yet with a new symbolic identifier.
  // this method does not copy the given value analysis state, but works directly with it
  private UnifyAnalysisState simplifySymbolicValues(
      final UnifyAnalysisState pValueState,
      final ConstraintsState pConstraints,
      final CFAEdge pEdge
  ) {

    // we only simplify symbolic values if one of the possible next edges is an assume edge.
    // otherwise, symbolic values won't be used in one of the next steps and we don't have to
    // simplify.
    // If the current edge is an assume edge, simplification doesn't work reliable since
    // a constraint added at this edge is not yet in the strengthening ConstraintsState.
    // For strengthening, unstrengthened states are used, always.
    if (!couldNextEdgeUseValues(pEdge) && pEdge.getEdgeType() != CFAEdgeType.AssumeEdge) {
      return pValueState;
    }


    final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();

    for (Map.Entry<MemoryLocation, NumberInterface> e : pValueState.getConstantsMapView().entrySet()) {
      NumberInterface currV = e.getValue();

      if (!(currV instanceof SymbolicValue) || isSimpleSymbolicValue((SymbolicValue) currV)) {
        continue;
      }

      SymbolicValue castVal = (SymbolicValue) currV;
      MemoryLocation currLoc = e.getKey();

      if (isIndependentInValueState(castVal, currLoc, pValueState)
          && doesNotAppearInConstraints(castVal, pConstraints)) {

        Type valueType = pValueState.getTypeForMemoryLocation(currLoc);
        SymbolicValue newIdentifier = factory.asConstant(factory.newIdentifier(), valueType);

        pValueState.assignConstant(currLoc, newIdentifier, valueType);
        replacedSymbolicExpressions++;
      }
    }

    return pValueState;
  }

  private boolean couldNextEdgeUseValues(CFAEdge pEdge) {
    final CFANode nextNode = pEdge.getSuccessor();

    for (CFAEdge currEdge : CFAUtils.leavingEdges(nextNode)) {
      if (usesValues(currEdge)) {
        return true;
      }
    }

    return false;
  }

  private boolean usesValues(CFAEdge pCurrEdge) {
    return !pCurrEdge.getEdgeType().equals(CFAEdgeType.BlankEdge);
  }

  private boolean doesNotAppearInConstraints(
      final SymbolicValue pValue,
      final ConstraintsState pConstraints
  ) {
    Collection<SymbolicIdentifier> identifiersInValue =
        SymbolicValues.getContainedSymbolicIdentifiers(pValue);

    Collection<SymbolicIdentifier> identifiersInConstraints =
        SymbolicValues.getContainedSymbolicIdentifiers(pConstraints);

    return containsAnyOf(identifiersInConstraints, identifiersInValue);
  }

  private boolean isSimpleSymbolicValue(final SymbolicValue pValue) {
    return pValue instanceof SymbolicIdentifier || pValue instanceof ConstantSymbolicExpression
        || pValue instanceof Constraint;
  }

  private boolean isIndependentInValueState(
      final SymbolicValue pValue,
      final MemoryLocation pMemLoc,
      final UnifyAnalysisState pState
  ) {

      UnifyAnalysisState stateWithoutValue = UnifyAnalysisState.copyOf(pState);
    stateWithoutValue.forget(pMemLoc);

    Collection<SymbolicIdentifier> identifiersInValue =
        SymbolicValues.getContainedSymbolicIdentifiers(pValue);

    Collection<SymbolicIdentifier> identifiersInState = getIdentifiersInState(pState);

    return !containsAnyOf(identifiersInState, identifiersInValue);
  }

  private boolean containsAnyOf(
      final Collection<SymbolicIdentifier> pContainer,
      final Collection<SymbolicIdentifier> pSelection
  ) {
    Collection<SymbolicIdentifier> smallerCollection;
    Collection<SymbolicIdentifier> biggerCollection;

    if (pContainer.size() <= pSelection.size()) {
      smallerCollection = pContainer;
      biggerCollection = pSelection;
    } else {
      smallerCollection = pSelection;
      biggerCollection = pContainer;
    }

    for (SymbolicIdentifier i : smallerCollection) {
      if (biggerCollection.contains(i)) {
        return true;
      }
    }

    return false;
  }

  private Collection<SymbolicIdentifier> getIdentifiersInState(final UnifyAnalysisState pState) {
    Collection<SymbolicIdentifier> ret = new HashSet<>();

    for (NumberInterface v : pState.getConstantsMapView().values()) {
      if (v instanceof SymbolicValue) {
        ret.addAll(SymbolicValues.getContainedSymbolicIdentifiers((SymbolicValue) v));
      }
    }

    return ret;
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    out.println("Total time for strengthening by ConstraintsCPA: " + totalTime);
    out.println("Replaced symbolic expressions: " + replacedSymbolicExpressions);
  }

  @Nullable
  @Override
  public String getName() {
    return ConstraintsStrengthenOperator.this.getClass().getSimpleName();
  }
}
