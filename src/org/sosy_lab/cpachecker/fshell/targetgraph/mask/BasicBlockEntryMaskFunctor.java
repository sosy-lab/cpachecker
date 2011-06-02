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
package org.sosy_lab.cpachecker.fshell.targetgraph.mask;

import java.util.HashSet;
import java.util.Set;

import org.jgrapht.graph.MaskFunctor;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.fshell.targetgraph.Edge;
import org.sosy_lab.cpachecker.fshell.targetgraph.Node;

public class BasicBlockEntryMaskFunctor implements MaskFunctor<Node, Edge> {

  private final Set<CFAEdge> mBasicBlockEntries;
  private final Set<CFANode> mCFANodes;

  public BasicBlockEntryMaskFunctor(Set<CFAEdge> pBasicBlockEntries) {
    mBasicBlockEntries = pBasicBlockEntries;

    mCFANodes = new HashSet<CFANode>();

    for (CFAEdge lCFAEdge : mBasicBlockEntries) {
      mCFANodes.add(lCFAEdge.getPredecessor());
      mCFANodes.add(lCFAEdge.getSuccessor());
    }
  }

  @Override
  public boolean isEdgeMasked(Edge pEdge) {
    CFAEdge lCFAEdge = pEdge.getCFAEdge();

    return !mBasicBlockEntries.contains(lCFAEdge);
  }

  @Override
  public boolean isVertexMasked(Node pNode) {
    CFANode lCFANode = pNode.getCFANode();

    return !mCFANodes.contains(lCFANode);
  }

}
