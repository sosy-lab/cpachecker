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
package org.sosy_lab.cpachecker.cpa.modifications;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class ModificationsTransferRelation extends SingleEdgeTransferRelation {

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    ModificationsState locations = (ModificationsState) pState;

    if (!locations.hasModification()) {
      CFANode nodeInGiven = locations.getLocationInGivenCfa();
      CFANode nodeInOriginal = locations.getLocationInOriginalCfa();

      if (CFAUtils.allLeavingEdges(nodeInGiven).contains(pCfaEdge)) {
        CFANode succInGiven = pCfaEdge.getSuccessor();
        Collection<ModificationsState> successors = new HashSet<>();
        for (CFAEdge edgeInOriginal : CFAUtils.allLeavingEdges(nodeInOriginal)) {
          if (edgesMatch(pCfaEdge, edgeInOriginal)) {
            // We assume that the edges leaving a node are disjunct.
            // Otherwise, we'll have to collect the set of differential states here
            // and return all possibilities
            successors.add(new ModificationsState(succInGiven, edgeInOriginal.getSuccessor()));
            break;
          }
        }

        // If no outgoing edge matched, add all outgoing edges to list of modified edges
        if (successors.isEmpty()) {
          for (CFAEdge edgeInOriginal : CFAUtils.allLeavingEdges(nodeInOriginal)) {
            successors.add(
                new ModificationsState(succInGiven, edgeInOriginal.getSuccessor(), true));
          }
        }

        assert !successors.isEmpty()
            : "List of successors should never be empty if previous state represents no modification";
        return successors;
      }
    }

    // if current location doesn't have edge as outgoing edge, or
    // if previous state already depicts modification
    return Collections.emptySet();
  }

  private boolean edgesMatch(CFAEdge pEdgeInGiven, CFAEdge pEdgeInOriginal) {
    String firstAst = pEdgeInGiven.getRawStatement();
    String sndAst = pEdgeInOriginal.getRawStatement();

    return firstAst.equals(sndAst);
  }
}
