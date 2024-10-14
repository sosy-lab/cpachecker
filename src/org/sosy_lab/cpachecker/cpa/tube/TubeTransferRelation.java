// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.tube;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.java_smt.api.BooleanFormula;

import java.util.Collection;


/**
 * Represents the transfer relation for the Tube CPA.
 *
 * <p>This class extends the {@link SingleEdgeTransferRelation} abstract base class, which
 * eliminates the need to implement a stub for {@link #getAbstractSuccessors(AbstractState,
 * Precision)}.
 */
public class TubeTransferRelation extends SingleEdgeTransferRelation {
  /**
   * Retrieves the abstract successors for a given edge.
   *
   * @param element  The abstract state at the start of the edge
   * @param prec     The precision for the analysis
   * @param cfaEdge  The edge in the control flow graph
   * @return The collection of abstract successors for the edge
   */
  @Override
  public Collection<TubeState> getAbstractSuccessorsForEdge(
          AbstractState element, Precision prec, CFAEdge cfaEdge) {
    TubeState tubeState = (TubeState) element;
    TubeState initialTubeState = new TubeState(cfaEdge,tubeState.getAsserts(), null,
        tubeState.isNegated(), tubeState.getErrorCounter(), tubeState.getSupplier(), tubeState.getLogManager(),
        tubeState.getCfa());

    if (cfaEdge.getCode().contains("reach_error();")&&(cfaEdge.getEdgeType().equals(CFAEdgeType.StatementEdge) || cfaEdge.getEdgeType().equals(CFAEdgeType.FunctionCallEdge))) {
      return getSuccessorForError(initialTubeState);
    }
    if(initialTubeState.getAsserts().containsKey(cfaEdge.getLineNumber()) && cfaEdge.getCode().contains("__VERIFIER_nondet")) {
      return getSuccessorsForAssertions(initialTubeState, cfaEdge);
    } else {
      return ImmutableSet.of(initialTubeState);
    }
  }

  /**
   * Returns the successors of a TubeState when an error is encountered.
   *
   * @param initialTubeState The initial TubeState
   * @return The collection of successors after encountering an error
   */
  private Collection<TubeState> getSuccessorForError(TubeState initialTubeState) {
    initialTubeState.incrementErrorCounter();
    return ImmutableSet.of(initialTubeState);
  }

  /**
   * Returns the successors for assertions in the Tube CPA.
   *
   * @param initialTubeState The initial TubeState for which the successors are computed
   * @param cfaEdge The CFAEdge for which the successors are computed
   * @return The collection of TubeState successors for the assertions
   */
  private Collection<TubeState> getSuccessorsForAssertions(TubeState initialTubeState, CFAEdge cfaEdge) {
    String exp = initialTubeState.getAssertAtLine(cfaEdge.getLineNumber(), false);
    String negExp = initialTubeState.getAssertAtLine(cfaEdge.getLineNumber(), true);
    TubeState successor = new TubeState(cfaEdge, initialTubeState.getAsserts(), exp, initialTubeState.isNegated(), initialTubeState.getErrorCounter(), initialTubeState.getSupplier(),initialTubeState.getLogManager(),
        initialTubeState.getCfa());
    TubeState successor2 = new TubeState(cfaEdge, initialTubeState.getAsserts(), negExp, true, initialTubeState.getErrorCounter(), initialTubeState.getSupplier(),initialTubeState.getLogManager(),
        successor.getCfa());
    return ImmutableSet.of(successor, successor2);
  }
}

