package org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph;

import org.jgrapht.Graph;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.fllesh.fql.backend.testgoals.TestGoal;
import org.sosy_lab.cpachecker.fllesh.fql.backend.testgoals.TestGoalVisitor;

public class Edge implements TestGoal {
  private Node mSource;
  private Node mTarget;
  private CFAEdge mCFAEdge;
  
  private int mHashCode;
  
  public Edge(Node pSource, Node pTarget, CFAEdge pCFAEdge, Graph<Node, Edge> pGraph) {
    assert(pSource != null);
    assert(pTarget != null);
    assert(pCFAEdge != null);
    assert(pGraph != null);
    
    mSource = pSource;
    mTarget = pTarget;
    mCFAEdge = pCFAEdge;
    
    mHashCode = 2341233 + mSource.hashCode() + mTarget.hashCode() + mCFAEdge.hashCode();
    
    // edge to graph
    pGraph.addVertex(mSource);
    pGraph.addVertex(mTarget);
    pGraph.addEdge(mSource, mTarget, this);
  }
  
  public Edge(Node pSource, Node pTarget, CFAEdge pCFAEdge) {
    assert(pSource != null);
    assert(pTarget != null);
    assert(pCFAEdge != null);
    
    mSource = pSource;
    mTarget = pTarget;
    mCFAEdge = pCFAEdge;
    
    mHashCode = 2341233 + mSource.hashCode() + mTarget.hashCode() + mCFAEdge.hashCode();
  }
  
  public Edge(Edge pEdge) {
    this(pEdge.getSource(), pEdge.getTarget(), pEdge.getCFAEdge());
  }
  
  @Override
  public int hashCode() {
    return mHashCode;
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther.getClass() == getClass()) {
      Edge lEdge = (Edge)pOther;
      
      return lEdge.mSource.equals(mSource) && lEdge.mTarget.equals(mTarget) && lEdge.mCFAEdge.equals(mCFAEdge);
    }
    
    return false;
  }
  
  @Override
  public String toString() {
    return mSource.toString() + "-(" + mCFAEdge.toString() + ")>" + mTarget.toString();
  }
  
  public Node getSource() {
    return mSource;
  }
  
  public Node getTarget() {
    return mTarget;
  }
  
  public CFAEdge getCFAEdge() {
    return mCFAEdge;
  }
  
  @Override
  public <T> T accept(TestGoalVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }
  
}
