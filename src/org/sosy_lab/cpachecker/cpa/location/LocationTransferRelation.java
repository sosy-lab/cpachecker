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

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.location.LocationState.LocationStateFactory;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class LocationTransferRelation implements TransferRelation {

  private final LocationStateFactory factory;

  public LocationTransferRelation(LocationStateFactory pFactory) {
    factory = pFactory;
  }

  private Collection<LocationState> getAbstractSuccessor(AbstractState element,
      CFAEdge cfaEdge, Precision prec) throws CPATransferException {

    LocationState inputElement = (LocationState) element;
    CFANode node = inputElement.getLocationNode();

    if (CFAUtils.allLeavingEdges(node).contains(cfaEdge)) {
      return Collections.singleton(factory.getState(cfaEdge.getSuccessor()));
    }

    return Collections.emptySet();
  }

  @Override
  public Collection<LocationState> getAbstractSuccessors(AbstractState element,
      Precision prec, CFAEdge cfaEdge) throws CPATransferException {

    if (cfaEdge != null) {
      return getAbstractSuccessor(element, cfaEdge, prec);
    }

    CFANode node = ((LocationState)element).getLocationNode();

    List<LocationState> allSuccessors = new ArrayList<>(node.getNumLeavingEdges());

    for (CFANode successor : CFAUtils.successorsOf(node)) {
      allSuccessors.add(factory.getState(successor));
    }

    return allSuccessors;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState element,
      List<AbstractState> otherElements, CFAEdge cfaEdge, Precision precision) {
    return null;
  }
}
