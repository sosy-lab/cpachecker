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
package org.sosy_lab.cpachecker.cfa.blocks;

import java.util.Collections;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFANode;

import com.google.common.collect.ImmutableSet;

/**
 * Represents a block as described in the BAM paper.
 */
public class Block {

  private final ImmutableSet<ReferencedVariable> referencedVariables;
  private final ImmutableSet<CFANode> callNodes;
  private final ImmutableSet<CFANode> returnNodes;
  private final ImmutableSet<CFANode> nodes;

  public Block(Set<ReferencedVariable> pReferencedVariables,
      Set<CFANode> pCallNodes, Set<CFANode> pReturnNodes, Set<CFANode> allNodes) {

    referencedVariables = ImmutableSet.copyOf(pReferencedVariables);
    callNodes = ImmutableSet.copyOf(pCallNodes);
    returnNodes = ImmutableSet.copyOf(pReturnNodes);
    nodes = ImmutableSet.copyOf(allNodes);
  }

  public Set<CFANode> getCallNodes() {
    return Collections.unmodifiableSet(callNodes);
  }

  public CFANode getCallNode() {
    assert callNodes.size() == 1;
    return callNodes.iterator().next();
  }

  public Set<ReferencedVariable> getReferencedVariables() {
    return referencedVariables;
  }

  public Set<CFANode> getNodes() {
    return nodes;
  }

  public boolean isReturnNode(CFANode pNode) {
    return returnNodes.contains(pNode);
  }

  public Set<CFANode> getReturnNodes() {
    return returnNodes;
  }

  public boolean isCallNode(CFANode pNode) {
    return callNodes.contains(pNode);
  }

  @Override
  public String toString() {
    return "Block (CallNodes: " + callNodes + ")";
  }
}
