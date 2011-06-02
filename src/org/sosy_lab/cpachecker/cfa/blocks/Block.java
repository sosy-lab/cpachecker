package org.sosy_lab.cpachecker.cfa.blocks;

import java.util.Collections;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

import com.google.common.collect.ImmutableSet;

/**
 * Represents a block as described in the ABM paper.
 * @author dwonisch
 *
 */
public class Block {

  private final ImmutableSet<ReferencedVariable> referencedVariables;
  private final ImmutableSet<CFANode> callNodes;
  private final ImmutableSet<CFANode> returnNodes;
  private final ImmutableSet<CFANode> uniqueNodes;
  private final ImmutableSet<CFANode> nodes;

  public Block(Set<ReferencedVariable> pReferencedVariables,
      Set<CFANode> pCallNodes, Set<CFANode> pReturnNodes, Set<CFANode> pUniqueNodeSet, Set<CFANode> allNodes) {

    referencedVariables = ImmutableSet.copyOf(pReferencedVariables);
    callNodes = ImmutableSet.copyOf(pCallNodes);
    returnNodes = ImmutableSet.copyOf(pReturnNodes);
    uniqueNodes = ImmutableSet.copyOf(pUniqueNodeSet);
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

  public Set<CFANode> getUniqueNodes() {
    return uniqueNodes;
  }

  public boolean isCallNode(CFANode pNode) {
    return callNodes.contains(pNode);
  }

  @Override
  public String toString() {
    return "Block (CallNodes: " + callNodes + ")";
  }
}
