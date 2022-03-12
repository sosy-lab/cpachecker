// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces.pcc;

import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/** Interface for classes representing an analysis that can be proof checked. */
public interface ProofChecker {
  /**
   * Checks whether the given set of abstract successors correctly over-approximates the set of
   * concrete successors the concretisations of the given abstract state has with respect to the
   * given CFA edge. If the given edge is <code>null</code> all CFA edges have to be considered.
   *
   * @param state abstract state with current state
   * @param cfaEdge null or an edge of the CFA
   * @param successors list of all successors of the current state (may be empty)
   * @return <code>true</code> if successors are valid over-approximation; <code>false</code>,
   *     otherwise.
   */
  boolean areAbstractSuccessors(
      AbstractState state, CFAEdge cfaEdge, Collection<? extends AbstractState> successors)
      throws CPATransferException, InterruptedException;

  /**
   * Checks whether the given state is covered by an other state. That is, the set of
   * concretisations of the state has to be a subset of the set of concretisations of the other
   * state.
   */
  boolean isCoveredBy(AbstractState state, AbstractState otherState)
      throws CPAException, InterruptedException;

  /** sub-interface to avoid several copies of identical code. */
  interface ProofCheckerCPA extends ConfigurableProgramAnalysis, ProofChecker {

    @Override
    default boolean areAbstractSuccessors(
        AbstractState pState, CFAEdge pCfaEdge, Collection<? extends AbstractState> pSuccessors)
        throws CPATransferException, InterruptedException {
      try {
        Collection<? extends AbstractState> computedSuccessors =
            getTransferRelation()
                .getAbstractSuccessorsForEdge(pState, SingletonPrecision.getInstance(), pCfaEdge);
        boolean found;
        for (AbstractState comp : computedSuccessors) {
          found = false;
          for (AbstractState e : pSuccessors) {
            if (isCoveredBy(comp, e)) {
              found = true;
              break;
            }
          }
          if (!found) {
            return false;
          }
        }
      } catch (CPAException e) {
        throw new CPATransferException("Cannot compare abstract successors", e);
      }
      return true;
    }

    @Override
    default boolean isCoveredBy(AbstractState state, AbstractState otherState)
        throws CPAException, InterruptedException {
      return getAbstractDomain().isLessOrEqual(state, otherState);
    }
  }
}
