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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.SortedSet;
import java.util.TreeSet;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElementWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableElement;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

import com.google.common.base.Preconditions;

public class LocationElement implements AbstractElementWithLocation, AbstractQueryableElement, Partitionable
{

  public static class LocationElementFactory {
    private LocationElement[] elements = null;

    public void initialize(CFANode initialNode) {
      if (elements != null) {
        // multiple calls are allowed, but only with nodes of the same CFA
        Preconditions.checkState(elements[initialNode.getNodeNumber()].getLocationNode().equals(initialNode));
        return;
      }

      SortedSet<CFANode> seenNodes = new TreeSet<CFANode>();
      Deque<CFANode> waitlist = new ArrayDeque<CFANode>();
      seenNodes.add(initialNode);
      waitlist.add(initialNode);

      while (!waitlist.isEmpty()) {
        CFANode n = waitlist.pop();

        CFAEdge edge = n.getLeavingSummaryEdge();
        if (edge != null) {
          CFANode succ = edge.getSuccessor();
          if (seenNodes.add(succ)) {
            waitlist.push(succ);
          }
        }

        for (int i = 0; i < n.getNumLeavingEdges(); i++) {
          edge = n.getLeavingEdge(i);
          CFANode succ = edge.getSuccessor();
          if (seenNodes.add(succ)) {
            waitlist.push(succ);
          }
        }
      }

      int maxNodeNumber = seenNodes.last().getNodeNumber();
      elements = new LocationElement[maxNodeNumber+1];
      for (CFANode n : seenNodes) {
        elements[n.getNodeNumber()] = new LocationElement(n);
      }
    }

    public LocationElement getElement(CFANode node) {
      return Preconditions.checkNotNull(elements[node.getNodeNumber()]);
    }
  }

    private final CFANode locationNode;

    private LocationElement (CFANode locationNode)
    {
        this.locationNode = locationNode;
    }

    @Override
    public CFANode getLocationNode ()
    {
        return locationNode;
    }

    @Override
    public String toString() {
      return locationNode + " (line " + locationNode.getLineNumber() + ")";
    }

    @Override
    public boolean checkProperty(String pProperty) throws InvalidQueryException {
      String[] parts = pProperty.split("==");
      if (parts.length != 2)
        throw new InvalidQueryException("The Query \"" + pProperty + "\" is invalid. Could not split the property string correctly.");
      else {
        if (parts[0].toLowerCase().equals("line")) {
          try {
            int queryLine = Integer.parseInt(parts[1]);
            return this.locationNode.getLineNumber() == queryLine;
          } catch (NumberFormatException nfe) {
            throw new InvalidQueryException("The Query \"" + pProperty + "\" is invalid. Could not parse the integer \"" + parts[1] + "\"");
          }
        } else if (parts[0].toLowerCase().equals("functionname")) {
          return this.locationNode.getFunctionName().equals(parts[1]);
        } else {
          throw new InvalidQueryException("The Query \"" + pProperty + "\" is invalid. \"" + parts[0] + "\" is no valid keyword");
        }
      }
    }

    @Override
    public void modifyProperty(String pModification)
        throws InvalidQueryException {
      throw new InvalidQueryException("The location CPA does not support modification.");
    }

    @Override
    public String getCPAName() {
      return "location";
    }

    @Override
    public Boolean evaluateProperty(
        String pProperty) throws InvalidQueryException {
      return Boolean.valueOf(checkProperty(pProperty));
    }

    @Override
    public Object getPartitionKey() {
      return this;
    }

    @Override
    public int hashCode() {
      return locationNode.hashCode();
    }

    // no equals and hashCode because there is always only one element per CFANode
}
