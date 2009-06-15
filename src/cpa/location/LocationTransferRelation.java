/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.location;

import java.util.ArrayList;
import java.util.List;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.CallToReturnEdge;

import exceptions.CPATransferException;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;

public class LocationTransferRelation implements TransferRelation
{
  private final LocationDomain locationDomain;

  public LocationTransferRelation (LocationDomain locationDomain)
  {
    this.locationDomain = locationDomain;
  }

  public AbstractElement getAbstractSuccessor (AbstractElement element, CFAEdge cfaEdge, Precision prec) throws CPATransferException
  {
    LocationElement inputElement = (LocationElement) element;
    CFANode node = inputElement.getLocationNode ();

    int numLeavingEdges = node.getNumLeavingEdges ();
    for (int edgeIdx = 0; edgeIdx < numLeavingEdges; edgeIdx++)
    {
      CFAEdge testEdge = node.getLeavingEdge (edgeIdx);
      if (testEdge == cfaEdge)
      {
        return new LocationElement (testEdge.getSuccessor ());
      }
    }

    if (node.getLeavingSummaryEdge() != null){
      CallToReturnEdge summaryEdge = node.getLeavingSummaryEdge();
      return new LocationElement (summaryEdge.getSuccessor());
    }

    return locationDomain.getBottomElement ();
  }

  public List<AbstractElementWithLocation> getAllAbstractSuccessors (AbstractElementWithLocation element, Precision prec) throws CPATransferException
  {
    CFANode node = element.getLocationNode ();

    List<AbstractElementWithLocation> allSuccessors = new ArrayList<AbstractElementWithLocation> ();
    int numLeavingEdges = node.getNumLeavingEdges ();

    for (int edgeIdx = 0; edgeIdx < numLeavingEdges; edgeIdx++)
    {
      CFAEdge tempEdge = node.getLeavingEdge (edgeIdx);
      allSuccessors.add (new LocationElement (tempEdge.getSuccessor ()));
    }

    return allSuccessors;
  }

  @Override
  public void strengthen(AbstractElement element,
                         List<AbstractElement> otherElements, CFAEdge cfaEdge,
                         Precision precision) {    
  }
}
