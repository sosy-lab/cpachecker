package fql.fllesh.cpa;

import java.util.HashMap;
import java.util.Map;

import cfa.objectmodel.BlankEdge;
import cfa.objectmodel.CFANode;

public class InternalSelfLoop extends BlankEdge {

  private static Map<CFANode, InternalSelfLoop> mEdgeCache = new HashMap<CFANode, InternalSelfLoop>();
  
  public static InternalSelfLoop getOrCreate(CFANode pNode) {
    if (mEdgeCache.containsKey(pNode)) {
      return mEdgeCache.get(pNode);
    }
    
    InternalSelfLoop lLoop = new InternalSelfLoop(pNode);
    
    mEdgeCache.put(pNode, lLoop);
    
    return lLoop;
  }
  
  private InternalSelfLoop(CFANode pNode) {
    super("Internal Self Loop", pNode.getLineNumber(), pNode, pNode);

    // do we want to add this edge to be added to the edges of pNode?
    // our transfer relation just returns bottom if it has no successor on the 
    // self loop
    pNode.addEnteringEdge(this);
    pNode.addLeavingEdge(this);

    // if we add it permanently to the CFA we don't need to maintain a cache.
    // mNode = pNode;
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
      InternalSelfLoop lLoop = (InternalSelfLoop)pOther;
      
      return lLoop.getSuccessor().equals(getSuccessor());
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 298492 + super.getSuccessor().hashCode();
  }

  @Override
  public void addToCFA() {
    throw new UnsupportedOperationException();
  }  
}
