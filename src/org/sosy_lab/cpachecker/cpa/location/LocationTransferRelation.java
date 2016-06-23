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
package org.sosy_lab.cpachecker.cpa.location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithShadowCode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class LocationTransferRelation implements TransferRelation {

  @Nonnull private final LocationStateFactory factory;

  public LocationTransferRelation(LocationStateFactory pFactory) {
    factory = Preconditions.checkNotNull(pFactory);
  }

  @Override
  public Collection<LocationState> getAbstractSuccessorsForEdge(
      AbstractState pElement, Precision pPrec, CFAEdge pCfaEdge) {

    LocationState inputElement = (LocationState) pElement;
    CFANode node = inputElement.getLocationNode();

    if (CFAUtils.allLeavingEdges(node).contains(pCfaEdge)) {
      return Collections.singleton(factory.getState(pCfaEdge.getSuccessor()));

    } else if (node.getNumLeavingEdges() == 1
        && node.getLeavingEdge(0) instanceof MultiEdge) {
      // maybe we are "entering" a MultiEdge via it's first component edge
      MultiEdge multiEdge = (MultiEdge)node.getLeavingEdge(0);
      if (multiEdge.getEdges().get(0).equals(pCfaEdge)) {
        return Collections.singleton(factory.getState(pCfaEdge.getSuccessor()));
      }
    }

    return Collections.emptySet();
  }

  @Override
  public Collection<LocationState> getAbstractSuccessors(AbstractState pElement,
      Precision pPrec) throws CPATransferException {

    CFANode node = ((LocationState)pElement).getLocationNode();

    List<LocationState> allSuccessors = new ArrayList<>(node.getNumLeavingEdges());

    for (CFANode successor : CFAUtils.successorsOf(node)) {
      allSuccessors.add(factory.getState(successor));
    }

    return allSuccessors;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pElement,
      List<AbstractState> pOtherElements, CFAEdge pCfaEdge, Precision pPrecision) {

    final LocationState e = (LocationState) pElement;

    //
    // Support for shadow CFA transitions.
    //    This transitions are not part of the program code,
    //    but get woven from a different source, for example, a specification (property) automata.
    //

    List<AbstractStateWithShadowCode> shadowingStates = Lists.newArrayList();
    for (AbstractState o: pOtherElements) {
      shadowingStates.addAll(AbstractStates.extractStatesByType(o, AbstractStateWithShadowCode.class));
    }

    if (shadowingStates.size() > 0) {

      // There are elements (components of the composite state) in the analysis
      // that provide shadow locations/transitions.

      List<AAstNode> shadowingCodeSequence = Lists.newLinkedList();

      for (AbstractStateWithShadowCode shadowProvider: shadowingStates) {
        List<AAstNode> shadowCode = shadowProvider.getOutgoingShadowCode(e.getLocationNode());
        shadowingCodeSequence.addAll(shadowCode);
      }

      if (shadowingCodeSequence.size() > 0) {

        // There is shadow code that must get woven...
        return ImmutableSet.of(factory.createStateWithShadowCode(shadowingCodeSequence, e.getLocationNode()));
      }

    }

    // Return 'null' if there was no change
    return null;
  }
}
