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

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * A partition of CFA nodes.
 */
public class Block {

  private final ImmutableSet<ReferencedVariable> referencedVariables;
  private final ImmutableSet<CFANode> callNodes;
  private final ImmutableSet<CFANode> returnNodes;
  private final ImmutableSet<CFANode> nodes;
  private final Set<CFANode> outerCallNodes;
  private final Set<String> referencedVarNames;

  public Block(
      Iterable<ReferencedVariable> pReferencedVariables,
      Set<CFANode> pCallNodes,
      Set<CFANode> pReturnNodes,
      Iterable<CFANode> allNodes,
      Set<CFANode> outerCallNodes) {

    referencedVariables = ImmutableSet.copyOf(pReferencedVariables);
    callNodes = ImmutableSet.copyOf(pCallNodes);
    returnNodes = ImmutableSet.copyOf(pReturnNodes);
    nodes = ImmutableSet.copyOf(allNodes);
    referencedVarNames = referencedVariables.stream()
        .map(r -> r.getName()).collect(Collectors.toSet());
    this.outerCallNodes = outerCallNodes;
  }

  public boolean referencesVar(String pVarName) {
    return referencedVarNames.contains(pVarName);
  }

  /**
   * @return nodes which are not in this block, but which successors are.
   */
  public Set<CFANode> getOuterCallNodes() {
    return outerCallNodes;
  }

  public Set<CFANode> getCallNodes() {
    return callNodes;
  }

  public CFANode getCallNode() {
    assert callNodes.size() == 1;
    return callNodes.iterator().next();
  }


  /**
   * @return all referenced variables for this and <em>all nested blocks</em> as well.
   */
  public Set<ReferencedVariable> getReferencedVariables() {
    return referencedVariables;
  }

  /**
   * @return all nodes contained in this block.
   */
  public Set<CFANode> getNodes() {
    return nodes;
  }

  public boolean isReturnNode(CFANode pNode) {
    return returnNodes.contains(pNode);
  }

  /**
   * @return all nodes in this block from which there exist edges to outside of the block.
   */
  public Set<CFANode> getReturnNodes() {
    return returnNodes;
  }

  public boolean isCallNode(CFANode pNode) {
    return callNodes.contains(pNode);
  }

  @Override
  public String toString() {
    return "Block " +
            "(CallNodes: " + callNodes + ") " +
            "(Nodes: " + (nodes.size() < 10 ? nodes : "[#=" + nodes.size() + "]") + ") " +
            "(ReturnNodes: " + returnNodes + ")";
  }
}
