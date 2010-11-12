package org.sosy_lab.cpachecker.fshell.targetgraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Path implements Iterable<Edge> {

  private Node mStartNode;
  private Node mEndNode;
  private List<Edge> mEdges;
  
  public Path(Node pNode, List<Edge> pEdges) {
    if (pEdges.size() == 0) {
      mStartNode = pNode;
      mEndNode = pNode;
      
      mEdges = Collections.emptyList();
    }
    else {
      mStartNode = pEdges.get(0).getSource();        
      mEndNode = pEdges.get(pEdges.size() - 1).getTarget();
      
      if (!pNode.equals(mEndNode)) {
        throw new IllegalArgumentException();
      }
      
      Node lTmpEndNode = mStartNode;
      
      for (Edge lEdge : pEdges) {
        if (!lTmpEndNode.equals(lEdge.getSource())) {
          throw new IllegalArgumentException();
        }
        
        lTmpEndNode = lEdge.getTarget();
      }
      
      mEdges = new ArrayList<Edge>(pEdges);
    }
  }
  
  public int length() {
    return mEdges.size();
  }
  
  public Node getStartNode() {
    return mStartNode;
  }
  
  public Node getEndNode() {
    return mEndNode;
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (!getClass().equals(pOther.getClass())) {
      return false;
    }
    
    Path lOther = (Path)pOther;
    
    return lOther.mStartNode.equals(mStartNode) && lOther.mEndNode.equals(mEndNode) && lOther.mEdges.equals(mEdges);
  }
  
  @Override
  public String toString() {
    StringBuffer lBuffer = new StringBuffer();
    
    lBuffer.append("[");
    
    if (this.length() > 0) {
      boolean lIsFirst = true;
      
      for (Edge lEdge : mEdges) {
        if (lIsFirst) {
          lIsFirst = false;
        }
        else {
          lBuffer.append(", ");
        }
        
        lBuffer.append(lEdge.toString());
      }        
    }
    else {
      lBuffer.append(mStartNode.toString());
    }
    
    lBuffer.append("]");
    
    return lBuffer.toString();
  }

  @Override
  public Iterator<Edge> iterator() {
    return mEdges.iterator();
  }
  
}
