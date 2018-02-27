/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
    AbstractState element, Precision prec, CFAEdge cfaEdge) throws CPATransferException, InterruptedException {

    assert element instanceof DominatorState;

    DominatorState dominatorState = (DominatorState) element;
    Set<CFANode> newDominators = new HashSet<>(dominatorState);
    // We have to go through the predecessor to get here
    newDominators.add(cfaEdge.getPredecessor());
    DominatorState successor = new DominatorState(newDominators);

    return Collections.singleton(successor);
  }
}
