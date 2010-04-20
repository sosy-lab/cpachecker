package fql.backend.testgoals;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fql.backend.targetgraph.Edge;
import fql.backend.targetgraph.Node;

public class EdgeSequence implements TestGoal, Iterable<Edge> {

  private List<Edge> mEdgeSequence;
  private Node mStartNode;
  private Node mEndNode;
  
  public EdgeSequence(List<Edge> pEdgeSequence) {
    assert(pEdgeSequence != null);
    
    if (pEdgeSequence.size() < 2) {
      throw new IllegalArgumentException("EdgeSequences have to contain at least two edges!");
    }
    
    mEdgeSequence = new ArrayList<Edge>(pEdgeSequence);
    
    mStartNode = mEdgeSequence.get(0).getSource();
    mEndNode = mEdgeSequence.get(mEdgeSequence.size() - 1).getTarget();
  }
  
  public Node getStartNode() {
    return mStartNode;
  }
  
  public Node getEndNode() {
    return mEndNode;
  }
  
  @Override
  public <T> T accept(TestGoalVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

  @Override
  public Iterator<Edge> iterator() {
    return mEdgeSequence.iterator();
  }
  
  public Edge get(int pIndex) {
    assert(pIndex >= 0);
    assert(pIndex < size());
    
    return mEdgeSequence.get(pIndex);
  }
  
  public int size() {
    return mEdgeSequence.size();
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
      EdgeSequence lSequence = (EdgeSequence)pOther;
      
      if (lSequence.size() != size()) {
        return false;
      }
      
      for (int lIndex = 0; lIndex < size(); lIndex++) {
        if (!lSequence.mEdgeSequence.get(lIndex).equals(mEdgeSequence.get(lIndex))) {
          return false;
        }
      }
      
      return true;
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 234829 + mEdgeSequence.hashCode();
  }
  
  @Override
  public String toString() {
    return mEdgeSequence.toString();
  }

}
