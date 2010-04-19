package cpa.art;

import java.util.LinkedList;

import cfa.objectmodel.CFAEdge;

import common.Pair;


public class Path extends LinkedList<Pair<ARTElement, CFAEdge>> {
  
  private static final long serialVersionUID = -3223480082103314555L;

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();

    for (Pair<ARTElement, CFAEdge> pair : this) {
      sb.append("Line ");
      sb.append(pair.getSecond().getSuccessor().getLineNumber());
      sb.append(": ");
      sb.append(pair.getSecond());
      sb.append("\n");
    }
    
    return sb.toString();
  }

}
