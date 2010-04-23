package fllesh.fql.fllesh.cpa;

import java.util.HashSet;
import java.util.Set;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import fllesh.fql.fllesh.util.CFATraversal;
import fllesh.fql.fllesh.util.CFAVisitor;

public class AddSelfLoop {
  private static class AddSelfLoopCFAVisitor implements CFAVisitor {

    private static AddSelfLoopCFAVisitor mInstance = new AddSelfLoopCFAVisitor();
    private Set<CFAEdge> mSelfLoops = new HashSet<CFAEdge>(); 
    
    @Override
    public void init(CFANode pInitialNode) {
      mSelfLoops.add(InternalSelfLoop.getOrCreate(pInitialNode));
    }

    @Override
    public void visit(CFAEdge pEdge) {
      CFANode lSuccessor = pEdge.getSuccessor();
      
      mSelfLoops.add(InternalSelfLoop.getOrCreate(lSuccessor));
    }
    
  }
  
  public static Set<CFAEdge> addSelfLoops(CFANode pInitialNode) {
    CFATraversal.traverse(pInitialNode, AddSelfLoopCFAVisitor.mInstance);
    
    return AddSelfLoopCFAVisitor.mInstance.mSelfLoops;
  }
}
