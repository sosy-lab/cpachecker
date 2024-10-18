/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

public class LocationTransferRelation implements TransferRelation {

  private final LocationElementFactory factory;

  public LocationTransferRelation(LocationElementFactory pFactory) {
    factory = pFactory;
  }

  private Collection<LocationElement> getAbstractSuccessor (AbstractElement element, CFAEdge cfaEdge, Precision prec) throws CPATransferException
  {
    LocationElement inputElement = (LocationElement) element;
    CFANode node = inputElement.getLocationNode ();

    // for rely-guarantee
    if (cfaEdge.getSuccessor().equals(node)){
      return Collections.singleton(inputElement);
    }

    int numLeavingEdges = node.getNumLeavingEdges ();
    for (int edgeIdx = 0; edgeIdx < numLeavingEdges; edgeIdx++)
    {
      CFAEdge testEdge = node.getLeavingEdge (edgeIdx);
      if (testEdge == cfaEdge)
      {
        return Collections.singleton(factory.getElement(testEdge.getSuccessor()));
      }
    }

    if (node.getLeavingSummaryEdge() != null){
      CallToReturnEdge summaryEdge = node.getLeavingSummaryEdge();
      return Collections.singleton(factory.getElement(summaryEdge.getSuccessor()));
    }

    return Collections.emptySet();
  }

  @Override
  public Collection<LocationElement> getAbstractSuccessors (AbstractElement element, Precision prec, CFAEdge cfaEdge) throws CPATransferException
  {
    if (cfaEdge != null) {
      return getAbstractSuccessor(element, cfaEdge, prec);
    }

    CFANode node = ((LocationElement)element).getLocationNode ();

    int numLeavingEdges = node.getNumLeavingEdges();
    List<LocationElement> allSuccessors = new ArrayList<LocationElement>(numLeavingEdges);

    for (int edgeIdx = 0; edgeIdx < numLeavingEdges; edgeIdx++)
    {
      CFAEdge tempEdge = node.getLeavingEdge (edgeIdx);
      allSuccessors.add(factory.getElement(tempEdge.getSuccessor()));
    }

    return allSuccessors;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement element,
                         List<AbstractElement> otherElements, CFAEdge cfaEdge,
                         Precision precision) {
    return null;
  }
}
