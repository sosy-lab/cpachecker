/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.differential;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.differential.DifferentialCPA.ModificationInfo;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/** Transfer relation of {@link DifferentialCPA}. TODO */
public class DifferentialTransferRelation extends SingleEdgeTransferRelation {

  private final ImmutableSet<CFANode> modifiedReachableFrom;
  private final ImmutableSet<CFANode> modified;

  DifferentialTransferRelation(ModificationInfo pModInfo) {
    modifiedReachableFrom = pModInfo.getNodesModificationReachableFrom();
    modified = pModInfo.getNodesWithModification();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    switch ((DifferentialState) pState) {
      case MODIFIED_NOT_REACHABLE:
        // if the predecessor-states is a state from which modified is not reachable,
        // this is also true for all states dominated by that state.
        // If a state is not dominated by this state, it will be reached through
        // another predecessor-state. So in both cases, it is not necessary to continue
        // analysis
        return Collections.emptySet();

      case MODIFIED:
        // if the predecessor-state is at a modified location,
        // all sucessors may have different values, too, so we can't say anything about them.
        // In this case, stop analysis so that the created assumption automaton
        // doesn't claim any assumptions.
        return Collections.emptySet();

      case MODIFIED_REACHABLE:
        CFANode nextLocation = pCfaEdge.getSuccessor();
        DifferentialState next;
        if (modified.contains(nextLocation)) {
          next = DifferentialState.MODIFIED;

        } else if (modifiedReachableFrom.contains(nextLocation)) {
          next = DifferentialState.MODIFIED_REACHABLE;

        } else {
          next = DifferentialState.MODIFIED_NOT_REACHABLE;
        }
        return Collections.singleton(next);
      default:
        throw new AssertionError("Unhandled state: " + pState);
    }
  }
}
