// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants;

import com.google.common.collect.FluentIterable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public interface CandidateInvariant {

  /**
   * Gets the uninstantiated invariant formula.
   *
   * @param pFMGR the formula manager.
   * @param pPFMGR the path formula manager.
   * @param pContext the path formula context.
   * @return the uninstantiated invariant formula.
   * @throws CPATransferException if a CPA transfer required to produce the assertion failed.
   * @throws InterruptedException if the formula creation was interrupted.
   */
  BooleanFormula getFormula(
      FormulaManagerView pFMGR, PathFormulaManager pPFMGR, @Nullable PathFormula pContext)
      throws CPATransferException, InterruptedException;

  /**
   * Creates an assertion of the invariant over the given reached set, using the given formula
   * managers.
   *
   * @param pReachedSet the reached set to assert the invariant over.
   * @param pFMGR the formula manager.
   * @param pPFMGR the path formula manager.
   * @return the assertion.
   * @throws CPATransferException if a CPA transfer required to produce the assertion failed.
   * @throws InterruptedException if the formula creation was interrupted.
   */
  BooleanFormula getAssertion(
      Iterable<AbstractState> pReachedSet, FormulaManagerView pFMGR, PathFormulaManager pPFMGR)
      throws CPATransferException, InterruptedException;

  /**
   * Assume that the invariant holds and remove states from the given reached set that must
   * therefore be unreachable.
   *
   * @param pReachedSet the reached set to remove unreachable states from.
   */
  void assumeTruth(ReachedSet pReachedSet);

  boolean appliesTo(CFANode pLocation);

  /**
   * Filters the given states to those this candidate invariant can be applied to.
   *
   * @param pStates the states to filter.
   * @return the filtered states.
   */
  default Iterable<AbstractState> filterApplicable(Iterable<AbstractState> pStates) {
    return FluentIterable.from(pStates)
        .filter(
            s -> FluentIterable.from(AbstractStates.extractLocations(s)).anyMatch(this::appliesTo));
  }
}
