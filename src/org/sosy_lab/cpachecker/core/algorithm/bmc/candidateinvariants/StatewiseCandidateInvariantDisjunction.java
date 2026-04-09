// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 CPAchecker contributors
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants;

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
 * Disjunction that is evaluated state-wise: for every applicable state, at least one applicable
 * operand must hold on that state.
 *
 * <p>This is useful for BMC termination mode, where one loop may terminate via several different
 * guards (for example the loop head condition or an internal {@code break} guard). Adding those
 * guards as separate candidates would incorrectly require all of them to hold on every frontier
 * state.
 */
public final class StatewiseCandidateInvariantDisjunction implements CandidateInvariant {

  private final ImmutableSet<CandidateInvariant> operands;

  public StatewiseCandidateInvariantDisjunction(Iterable<? extends CandidateInvariant> pOperands) {
    operands = ImmutableSet.copyOf(pOperands);
    if (operands.isEmpty()) {
      throw new IllegalArgumentException("Expected at least one operand.");
    }
  }

  @Override
  public BooleanFormula getFormula(
      FormulaManagerView pFMGR, PathFormulaManager pPFMGR, @Nullable PathFormula pContext)
      throws CPATransferException, InterruptedException {
    BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
    BooleanFormula result = bfmgr.makeFalse();
    for (CandidateInvariant operand : operands) {
      result = bfmgr.or(result, operand.getFormula(pFMGR, pPFMGR, pContext));
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

      BooleanFormula stateCandidate = bfmgr.makeFalse();
      SSAMap ssaMap = pathFormula.getSsa().withDefault(1);
      for (CandidateInvariant operand : getApplicableOperands(state)) {
        stateCandidate =
            bfmgr.or(
                stateCandidate,
                pFMGR.instantiate(operand.getFormula(pFMGR, pPFMGR, pathFormula), ssaMap));
      }
      assertions.add(bfmgr.or(bfmgr.not(stateFormula), stateCandidate));
    }

    return bfmgr.and(assertions);
  }

  @Override
  public void assumeTruth(ReachedSet pReachedSet) {
    // Keep termination disjunctions observational only.
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
    return pOther instanceof StatewiseCandidateInvariantDisjunction other
        && operands.equals(other.operands);
  }

  @Override
  public String toString() {
    return operands.stream().map(Object::toString).collect(Collectors.joining(" or "));
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
