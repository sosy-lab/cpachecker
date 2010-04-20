package fql.fllesh.cpa;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import fql.fllesh.util.CFATraversal;
import fql.fllesh.util.CFAVisitor;

public class AddSelfLoop {
  private static class AddSelfLoopCFAVisitor implements CFAVisitor {

    private static AddSelfLoopCFAVisitor mInstance = new AddSelfLoopCFAVisitor();
    
    @Override
    public void init(CFANode pInitialNode) {
      InternalSelfLoop.getOrCreate(pInitialNode);
    }

    @Override
    public void visit(CFAEdge pEdge) {
      CFANode lSuccessor = pEdge.getSuccessor();
      
      InternalSelfLoop.getOrCreate(lSuccessor);
    }
    
  }
  
  public static void addSelfLoops(CFANode pInitialNode) {
    CFATraversal.traverse(pInitialNode, AddSelfLoopCFAVisitor.mInstance);
  }
}
