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
package org.sosy_lab.cpachecker.cpa.arg;

import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

public class ARGTransferRelation implements TransferRelation {

  private final TransferRelation transferRelation;

  public ARGTransferRelation(TransferRelation tr) {
    transferRelation = tr;
  }

  @Override
  public Collection<ARGState> getAbstractSuccessors(
      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    ARGState element = (ARGState)pElement;

    // covered elements may be in the reached set, but should always be ignored
    if (element.isCovered()) {
      return Collections.emptySet();
    }

    element.markExpanded();

    AbstractState wrappedState = element.getWrappedState();
    Collection<? extends AbstractState> successors = transferRelation.getAbstractSuccessors(wrappedState, pPrecision, pCfaEdge);
    if (successors.isEmpty()) {
      return Collections.emptySet();
    }

    Collection<ARGState> wrappedSuccessors = new ArrayList<>();
    for (AbstractState absElement : successors) {
      ARGState successorElem = new ARGState(absElement, element);
      wrappedSuccessors.add(successorElem);
    }

    return wrappedSuccessors;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState element,
                         List<AbstractState> otherElements, CFAEdge cfaEdge,
                         Precision precision) {
    return null;
  }

  boolean areAbstractSuccessors(AbstractState pElement, CFAEdge pCfaEdge, Collection<? extends AbstractState> pSuccessors, ProofChecker wrappedProofChecker) throws CPATransferException, InterruptedException {
    ARGState element = (ARGState)pElement;

    assert Iterables.elementsEqual(element.getChildren(), pSuccessors);

    AbstractState wrappedState = element.getWrappedState();
    Multimap<CFAEdge, AbstractState> wrappedSuccessors = HashMultimap.create();
    for (AbstractState absElement : pSuccessors) {
      ARGState successorElem = (ARGState)absElement;
      wrappedSuccessors.put(element.getEdgeToChild(successorElem), successorElem.getWrappedState());
    }

    if (pCfaEdge != null) {
      return wrappedProofChecker.areAbstractSuccessors(wrappedState, pCfaEdge, wrappedSuccessors.get(pCfaEdge));
    }

    CFANode loc = AbstractStates.extractLocation(element);
    for (CFAEdge edge : leavingEdges(loc)) {
      if (!wrappedProofChecker.areAbstractSuccessors(wrappedState, edge, wrappedSuccessors.get(edge))) {
        return false;
      }
    }
    return true;
  }
}
