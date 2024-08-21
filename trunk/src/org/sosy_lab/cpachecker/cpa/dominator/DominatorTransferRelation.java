// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.dominator;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Transfer relation of {@link DominatorCPA}.
 *
 * <p>Computes the set of dominators of the successor location of a given edge. It only considers
 * the dominators of the predecessor, so if multiple predecessors to a location exist, the returned
 * dominator state is only an intermediate computation step. Multiple dominator states are
 * automatically combined to the final set of dominators through the merge^join operator that uses
 * {@link DominatorState#join(DominatorState)}. Thus, BFS traversal must be used to ensure that the
 * {@link DominatorState} at a program location represents the correct, final state of dominators of
 * that location. Otherwise, it can only be ensured that states represent the final dominators once
 * the CPA algorithm is finished.
 *
 * @see DominatorCPA
 * @see DominatorState
 */
public class DominatorTransferRelation extends SingleEdgeTransferRelation {

  @Override
  public Collection<DominatorState> getAbstractSuccessorsForEdge(
      AbstractState element, Precision prec, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {

    assert element instanceof DominatorState;

    DominatorState dominatorState = (DominatorState) element;
    Set<CFANode> newDominators = new HashSet<>(dominatorState);
    // We have to go through the predecessor to get here
    newDominators.add(cfaEdge.getPredecessor());
    DominatorState successor = new DominatorState(newDominators);

    return Collections.singleton(successor);
  }
}
