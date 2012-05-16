/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.location.LocationElement.LocationElementFactory;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class InverseLocationTransferRelation implements TransferRelation {

  private final LocationElementFactory factory;

  public InverseLocationTransferRelation(LocationElementFactory pFactory) {
    factory = pFactory;
  }

  private Collection<LocationElement> getAbstractSuccessor(AbstractElement element,
      CFAEdge cfaEdge, Precision prec) throws CPATransferException {

    LocationElement inputElement = (LocationElement) element;
    CFANode node = inputElement.getLocationNode();

    int numEnteringEdges = node.getNumEnteringEdges();

    for (int edgeIdx = 0; edgeIdx < numEnteringEdges; edgeIdx++) {
      CFAEdge testEdge = node.getEnteringEdge(edgeIdx);

      if (testEdge == cfaEdge) {
        return Collections.singleton(factory.getElement(testEdge.getPredecessor()));
      }
    }

    if (node.getEnteringSummaryEdge() != null) {
      CallToReturnEdge summaryEdge = node.getEnteringSummaryEdge();
      return Collections.singleton(factory.getElement(summaryEdge.getPredecessor()));
    }

    return Collections.emptySet();
  }

  @Override
  public Collection<LocationElement> getAbstractSuccessors(AbstractElement element,
      Precision prec, CFAEdge cfaEdge) throws CPATransferException {

    if (cfaEdge != null) {
      return getAbstractSuccessor(element, cfaEdge, prec);
    }

    CFANode node = ((LocationElement)element).getLocationNode();

    int numEnteringEdges = node.getNumEnteringEdges();
    List<LocationElement> allSuccessors = new ArrayList<LocationElement>(numEnteringEdges);

    for (int edgeIdx = 0; edgeIdx < numEnteringEdges; edgeIdx++) {
      CFAEdge tempEdge = node.getEnteringEdge(edgeIdx);
      allSuccessors.add(factory.getElement(tempEdge.getPredecessor()));
    }

    return allSuccessors;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement element,
      List<AbstractElement> otherElements, CFAEdge cfaEdge, Precision precision) {
    return null;
  }
}
