// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic;

import static com.google.common.collect.FluentIterable.from;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicValues;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Strengthener for ValueAnalysis with {@link ConstraintsCPA}. */
@Options(prefix = "cpa.value.symbolic")
public class ConstraintsStrengthenOperator implements Statistics {

  @Option(description = "Whether to simplify symbolic expressions, if possible.")
  private boolean simplifySymbolics = true;

  // statistics
  private final Timer totalTime = new Timer();
  private int replacedSymbolicExpressions = 0;

  private final LogManager logger;

  public ConstraintsStrengthenOperator(final Configuration pConfig, final LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    logger = pLogger;
  }

  /**
   * Strengthen the given {@link org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState} with the
   * given {@link org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState}.
   *
   * <p>The returned <code>Collection</code> contains all reachable states after strengthening. A
   * returned empty <code>Collection</code> represents 'bottom', a returned <code>null</code>
   * represents that no changes were made to the given <code>ValueAnalysisState</code>.
   *
   * @param pStateToStrengthen the state to strengthen
   * @param pStrengtheningState the state to strengthen the first state with
   * @return <code>null</code> if no changes were made to the given <code>ValueAnalysisState</code>,
   *     an empty <code>Collection</code>, if the resulting state is not reachable and a <code>
   *     Collection</code> containing all reachable states, otherwise
   */
  public Collection<ValueAnalysisState> strengthen(
      final ValueAnalysisState pStateToStrengthen,
      final ConstraintsState pStrengtheningState,
      final CFAEdge pEdge) {
    totalTime.start();
    try {
      ValueAnalysisState newState = pStateToStrengthen;

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

  // replaces symbolic expressions that are not used anywhere yet with a new symbolic identifier.
  // this method does not copy the given value analysis state, but works directly with it
  private ValueAnalysisState simplifySymbolicValues(
      final ValueAnalysisState pValueState,
      final ConstraintsState pConstraints,
      final CFAEdge pEdge) {

    // If the current edge is an assume edge, simplification doesn't work reliable since
    // a constraint added at this edge is not yet in the strengthening ConstraintsState.
    // For strengthening, unstrengthened states are used, always.
    if (pEdge.getEdgeType() != CFAEdgeType.AssumeEdge) {
      return pValueState;
    }

    final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();

    for (Entry<MemoryLocation, ValueAndType> e : pValueState.getConstants()) {
      Value currV = e.getValue().getValue();
      Type valueType = e.getValue().getType();

      if (!(currV instanceof SymbolicValue) || isSimpleSymbolicValue((SymbolicValue) currV)) {
        continue;
      }

      SymbolicValue castVal = (SymbolicValue) currV;
      MemoryLocation currLoc = e.getKey();

      if (isIndependentInValueState(castVal, currLoc, pValueState)
          && doesNotAppearInConstraints(castVal, pConstraints)) {
        SymbolicValue newIdentifier =
            factory.asConstant(factory.newIdentifier(e.getKey()), valueType);
        pValueState.assignConstant(currLoc, newIdentifier, valueType);
        logger.log(Level.FINE, "Replaced %s with %s", currV, newIdentifier);
        replacedSymbolicExpressions++;
      }
    }

    return pValueState;
  }

  private boolean doesNotAppearInConstraints(
      final SymbolicValue pValue, final ConstraintsState pConstraints) {
    Collection<SymbolicIdentifier> identifiersInValue =
        SymbolicValues.getContainedSymbolicIdentifiers(pValue);

    Collection<SymbolicIdentifier> identifiersInConstraints =
        SymbolicValues.getContainedSymbolicIdentifiers(pConstraints);

    return containsAnyOf(identifiersInConstraints, identifiersInValue);
  }

  private boolean isSimpleSymbolicValue(final SymbolicValue pValue) {
    return pValue instanceof SymbolicIdentifier
        || pValue instanceof ConstantSymbolicExpression
        || pValue instanceof Constraint;
  }

  private boolean isIndependentInValueState(
      final SymbolicValue pValue, final MemoryLocation pMemLoc, final ValueAnalysisState pState) {

    ValueAnalysisState stateWithoutValue = ValueAnalysisState.copyOf(pState);
    stateWithoutValue.forget(pMemLoc);

    Collection<SymbolicIdentifier> identifiersInValue =
        SymbolicValues.getContainedSymbolicIdentifiers(pValue);

    Collection<SymbolicIdentifier> identifiersInState = getIdentifiersInState(pState);

    return !containsAnyOf(identifiersInState, identifiersInValue);
  }

  private boolean containsAnyOf(
      final Collection<SymbolicIdentifier> pContainer,
      final Collection<SymbolicIdentifier> pSelection) {
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

  private Collection<SymbolicIdentifier> getIdentifiersInState(final ValueAnalysisState pState) {
    return from(pState.getConstants())
        .transform(e -> e.getValue().getValue())
        .filter(SymbolicValue.class)
        .transformAndConcat(SymbolicValues::getContainedSymbolicIdentifiers)
        .copyInto(new HashSet<>());
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
