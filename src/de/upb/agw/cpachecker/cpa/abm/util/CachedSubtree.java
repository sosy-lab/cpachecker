package de.upb.agw.cpachecker.cpa.abm.util;

import java.util.Collections;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

/**
 * Represents a block as described in the ABM paper.
 * @author dwonisch
 *
 */
public class CachedSubtree {
  private Set<ReferencedVariable> referencedVariables;
  private Set<CFANode> callNodes;
  private Set<CFANode> returnNodes;
  private Set<CFANode> uniqueNodes;
  private Set<CFANode> nodes;  
  
  public CachedSubtree(Set<ReferencedVariable> pReferencedVariables,
      Set<CFANode> pCallNodes, Set<CFANode> pReturnNodes, Set<CFANode> pUniqueNodeSet, Set<CFANode> allNodes) {
    super();
    referencedVariables = pReferencedVariables;
    callNodes = pCallNodes;
    returnNodes = pReturnNodes;
    uniqueNodes = pUniqueNodeSet;
    nodes = allNodes;
  }

  public Set<CFANode> getCallNodes() {
    return Collections.unmodifiableSet(callNodes);
  } 
  
  public CFANode getCallNode() {
    assert callNodes.size() == 1;
    return callNodes.iterator().next();    
  } 
  
  public Set<ReferencedVariable> getReferencedVariables() {
    return Collections.unmodifiableSet(referencedVariables);
  }

  public Set<CFANode> getNodes() {
    return Collections.unmodifiableSet(nodes);
  }

  public boolean isReturnNode(CFANode pNode) {
    return returnNodes.contains(pNode);
  }

  public Set<CFANode> getReturnNodes() {
    return Collections.unmodifiableSet(returnNodes);
  }
  
  public Set<CFANode> getUniqueNodes() {
    return Collections.unmodifiableSet(uniqueNodes);
  }

  public boolean isCallNode(CFANode pNode) {
    return callNodes.contains(pNode);
  }
  
  @Override
  public String toString() {
    return "CachedSubtree (CallNodes: " + callNodes + ")";
  }
}
