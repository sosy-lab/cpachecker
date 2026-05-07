// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 CPAchecker contributors
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

/**
 * Conjunction that is evaluated state-wise: for every applicable state, all applicable operands
 * must hold on that state.
 *
 * <p>This is used for non-termination mode, where loop continuation requires all relevant
 * continuation guards to hold. For example, {@code while (x != 0 && x != -1)} should become {@code
 * x != 0 && x != -1}, not {@code x != 0 || x != -1}.
 */
public final class StatewiseCandidateInvariantConjunction implements CandidateInvariant {

  private final ImmutableSet<CandidateInvariant> operands;

  public StatewiseCandidateInvariantConjunction(Iterable<? extends CandidateInvariant> pOperands) {
    operands = ImmutableSet.copyOf(pOperands);
    checkArgument(!operands.isEmpty(), "Expected at least one operand.");
  }

  ImmutableSet<CandidateInvariant> getOperands() {
    return operands;
  }

  @Override
  public BooleanFormula getFormula(
      FormulaManagerView pFMGR, PathFormulaManager pPFMGR, @Nullable PathFormula pContext)
      throws CPATransferException, InterruptedException {
    BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
    BooleanFormula result = bfmgr.makeTrue();
    for (CandidateInvariant operand : operands) {
      result = bfmgr.and(result, operand.getFormula(pFMGR, pPFMGR, pContext));
    }
    return result;
  }

  @Override
  public BooleanFormula getAssertion(
      Iterable<AbstractState> pReachedSet, FormulaManagerView pFMGR, PathFormulaManager pPFMGR)
      throws CPATransferException, InterruptedException {
    BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
    List<BooleanFormula> assertions = new ArrayList<>();

    for (AbstractState state : filterApplicable(pReachedSet)) {
      PredicateAbstractState predicateState =
          AbstractStates.extractStateByType(state, PredicateAbstractState.class);
      if (predicateState == null) {
        continue;
      }

      PathFormula pathFormula = predicateState.getPathFormula();
      BooleanFormula stateFormula = pathFormula.getFormula();
      if (bfmgr.isFalse(stateFormula)) {
        continue;
      }

      BooleanFormula stateCandidate = bfmgr.makeTrue();
      SSAMap ssaMap = pathFormula.getSsa().withDefault(1);
      for (CandidateInvariant operand : getApplicableOperands(state)) {
        stateCandidate =
            bfmgr.and(
                stateCandidate,
                pFMGR.instantiate(operand.getFormula(pFMGR, pPFMGR, pathFormula), ssaMap));
      }
      assertions.add(bfmgr.or(bfmgr.not(stateFormula), stateCandidate));
    }

    return bfmgr.and(assertions);
  }

  @Override
  public void assumeTruth(ReachedSet pReachedSet) {
    // Keep non-termination conjunctions observational only.
  }

  @Override
  public boolean appliesTo(CFANode pLocation) {
    return operands.stream().anyMatch(operand -> operand.appliesTo(pLocation));
  }

  @Override
  public Iterable<AbstractState> filterApplicable(Iterable<AbstractState> pStates) {
    return FluentIterable.from(pStates).filter(state -> !getApplicableOperands(state).isEmpty());
  }

  @Override
  public int hashCode() {
    return operands.hashCode();
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof StatewiseCandidateInvariantConjunction other
        && operands.equals(other.operands);
  }

  @Override
  public String toString() {
    return operands.stream().map(Object::toString).collect(Collectors.joining(" and "));
  }

  private ImmutableSet<CandidateInvariant> getApplicableOperands(AbstractState pState) {
    ImmutableSet.Builder<CandidateInvariant> applicable = ImmutableSet.builder();
    for (CandidateInvariant operand : operands) {
      if (!Iterables.isEmpty(operand.filterApplicable(Collections.singleton(pState)))) {
        applicable.add(operand);
      }
    }
    return applicable.build();
  }
}
