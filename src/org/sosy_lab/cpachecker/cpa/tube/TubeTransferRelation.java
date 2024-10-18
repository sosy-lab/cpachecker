// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.tube;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

/**
 * Represents the transfer relation for handling abstract successors of TubeState objects based on
 * CFA edges.
 */
public class TubeTransferRelation extends SingleEdgeTransferRelation {
  /**
   * Retrieves the abstract successors for a given CFA edge based on the provided element,
   * precision, and edge.
   *
   * @param element the abstract state before the edge transition
   * @param prec the precision to consider for the transition
   * @param cfaEdge the edge in the CFA for which successors are to be computed
   * @return a collection of TubeState objects representing the abstract successors
   */
  @Override
  public Collection<TubeState> getAbstractSuccessorsForEdge(
      AbstractState element, Precision prec, CFAEdge cfaEdge) {
    TubeState tubeState = (TubeState) element;
    TubeState initialTubeState =
        new TubeState(
            cfaEdge,
            tubeState.getAsserts(),
            tubeState.getBooleanExp(),
            tubeState.getIsNegated(),
            tubeState.getErrorCounter(),
            tubeState.getSupplier(),
            tubeState.getLogManager(),
            tubeState.getCfa());

    if (cfaEdge.getCode().contains("reach_error();")
        && (cfaEdge.getEdgeType().equals(CFAEdgeType.StatementEdge)
            || (cfaEdge.getCode().contains("reach_error();")
                && cfaEdge.getEdgeType().equals(CFAEdgeType.FunctionCallEdge)))) {
      initialTubeState.incrementErrorCounter();
      return ImmutableSet.of(initialTubeState);
    } else if (initialTubeState.getAsserts().containsKey(cfaEdge.getLineNumber())
        && cfaEdge.getCode().contains("__VERIFIER_nondet")) {
      return getSuccessorsForAssertions(initialTubeState, cfaEdge);
    } else {
      return ImmutableSet.of(initialTubeState);
    }
  }

  /**
   * Retrieves the successors for assertions based on the initialTubeState and CFAEdge.
   *
   * @param initialTubeState the initial TubeState before the edge transition
   * @param cfaEdge the CFAEdge for which successors are to be computed
   * @return a collection of TubeState objects representing the successors with assertions
   */
  private Collection<TubeState> getSuccessorsForAssertions(
      TubeState initialTubeState, CFAEdge cfaEdge) {
    String exp = initialTubeState.getAssertAtLine(cfaEdge.getLineNumber(), false);
    String negExp = initialTubeState.getAssertAtLine(cfaEdge.getLineNumber(), true);

    TubeState successor =
        new TubeState(
            cfaEdge,
            initialTubeState.getAsserts(),
            exp,
            initialTubeState.getIsNegated(),
            initialTubeState.getErrorCounter(),
            initialTubeState.getSupplier(),
            initialTubeState.getLogManager(),
            initialTubeState.getCfa());
    TubeState successor2 =
        new TubeState(
            cfaEdge,
            initialTubeState.getAsserts(),
            negExp,
            true,
            initialTubeState.getErrorCounter(),
            initialTubeState.getSupplier(),
            initialTubeState.getLogManager(),
            initialTubeState.getCfa());
    return ImmutableSet.of(successor, successor2);
  }
}
